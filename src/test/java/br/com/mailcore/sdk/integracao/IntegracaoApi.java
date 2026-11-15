package br.com.mailcore.sdk.integracao;

import br.com.mailcore.sdk.Cliente;
import br.com.mailcore.sdk.Configuracao;
import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Cliente do laboratório Docker. A saída contém somente resultados sanitizados. */
public final class IntegracaoApi implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final int LIMITE_FIXTURE = 1024 * 1024;

    private final ObjectNode fixture;
    private final byte[] ca;
    private final OkHttpClient http;
    private final List<Cliente> clientes = new ArrayList<Cliente>();
    private final List<String> cenarios = new ArrayList<String>();
    private final Set<String> protocolos = new LinkedHashSet<String>();
    private String etapa = "prontidao";

    private static final class Falha extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String codigo;
        private final Integer status;

        private Falha() {
            super("Contrato de integração não satisfeito.", null, false, false);
            codigo = null;
            status = null;
        }

        private Falha(ErroMailcore erro) {
            super("Contrato de integração não satisfeito.", null, false, false);
            codigo = erro.codigo();
            status = erro.status();
        }
    }

    private IntegracaoApi(ObjectNode fixture) throws Exception {
        this.fixture = fixture;
        ca = texto(fixture, "certificado_ca").getBytes(StandardCharsets.US_ASCII);
        fixture.remove("certificado_ca");
        http = clienteHttp(ca);
    }

    private static void exigir(boolean condicao) {
        if (!condicao) {
            throw new Falha();
        }
    }

    private static String texto(JsonNode documento, String nome) {
        JsonNode valor = documento.get(nome);
        exigir(valor != null && valor.isTextual());
        return valor.textValue();
    }

    private static String id(JsonNode documento) {
        return texto(documento, "id");
    }

    private static JsonNode lista(JsonNode documento, String nome) {
        JsonNode valor = documento.get(nome);
        exigir(valor != null && valor.isArray());
        return valor;
    }

    private static boolean numero(JsonNode documento, String nome, long esperado) {
        JsonNode valor = documento.get(nome);
        return valor != null && valor.isIntegralNumber()
                && valor.bigIntegerValue().equals(java.math.BigInteger.valueOf(esperado));
    }

    private static ObjectNode objeto(Object... pares) {
        exigir(pares.length % 2 == 0);
        ObjectNode documento = JSON.createObjectNode();
        for (int indice = 0; indice < pares.length; indice += 2) {
            documento.set((String) pares[indice], JSON.valueToTree(pares[indice + 1]));
        }
        return documento;
    }

    private static ArrayNode array(Object... valores) {
        ArrayNode lista = JSON.createArrayNode();
        for (Object valor : valores) {
            lista.add(JSON.valueToTree(valor));
        }
        return lista;
    }

    private static ObjectNode alterar(JsonNode original, String nome, Object valor) {
        exigir(original.isObject());
        ObjectNode copia = ((ObjectNode) original).deepCopy();
        copia.set(nome, JSON.valueToTree(valor));
        return copia;
    }

    private static Map<String, Object> consulta(int limite) {
        Map<String, Object> valores = new LinkedHashMap<String, Object>();
        valores.put("limite", Integer.valueOf(limite));
        return valores;
    }

    private static OpcoesRequisicao idempotencia(String chave) {
        return OpcoesRequisicao.construtor().chaveIdempotencia(chave).construir();
    }

    private static Map<String, List<String>> cabecalhos(String... pares) {
        Map<String, List<String>> valores = new LinkedHashMap<String, List<String>>();
        for (int indice = 0; indice < pares.length; indice += 2) {
            valores.put(pares[indice], Collections.singletonList(pares[indice + 1]));
        }
        return valores;
    }

    private static String cabecalho(Resposta resposta, String nome) {
        for (Map.Entry<String, List<String>> entrada : resposta.cabecalhos().entrySet()) {
            if (entrada.getKey().equalsIgnoreCase(nome) && !entrada.getValue().isEmpty()) {
                return entrada.getValue().get(0);
            }
        }
        return null;
    }

    private static void esperarErro(int status, Runnable acao) {
        try {
            acao.run();
        } catch (ErroMailcore erro) {
            if (Integer.valueOf(status).equals(erro.status())) {
                return;
            }
            throw new Falha(erro);
        }
        throw new Falha();
    }

    private static List<String> ids(JsonNode documento) {
        List<String> valores = new ArrayList<String>();
        for (JsonNode email : lista(documento, "dados")) {
            valores.add(id(email));
        }
        Collections.sort(valores);
        return valores;
    }

    private static byte[] lerLimitado(InputStream entrada) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] parte = new byte[8192];
        int lidos;
        while ((lidos = entrada.read(parte)) != -1) {
            exigir(lidos <= LIMITE_FIXTURE - bytes.size());
            bytes.write(parte, 0, lidos);
        }
        return bytes.toByteArray();
    }

    private static OkHttpClient clienteHttp(byte[] ca) throws Exception {
        KeyStore chaves = KeyStore.getInstance(KeyStore.getDefaultType());
        chaves.load(null, null);
        int indice = 0;
        for (Certificate certificado : CertificateFactory.getInstance("X.509")
                .generateCertificates(new ByteArrayInputStream(ca))) {
            chaves.setCertificateEntry("ca-" + indice++, certificado);
        }
        exigir(indice > 0);
        TrustManagerFactory fabrica = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        fabrica.init(chaves);
        X509TrustManager confianca = null;
        for (TrustManager gerente : fabrica.getTrustManagers()) {
            if (gerente instanceof X509TrustManager) {
                confianca = (X509TrustManager) gerente;
                break;
            }
        }
        exigir(confianca != null);
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(null, new TrustManager[] {confianca}, null);
        return new OkHttpClient.Builder()
                .sslSocketFactory(tls.getSocketFactory(), confianca)
                .proxy(Proxy.NO_PROXY)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .connectTimeout(2, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    private Cliente cliente(String chave, boolean injetado) {
        Configuracao.Construtor configuracao = Configuracao.construtor()
                .urlBase("https://mailcore-api:3443/v1")
                .tempoLimite(Duration.ofSeconds(10))
                .tempoConexao(Duration.ofSeconds(2));
        if (injetado) {
            configuracao.clienteHttp(http);
        } else {
            configuracao.certificadoCa(ca);
        }
        Cliente cliente = new Cliente(chave, configuracao.construir());
        clientes.add(cliente);
        return cliente;
    }

    /** Sessão e callback pertencem apenas à fixture, fora da superfície do SDK. */
    private ObjectNode requisitar(String caminho, String metodo,
            Map<String, List<String>> cabecalhos, String corpo) throws IOException {
        Request.Builder pedido = new Request.Builder().url("https://mailcore-api:3443" + caminho);
        for (Map.Entry<String, List<String>> entrada : cabecalhos.entrySet()) {
            for (String valor : entrada.getValue()) {
                pedido.addHeader(entrada.getKey(), valor);
            }
        }
        pedido.header("Accept-Encoding", "identity");
        RequestBody conteudo = corpo == null ? null : RequestBody.create(
                corpo.getBytes(StandardCharsets.UTF_8), MediaType.get("application/json"));
        pedido.method(metodo, conteudo);
        try (Response retorno = http.newCall(pedido.build()).execute()) {
            Protocol versao = retorno.protocol();
            protocolos.add(versao == Protocol.HTTP_2 ? "HTTP/2" : versao.toString());
            ResponseBody resposta = retorno.body();
            exigir(resposta != null);
            byte[] bytes = lerLimitado(resposta.byteStream());
            JsonNode dados = bytes.length == 0 ? JSON.nullNode() : JSON.readTree(bytes);
            return objeto("status", Integer.valueOf(retorno.code()), "dados", dados);
        }
    }

    private void concluir() {
        cenarios.add(etapa);
    }

    private void leiturasConcorrentes(final Cliente sdk, final String emailId) throws Exception {
        ExecutorService tarefas = Executors.newFixedThreadPool(8);
        List<Future<JsonNode>> leituras = new ArrayList<Future<JsonNode>>();
        try {
            for (int indice = 0; indice < 8; indice++) {
                leituras.add(tarefas.submit(new Callable<JsonNode>() {
                    @Override
                    public JsonNode call() {
                        return sdk.emails().obter(emailId).json();
                    }
                }));
            }
            for (Future<JsonNode> leitura : leituras) {
                try {
                    exigir(id(leitura.get(15, TimeUnit.SECONDS)).equals(emailId));
                } catch (ExecutionException erro) {
                    if (erro.getCause() instanceof ErroMailcore) {
                        throw new Falha((ErroMailcore) erro.getCause());
                    }
                    throw new Falha();
                }
            }
        } finally {
            for (Future<JsonNode> leitura : leituras) {
                if (!leitura.isDone()) {
                    leitura.cancel(true);
                }
            }
            tarefas.shutdownNow();
        }
    }

    private ObjectNode executarCenarios() throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        boolean pronta = false;
        while (!pronta && System.nanoTime() < fim) {
            try {
                pronta = numero(requisitar("/health/ready", "GET",
                        Collections.<String, List<String>>emptyMap(), null), "status", 200);
            } catch (IOException | Falha ignorada) {
                // A sonda não aceita nenhuma intenção de envio.
            }
            if (!pronta) {
                Thread.sleep(200);
            }
        }
        exigir(pronta);

        etapa = "criar_full_sessao_sintetica";
        ObjectNode criacao = requisitar("/v1/api-keys", "POST", cabecalhos(
                "Content-Type", "application/json", "Origin", "https://localhost:17443",
                "Cookie", "__Host-mailcore=" + texto(fixture, "token"),
                "X-CSRF-Token", texto(fixture, "csrf")),
                objeto("nome", "Fixture Java Full", "permissao", "full_access").toString());
        fixture.remove("token");
        fixture.remove("csrf");
        exigir(numero(criacao, "status", 201));
        JsonNode full = criacao.get("dados");
        final Cliente sdk = cliente(texto(full, "chave"), false);
        final Cliente sdkInjetado = cliente(texto(full, "chave"), true);
        String remetente = "avisos@" + texto(fixture, "nome_dominio");

        etapa = "chaves_full_sending_escopo_dump";
        JsonNode chaveEnvio = sdk.chavesApi().criar(objeto("nome", "Fixture Java Sending",
                "permissao", "sending_access", "dominio_id", texto(fixture, "dominio"))).json();
        final Cliente envio = cliente(texto(chaveEnvio, "chave"), false);
        esperarErro(403, () -> envio.dominios().listar(null));
        exigir(!sdk.toString().contains(texto(full, "chave")));
        boolean encontrada = false;
        for (JsonNode chave : lista(sdk.chavesApi().listar().json(), "dados")) {
            encontrada |= chave.get("id").equals(chaveEnvio.get("id"));
        }
        exigir(encontrada);
        concluir();

        final ObjectNode pedido = objeto("remetente", "Equipe Java <" + remetente + ">",
                "destinatarios", array("principal@example.test"), "cc", array("copia@example.test"),
                "cco", array("oculto@example.test"), "assunto", "Validação Java",
                "texto", "Dados sintéticos", "html", "<p>Dados sintéticos<img src=\"cid:marca@example.test\"></p>",
                "cabecalhos", objeto("X-Teste-SDK", "sintetico"),
                "agendada_em", texto(fixture, "agendada_em"),
                "anexos", array(objeto("nome", "marca.png", "tipo_conteudo", "image/png",
                        "conteudo_base64", Base64.getEncoder().encodeToString(PNG), "id_conteudo", "marca@example.test")));
        etapa = "envio_composto_idempotencia_409_download_cancelamento_concorrencia";
        final OpcoesRequisicao idem = idempotencia("java-composto");
        JsonNode aceita = envio.emails().enviar(pedido, idem).json();
        exigir(texto(aceita, "estado").equals("agendada"));
        exigir(envio.emails().enviar(pedido, idem).json().equals(aceita));
        esperarErro(409, () -> envio.emails().enviar(alterar(pedido, "assunto", "Outra intenção"), idem));
        exigir(sdk.emails().obterPorIdempotencia("java-composto").json().get("id").equals(aceita.get("id")));
        final String emailId = id(aceita);
        exigir(lista(sdk.emails().obter(emailId).json(), "destinatarios").size() == 3);
        exigir(Arrays.equals(sdk.emails().baixarAnexo(emailId, 0).dados(), PNG));
        leiturasConcorrentes(sdk, emailId);
        exigir(texto(sdk.emails().cancelar(emailId).json(), "estado").equals("cancelada"));
        concluir();

        etapa = "lote_atomico_repeticao_conflito";
        final ObjectNode item = objeto("remetente", remetente, "destinatarios", array("lote@example.test"),
                "assunto", "Lote Java", "texto", "Dados sintéticos", "agendada_em", texto(fixture, "agendada_em"));
        List<String> antes = ids(sdk.emails().listar(consulta(100)).json());
        esperarErro(422, () -> envio.lotes().enviar(array(item,
                alterar(item, "destinatarios", array("endereco-invalido"))), idempotencia("java-lote-invalido")));
        exigir(antes.equals(ids(sdk.emails().listar(consulta(100)).json())));
        final ArrayNode lote = array(item, alterar(item, "assunto", "Segundo item Java"));
        final OpcoesRequisicao idemLote = idempotencia("java-lote");
        JsonNode aceitaLote = envio.lotes().enviar(lote, idemLote).json();
        exigir(lista(aceitaLote, "dados").size() == 2);
        exigir(envio.lotes().enviar(lote, idemLote).json().equals(aceitaLote));
        exigir(sdk.lotes().obterPorIdempotencia("java-lote").json().equals(aceitaLote));
        esperarErro(409, () -> envio.lotes().enviar(array(alterar(item, "assunto", "Divergente"), lote.get(1)), idemLote));
        exigir(ids(sdk.emails().listar(consulta(100)).json()).size() == antes.size() + 2);
        for (JsonNode email : lista(aceitaLote, "dados")) {
            sdk.emails().cancelar(id(email));
        }
        concluir();

        etapa = "dominios_capacidades_tracking_crud";
        JsonNode dominio = sdk.dominios().atualizar(texto(fixture, "dominio"), objeto(
                "capacidades", objeto("envio", "enabled", "recebimento", "enabled"),
                "rastreamento_aberturas", Boolean.TRUE, "rastreamento_cliques", Boolean.TRUE)).json();
        exigir(texto(dominio.get("capacidades"), "recebimento").equals("enabled")
                && dominio.get("rastreamento_aberturas").asBoolean());
        JsonNode novo = sdk.dominios().criar(objeto("nome", "java-"
                + texto(fixture, "organizacao").replace("-", "") + ".example.com",
                "capacidades", objeto("envio", "enabled", "recebimento", "disabled"))).json();
        exigir(texto(sdk.dominios().remover(id(novo)).json(), "estado").equals("suspenso"));
        concluir();

        etapa = "contatos_objetos_listas_null_false_importacao_csv_fluxos";
        JsonNode segmento = sdk.segmentos().criar(objeto("nome", "Segmento Java")).json();
        JsonNode topico = sdk.topicos().criar(objeto("nome", "Tópico Java",
                "visibilidade", "publico", "assinatura_padrao", "opt_in")).json();
        JsonNode contato = sdk.contatos().criar(objeto("email", "contato@example.test",
                "primeiro_nome", "Sintético", "descadastrado", Boolean.FALSE,
                "segmentos", array(id(segmento)), "topicos", array(objeto("id", id(topico), "assinatura", "opt_in")))).json();
        exigir(sdk.contatos().obter(id(contato)).json().get("id").equals(contato.get("id"))
                && contato.get("descadastrado").isBoolean() && !contato.get("descadastrado").booleanValue());
        JsonNode importacao = sdk.contatos().importacoes().criar(objeto("arquivo_nome", "fixture.csv",
                "conteudo", "email,primeiro_nome\ncontato@example.test,Importado\n",
                "mapeamento", objeto("0", "email", "1", "primeiro_nome")), idempotencia("java-importacao")).json();
        // Sem worker: a importação aceita conserva os contadores pendentes.
        exigir(texto(importacao, "estado").equals("pendente") && numero(importacao, "total", 1)
                && numero(importacao, "processados", 0));
        JsonNode atualizado = sdk.contatos().atualizar(id(contato), objeto("primeiro_nome", null)).json();
        exigir(atualizado.has("primeiro_nome") && atualizado.get("primeiro_nome").isNull());
        JsonNode exportacao = sdk.contatos().exportacoes().criar(objeto(), idempotencia("java-exportacao")).json();
        exigir(texto(exportacao, "estado").equals("concluida") && numero(exportacao, "total", 1));
        final ByteArrayOutputStream csv = new ByteArrayOutputStream();
        Resposta csvResposta = sdk.contatos().exportacoes().baixarFluxo(id(exportacao), bytes -> csv.write(bytes));
        exigir(new String(csv.toByteArray(), StandardCharsets.UTF_8).contains("contato@example.test")
                && csvResposta.totalBytes() == csv.size() && csvResposta.dados().length == 0);
        final ByteArrayOutputStream csvInjetado = new ByteArrayOutputStream();
        Resposta csvOutra = sdkInjetado.contatos().exportacoes().baixarFluxo(id(exportacao), bytes -> csvInjetado.write(bytes));
        exigir(Arrays.equals(csv.toByteArray(), csvInjetado.toByteArray()) && csvOutra.totalBytes() == csvInjetado.size());
        exigir(lista(sdk.contatos().exportacoes().listar(null).json(), "dados").size() == 1);
        concluir();

        etapa = "modelos_criar_pasta";
        JsonNode pasta = sdk.modelos().pastas().criar(objeto("nome", "Pasta Java")).json();
        etapa = "modelos_criar";
        JsonNode modelo = sdk.modelos().criar(objeto("nome", "Modelo Java", "html", "<p>Olá, {{{NOME}}}.</p>",
                "pasta_id", id(pasta), "remetente", remetente, "assunto", "Modelo sintético",
                "variaveis", array(objeto("chave", "NOME", "tipo", "string")))).json();
        etapa = "modelos_versoes_obter";
        JsonNode versoes = lista(sdk.modelos().versoes().listar(id(modelo), null).json(), "dados");
        exigir(versoes.size() > 0);
        JsonNode versao = versoes.get(0);
        JsonNode versaoObtida = sdk.modelos().versoes().obter(id(modelo), id(versao)).json();
        exigir(versaoObtida.get("versao_id").equals(versao.get("id")) && versaoObtida.get("id").equals(modelo.get("id")));
        etapa = "modelos_prever";
        JsonNode previa = sdk.modelos().prever(id(modelo), objeto("revisao", modelo.get("revisao"),
                "variaveis", objeto("NOME", "Java"))).json();
        exigir(texto(previa, "html").contains("Java"));
        String prefixo = "/v1/template-previews/" + texto(fixture, "organizacao") + "/";
        String endereco = texto(previa, "url");
        exigir(endereco.startsWith(prefixo));
        String previaId = endereco.substring(prefixo.length());
        exigir(!previaId.isEmpty() && !previaId.contains("/"));
        etapa = "modelos_baixar_previa";
        exigir(new String(sdk.modelos().obterPrevia(texto(fixture, "organizacao"), previaId).dados(),
                StandardCharsets.UTF_8).contains("Java"));
        etapa = "modelos_atualizar_cas";
        final JsonNode antiga = modelo.get("revisao");
        modelo = sdk.modelos().atualizar(id(modelo), objeto("revisao", antiga, "nome", "Modelo editado")).json();
        final String modeloId = id(modelo);
        esperarErro(409, () -> sdk.modelos().atualizar(modeloId, objeto("revisao", antiga, "nome", "Conflito")));
        etapa = "modelos_publicar_restaurar";
        modelo = sdk.modelos().publicar(modeloId, objeto("revisao", modelo.get("revisao"))).json();
        exigir(texto(modelo, "status").equals("published"));
        modelo = sdk.modelos().versoes().restaurar(modeloId, id(versao), objeto("revisao", modelo.get("revisao"))).json();
        etapa = "modelos_pastas_versoes_previa_publicacao_cas";
        concluir();

        etapa = "marketing_objeto_vazio_crud_sem_disparo";
        JsonNode campanha = sdk.campanhas().criar(objeto("nome", "Campanha Java")).json();
        campanha = sdk.campanhas().atualizar(id(campanha), objeto("revisao", campanha.get("revisao"), "nome", "Campanha editada")).json();
        exigir(sdk.campanhas().obter(id(campanha)).json().get("id").equals(campanha.get("id")));
        sdk.campanhas().remover(id(campanha));
        JsonNode evento = sdk.eventos().criar(objeto("nome", "java.qa", "esquema", objeto())).json();
        exigir(evento.get("esquema").isObject() && evento.get("esquema").size() == 0);
        JsonNode eventoNulo = sdk.eventos().atualizar(id(evento), objeto("esquema", null)).json();
        exigir(eventoNulo.has("esquema") && eventoNulo.get("esquema").isNull());
        JsonNode automacao = sdk.automacoes().criar(objeto("nome", "Automação Java", "estado", "inativa")).json();
        automacao = sdk.automacoes().atualizar(id(automacao), objeto("revisao", automacao.get("revisao"), "nome", "Automação editada")).json();
        exigir(sdk.automacoes().obter(id(automacao)).json().get("id").equals(automacao.get("id")));
        sdk.automacoes().remover(id(automacao));
        sdk.eventos().remover(id(evento));
        concluir();

        etapa = "caixas_etiquetas_rascunhos_cas_sem_envio";
        JsonNode caixa = sdk.caixas().criar(objeto("endereco", "suporte@" + texto(fixture, "nome_dominio"), "nome", "Suporte Java")).json();
        exigir(sdk.caixas().obter(id(caixa)).json().get("id").equals(caixa.get("id")));
        JsonNode etiqueta = sdk.caixas().etiquetas().criar(id(caixa), objeto("nome", "Pedidos", "cor", "#224466")).json();
        JsonNode rascunho = sdk.caixas().rascunhos().criar(id(caixa), objeto("para", array("cliente@example.test"), "assunto", "Pedido", "texto", "Olá")).json();
        final JsonNode revisaoRascunho = rascunho.get("revisao");
        rascunho = sdk.caixas().rascunhos().atualizar(id(caixa), id(rascunho), objeto("revisao", revisaoRascunho, "texto", "Atualizado")).json();
        final String caixaId = id(caixa);
        final String rascunhoId = id(rascunho);
        esperarErro(409, () -> sdk.caixas().rascunhos().atualizar(caixaId, rascunhoId, objeto("revisao", revisaoRascunho, "texto", "Conflito")));
        JsonNode corpoRascunho = sdk.caixas().rascunhos().obter(caixaId, rascunhoId).json();
        exigir(corpoRascunho.has("html") && corpoRascunho.get("html").isNull());
        sdk.caixas().rascunhos().remover(caixaId, rascunhoId);
        sdk.caixas().etiquetas().remover(caixaId, id(etiqueta));
        sdk.caixas().remover(caixaId);
        concluir();

        etapa = "recebimento_mime_real_pendente_bytes_fluxos_padrao_injetado";
        exigir(lista(sdk.recebimento().listar(null).json(), "dados").size() == 0);
        String caminho = "/interno/postal/recebidos/" + texto(fixture, "rota");
        Map<String, List<String>> headers = cabecalhos("Content-Type", "application/json",
                "X-Postal-Signature-256", texto(fixture, "callback_assinatura"));
        String callbackCorpo = texto(fixture, "callback_corpo");
        ObjectNode callback = requisitar(caminho, "POST", headers, callbackCorpo);
        exigir(numero(callback, "status", 200));
        String recebido = id(callback.get("dados"));
        ObjectNode repetida = requisitar(caminho, "POST", headers, callbackCorpo);
        exigir(numero(repetida, "status", 200) && id(repetida.get("dados")).equals(recebido));
        byte[] mime = Base64.getDecoder().decode(texto(fixture, "mime_esperado"));
        Map<String, Object> formato = new LinkedHashMap<String, Object>();
        formato.put("formato_html", "cid");
        JsonNode detalhe = sdk.recebimento().obter(recebido, formato).json();
        exigir(texto(detalhe, "estado_processamento").equals("pendente")
                && lista(detalhe, "para").equals(array("real@" + texto(fixture, "nome_dominio"))));
        Resposta original = sdk.recebimento().baixarOriginal(recebido);
        exigir(original.status() == 200 && "message/rfc822".equals(cabecalho(original, "content-type"))
                && Arrays.equals(original.dados(), mime));
        final ByteArrayOutputStream bruto = new ByteArrayOutputStream();
        Resposta fluxo = sdk.recebimento().baixarOriginalFluxo(recebido, bytes -> bruto.write(bytes));
        exigir(Arrays.equals(bruto.toByteArray(), mime) && fluxo.totalBytes() == mime.length && fluxo.dados().length == 0);
        final ByteArrayOutputStream brutoInjetado = new ByteArrayOutputStream();
        Resposta fluxoInjetado = sdkInjetado.recebimento().baixarOriginalFluxo(recebido, bytes -> brutoInjetado.write(bytes));
        exigir(Arrays.equals(brutoInjetado.toByteArray(), mime) && fluxoInjetado.totalBytes() == mime.length
                && "message/rfc822".equals(cabecalho(fluxoInjetado, "content-type")));
        exigir(lista(sdk.recebimento().listar(null).json(), "dados").size() == 1);
        esperarErro(403, () -> envio.recebimento().listar(null));
        concluir();

        etapa = "cli_revogacao_e_limpeza_crud";
        exigir(lista(sdk.cli().concessoes().listar(consulta(1)).json(), "dados").size() == 0);
        esperarErro(403, () -> envio.cli().concessoes().listar(null));
        List<Resposta> removidas = Arrays.asList(sdk.contatos().remover(id(contato)),
                sdk.segmentos().remover(id(segmento)), sdk.topicos().remover(id(topico)),
                sdk.modelos().remover(id(modelo)), sdk.modelos().pastas().remover(id(pasta)),
                sdk.chavesApi().revogar(id(chaveEnvio)));
        for (Resposta removida : removidas) {
            exigir(removida.status() >= 200 && removida.status() <= 299);
        }
        esperarErro(401, () -> envio.emails().enviar(pedido, idempotencia("java-revogada")));
        concluir();
        return objeto("resultado", "aprovado", "cenarios", cenarios,
                "recebimento_processado_por_worker", Boolean.FALSE, "importacao_processada_por_worker", Boolean.FALSE,
                "callbacks_fluxo_padrao", Integer.valueOf(2), "callbacks_fluxo_http_injetado", Integer.valueOf(2),
                "http_injetado_provedor", "okhttp", "leituras_concorrentes_mesmo_cliente", Integer.valueOf(8),
                "politica_http_injetado", "tls_ca_explicita_sem_proxy_redirect_retry_conexao_call_timeout",
                "protocolos_http_helper_observados", protocolos);
    }

    private ObjectNode executar() {
        try {
            return executarCenarios();
        } catch (Throwable erro) {
            if (erro instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            ObjectNode relatorio = objeto("resultado", "falhou", "etapa", etapa, "cenarios", cenarios,
                    "falha", "Contrato de integração não satisfeito; detalhes privados omitidos.");
            if (erro instanceof ErroMailcore) {
                ErroMailcore api = (ErroMailcore) erro;
                relatorio.set("erro_http", objeto("codigo", api.codigo(), "status", api.status()));
            } else if (erro instanceof Falha && ((Falha) erro).codigo != null) {
                Falha api = (Falha) erro;
                relatorio.set("erro_http", objeto("codigo", api.codigo, "status", api.status));
            }
            return relatorio;
        }
    }

    @Override
    public void close() {
        for (int indice = clientes.size() - 1; indice >= 0; indice--) {
            try {
                clientes.get(indice).close();
            } catch (RuntimeException ignorada) {
                // Todos os outros recursos do laboratório ainda precisam ser liberados.
            }
        }
        http.connectionPool().evictAll();
        http.dispatcher().executorService().shutdown();
    }

    /** Recebe uma única fixture via stdin; não aceita credenciais em argumentos. */
    public static void main(String[] argumentos) {
        ObjectNode relatorio;
        try {
            JsonNode entrada = JSON.readTree(lerLimitado(System.in));
            exigir(entrada != null && entrada.isObject());
            try (IntegracaoApi ensaio = new IntegracaoApi((ObjectNode) entrada)) {
                relatorio = ensaio.executar();
            }
        } catch (Throwable erro) {
            if (erro instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            relatorio = objeto("resultado", "falhou", "etapa", "configuracao",
                    "falha", "Não foi possível iniciar o cliente sintético.");
        }
        relatorio.put("java", System.getProperty("java.version"));
        try {
            System.out.println(JSON.writeValueAsString(relatorio));
        } catch (IOException erro) {
            System.out.println("{\"resultado\":\"falhou\",\"etapa\":\"relatorio\"}");
            System.exit(1);
        }
        if (!"aprovado".equals(relatorio.path("resultado").asText())) {
            System.exit(1);
        }
    }
}
