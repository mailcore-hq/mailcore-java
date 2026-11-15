package br.com.mailcore.consumidor;

import br.com.mailcore.sdk.Assinatura;
import br.com.mailcore.sdk.Cliente;
import br.com.mailcore.sdk.Configuracao;
import br.com.mailcore.sdk.ConsumidorFluxo;
import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.EventoWebhook;
import br.com.mailcore.sdk.OpcoesAssinatura;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.PedidoEmail;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.RespostaEnvio;
import br.com.mailcore.sdk.SinalCancelamento;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/** Consumidor externo do JAR instalado; nenhuma chamada alcança a rede. */
public final class ConsumidorInstalado {
    private static final String TOKEN = "mc_ensaio_instalacao_sintetico_01234567890123456789";
    private static final String IDEMPOTENCIA = "consumidor-instalado-idempotencia";
    private static final String ID = "opaco/mais+teste@example.test";
    private static final String ID_CODIFICADO = "opaco%2Fmais%2Bteste%40example.test";
    private static final String RECURSOS = "br.com.mailcore.sdk.recursos.";
    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final BigInteger INTEIRO = new BigInteger("18446744073709551617012345678901234567890");
    private static final BigDecimal DECIMAL = new BigDecimal("1234567890.123456789012345678901234567890");
    private static final byte[] BINARIO = new byte[] {0, 1, 13, 10, (byte) 255, 42};
    private static final byte[] HTML = "<p>Prévia sintética.</p>".getBytes(StandardCharsets.UTF_8);
    private static String etapa = "inicio";

    private ConsumidorInstalado() {}

    public static void main(String[] argumentos) {
        try {
            System.out.println(MAPEADOR.writeValueAsString(executar()));
        } catch (Throwable falha) {
            // A mensagem, a causa e a pilha podem conter dados da aplicação.
            System.out.println("{\"resultado\":\"falha\",\"etapa\":\"" + etapa + "\"}");
            System.exit(1);
        }
    }

    private static Map<String, Object> executar() throws Exception {
        etapa = "origem_jar";
        URL origem = Cliente.class.getProtectionDomain().getCodeSource().getLocation();
        exigir("file".equals(origem.getProtocol())
                && origem.getPath().endsWith(".jar")
                && Files.isRegularFile(Paths.get(origem.toURI())));

        Simulado simulado = new Simulado();
        OkHttpClient http = new OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .cookieJar(CookieJar.NO_COOKIES)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .connectTimeout(Duration.ofSeconds(1))
                .callTimeout(Duration.ofSeconds(5))
                .addInterceptor(simulado)
                .build();
        try {
            return conferirCliente(http, simulado);
        } finally {
            http.connectionPool().evictAll();
            http.dispatcher().executorService().shutdown();
        }
    }

    private static Map<String, Object> conferirCliente(OkHttpClient http, Simulado simulado) throws Exception {
        Configuracao configuracao = Configuracao.construtor()
                .urlBase("https://api.example.test/v1")
                .clienteHttp(http)
                .tempoLimite(Duration.ofSeconds(5))
                .construir();
        OpcoesRequisicao opcoes = OpcoesRequisicao.construtor()
                .chaveIdempotencia(IDEMPOTENCIA)
                .cabecalhos(Collections.singletonMap("X-Consumidor", Collections.singletonList("instalado")))
                .construir();

        Contagem contagem = new Contagem();
        Cliente cliente = new Cliente(TOKEN, configuracao);
        try {
            etapa = "superficie_publica";
            percorrer(cliente, simulado, opcoes, contagem,
                    Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
            exigir(contagem.recursos == 30 && contagem.metodos == 151 && contagem.callbacks == 3);
            exigir(simulado.requisicoes == 151);

            etapa = "pedido_tipado";
            PedidoEmail pedido = PedidoEmail.construtor()
                    .remetente("origem@example.test")
                    .destinatarios(Collections.singletonList("destino@example.test"))
                    .assunto("Assunto sintético")
                    .texto("Conteúdo sintético")
                    .construir();
            ((ObjectNode) pedido.dados()).put("assunto", "Cópia alterada");
            simulado.modo = "tipado";
            simulado.esperaConsulta = false;
            simulado.esperaId = false;
            Resposta tipada = cliente.emails().enviar(pedido, opcoes);
            exigir(tipada.status() == 202);
            RespostaEnvio envio = tipada.decodificar(RespostaEnvio.class);
            exigir("intencao_sintetica".equals(envio.id()) && "enfileirado".equals(envio.estado()));
            byte[] copia = tipada.dados();
            copia[0] = '!';
            exigir(tipada.json().path("id").asText().equals("intencao_sintetica"));
            conferirCabecalhosResposta(tipada);

            etapa = "valores_json";
            simulado.modo = "eco";
            Resposta eco = cliente.eventos().enviar(corpo(), opcoes);
            JsonNode json = eco.json();
            exigir(json.path("inteiro").bigIntegerValue().equals(INTEIRO));
            exigir(json.path("decimal").decimalValue().equals(DECIMAL));
            exigir(json.path("objeto").isObject() && json.path("objeto").size() == 0);
            exigir(json.path("lista").isArray() && json.path("lista").size() == 0);
            exigir(json.has("nulo") && json.path("nulo").isNull());
            exigir(json.path("falso").isBoolean() && !json.path("falso").booleanValue());
            Map<String, Object> decodificado = eco.decodificar(new TypeReference<Map<String, Object>>() {});
            exigir(INTEIRO.equals(decodificado.get("inteiro")) && DECIMAL.equals(decodificado.get("decimal")));
            conferirCabecalhosResposta(eco);

            etapa = "erro_sanitizado";
            simulado.modo = "erro";
            ErroMailcore erro = esperarErro(() -> cliente.emails().listar(null, opcoes));
            exigir("indisponivel".equals(erro.codigo()) && Integer.valueOf(503).equals(erro.status()));
            exigir(erro.mensagem().contains(TOKEN));
            exigir(erro.getCause() == null && !erro.toString().contains(TOKEN)
                    && !erro.getMessage().contains(TOKEN));
            exigir(!cliente.toString().contains(TOKEN) && !configuracao.toString().contains(TOKEN)
                    && !opcoes.toString().contains(TOKEN) && !pedido.toString().contains(TOKEN));
            conferirCabecalhos(erro.cabecalhos());

            etapa = "resposta_vazia";
            simulado.modo = "vazio";
            simulado.esperaId = true;
            Resposta vazia = cliente.emails().cancelar(ID, opcoes);
            exigir(vazia.status() == 204 && vazia.totalBytes() == 0 && vazia.dados().length == 0);

            etapa = "cabecalhos_reservados";
            int antes = simulado.requisicoes;
            for (String nome : Arrays.asList("Authorization", "Cookie", "Host", "Content-Length", "Idempotency-Key", "Accept")) {
                OpcoesRequisicao proibidas = OpcoesRequisicao.construtor()
                        .cabecalhos(Collections.singletonMap(nome, Collections.singletonList("sintetico")))
                        .construir();
                ErroMailcore rejeicao = esperarErro(() -> cliente.emails().listar(null, proibidas));
                exigir("configuracao_invalida".equals(rejeicao.codigo()) && rejeicao.status() == null);
            }
            OpcoesRequisicao controle = OpcoesRequisicao.construtor()
                    .cabecalhos(Collections.singletonMap("X-Consumidor", Collections.singletonList("linha\nindevida")))
                    .construir();
            exigir("configuracao_invalida".equals(esperarErro(() -> cliente.emails().listar(null, controle)).codigo()));
            exigir(simulado.requisicoes == antes);

            etapa = "cancelamento";
            SinalCancelamento sinal = new SinalCancelamento();
            sinal.cancelar();
            OpcoesRequisicao canceladas = OpcoesRequisicao.construtor().sinalCancelamento(sinal).construir();
            exigir("cancelado".equals(esperarErro(() -> cliente.emails().listar(null, canceladas)).codigo()));
            exigir(simulado.requisicoes == antes);

            etapa = "assinatura_webhook";
            conferirAssinatura();
        } finally {
            cliente.close();
        }

        etapa = "encerramento";
        int antes = simulado.requisicoes;
        exigir("cliente_fechado".equals(esperarErro(() -> cliente.emails().listar()).codigo()));
        exigir(simulado.requisicoes == antes && antes == 155);
        exigir(!http.dispatcher().executorService().isShutdown());

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("resultado", "aprovado");
        resultado.put("java", System.getProperty("java.version"));
        resultado.put("versao_sdk", "0.1.0-alpha.1");
        resultado.put("recursos", contagem.recursos);
        resultado.put("metodos", contagem.metodos);
        resultado.put("callbacks_fluxo", contagem.callbacks);
        resultado.put("operacoes_extras", 4);
        resultado.put("requisicoes_http", simulado.requisicoes);
        resultado.put("origem_jar", true);
        resultado.put("pedido_tipado", true);
        resultado.put("json_preciso", true);
        resultado.put("assinatura_webhook", true);
        resultado.put("erro_sanitizado", true);
        resultado.put("cabecalhos_protegidos", true);
        resultado.put("cancelamento_sem_rede", true);
        resultado.put("cliente_http_preservado", true);
        resultado.put("rede_real", false);
        return resultado;
    }

    private static void percorrer(Object recurso, Simulado simulado, OpcoesRequisicao opcoes,
            Contagem contagem, Set<Object> visitados) throws Exception {
        if (!visitados.add(recurso)) {
            return;
        }
        if (recurso.getClass().getName().startsWith(RECURSOS)) {
            contagem.recursos++;
        }
        Method[] metodos = recurso.getClass().getDeclaredMethods();
        Arrays.sort(metodos, Comparator.comparing(Method::toGenericString));
        for (Method metodo : metodos) {
            if (!Modifier.isPublic(metodo.getModifiers()) || metodo.isSynthetic()) {
                continue;
            }
            Class<?>[] tipos = metodo.getParameterTypes();
            if (tipos.length == 0 && metodo.getReturnType().getName().startsWith(RECURSOS)) {
                percorrer(invocar(metodo, recurso, new Object[0]), simulado, opcoes, contagem, visitados);
                continue;
            }
            if (metodo.getReturnType() != Resposta.class || tipos.length == 0
                    || tipos[tipos.length - 1] != OpcoesRequisicao.class) {
                continue;
            }
            simulado.modo = "normal";
            simulado.esperaConsulta = false;
            simulado.esperaId = false;
            ByteArrayOutputStream recebido = new ByteArrayOutputStream();
            boolean fluxo = false;
            Object[] argumentos = new Object[tipos.length];
            for (int i = 0; i < tipos.length; i++) {
                if (tipos[i] == OpcoesRequisicao.class) {
                    argumentos[i] = opcoes;
                } else if (tipos[i] == String.class) {
                    argumentos[i] = ID;
                    simulado.esperaId = true;
                } else if (tipos[i] == Object.class) {
                    argumentos[i] = recurso.getClass().getSimpleName().equals("Lotes")
                            ? Collections.singletonList(corpo()) : corpo();
                } else if (Map.class.isAssignableFrom(tipos[i])) {
                    argumentos[i] = consulta();
                    simulado.esperaConsulta = true;
                } else if (List.class.isAssignableFrom(tipos[i])) {
                    argumentos[i] = recurso.getClass().getSimpleName().equals("TopicosContato")
                            ? Collections.singletonList(Collections.singletonMap("id", "topico_sintetico"))
                            : Collections.singletonList("etiqueta_sintetica");
                } else if (tipos[i] == int.class) {
                    argumentos[i] = 0;
                } else if (tipos[i] == ConsumidorFluxo.class) {
                    argumentos[i] = (ConsumidorFluxo) parte -> recebido.write(parte);
                    fluxo = true;
                } else {
                    throw new IllegalStateException("assinatura_inesperada");
                }
            }
            int antes = simulado.requisicoes;
            Resposta resposta = (Resposta) invocar(metodo, recurso, argumentos);
            exigir(simulado.requisicoes == antes + 1 && resposta.status() == 200);
            conferirCabecalhosResposta(resposta);
            if (fluxo) {
                exigir(Arrays.equals(recebido.toByteArray(), BINARIO));
                exigir(resposta.dados().length == 0 && resposta.totalBytes() == BINARIO.length);
                contagem.callbacks++;
            } else if ("application/json".equals(simulado.ultimoAceite)) {
                exigir(resposta.json().isObject() && resposta.json().size() == 0);
            } else {
                byte[] esperado = "text/html".equals(simulado.ultimoAceite) ? HTML : BINARIO;
                exigir(Arrays.equals(resposta.dados(), esperado) && resposta.totalBytes() == esperado.length);
            }
            contagem.metodos++;
        }
    }

    private static Object invocar(Method metodo, Object recurso, Object[] argumentos) throws Exception {
        try {
            return metodo.invoke(recurso, argumentos);
        } catch (InvocationTargetException falha) {
            Throwable causa = falha.getCause();
            if (causa instanceof Exception) {
                throw (Exception) causa;
            }
            if (causa instanceof Error) {
                throw (Error) causa;
            }
            throw new IllegalStateException("invocacao_invalida");
        }
    }

    private static Map<String, Object> corpo() {
        Map<String, Object> valor = new LinkedHashMap<>();
        valor.put("inteiro", INTEIRO);
        valor.put("decimal", DECIMAL);
        valor.put("objeto", Collections.emptyMap());
        valor.put("lista", Collections.emptyList());
        valor.put("nulo", null);
        valor.put("falso", false);
        return valor;
    }

    private static Map<String, Object> consulta() {
        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("ativo", false);
        filtros.put("ausente", null);
        filtros.put("limite", 1);
        filtros.put("busca", "ação + teste");
        return filtros;
    }

    private static void conferirAssinatura() throws Exception {
        Instant agora = Instant.parse("2026-11-14T12:00:00Z");
        String instante = Long.toString(agora.getEpochSecond());
        String id = "msg_sintetica";
        byte[] chave = new byte[32];
        Arrays.fill(chave, (byte) 7);
        String segredo = "whsec_" + Base64.getEncoder().encodeToString(chave);
        byte[] corpo = ("{\"type\":\"email.received\",\"created_at\":\"2026-11-14T12:00:00Z\","
                + "\"data\":{\"email_id\":\"identificador_sintetico\"}}")
                .getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(chave, "HmacSHA256"));
        mac.update((id + "." + instante + ".").getBytes(StandardCharsets.US_ASCII));
        Map<String, List<String>> cabecalhos = new LinkedHashMap<>();
        cabecalhos.put("Svix-Id", Collections.singletonList(id));
        cabecalhos.put("svix-timestamp", Collections.singletonList(instante));
        cabecalhos.put("svix-signature", Collections.singletonList("v1," + Base64.getEncoder().encodeToString(mac.doFinal(corpo))));
        OpcoesAssinatura opcoes = OpcoesAssinatura.construtor().agora(agora).construir();
        EventoWebhook evento = Assinatura.verificar(corpo, cabecalhos, segredo, opcoes);
        exigir("email.received".equals(evento.tipo()));
        exigir("identificador_sintetico".equals(evento.dados().path("email_id").asText()));
        byte[] alterado = Arrays.copyOf(corpo, corpo.length + 1);
        alterado[alterado.length - 1] = ' ';
        ErroMailcore rejeicao = esperarErro(() -> Assinatura.verificar(alterado, cabecalhos, segredo, opcoes));
        exigir("assinatura_invalida".equals(rejeicao.codigo()) && rejeicao.getCause() == null
                && !rejeicao.toString().contains(segredo));
    }

    private static ErroMailcore esperarErro(Runnable operacao) {
        try {
            operacao.run();
        } catch (ErroMailcore erro) {
            return erro;
        }
        throw new IllegalStateException("erro_esperado");
    }

    private static void conferirCabecalhosResposta(Resposta resposta) {
        conferirCabecalhos(resposta.cabecalhos());
        exigir(!resposta.toString().contains(TOKEN));
    }

    private static void conferirCabecalhos(Map<String, List<String>> cabecalhos) {
        boolean preservado = false;
        for (Map.Entry<String, List<String>> entrada : cabecalhos.entrySet()) {
            exigir(!entrada.getKey().equalsIgnoreCase("Set-Cookie")
                    && !entrada.getKey().equalsIgnoreCase("Authorization")
                    && !entrada.getKey().equalsIgnoreCase("Proxy-Authorization"));
            if (entrada.getKey().equalsIgnoreCase("X-Ensaio")) {
                preservado = entrada.getValue().equals(Collections.singletonList("preservado"));
            }
        }
        exigir(preservado);
    }

    private static void exigir(boolean condicao) {
        if (!condicao) {
            throw new IllegalStateException("verificacao_recusada");
        }
    }

    private static final class Contagem {
        private int recursos;
        private int metodos;
        private int callbacks;
    }

    private static final class Simulado implements Interceptor {
        private int requisicoes;
        private String modo = "normal";
        private boolean esperaConsulta;
        private boolean esperaId;
        private String ultimoAceite;

        @Override
        public Response intercept(Chain cadeia) throws IOException {
            // Interceptor de aplicação: não chama proceed nem abre conexões.
            Request pedido = cadeia.request();
            requisicoes++;
            exigir("https".equals(pedido.url().scheme()) && "api.example.test".equals(pedido.url().host()));
            exigir(pedido.url().encodedPath().startsWith("/v1/"));
            exigir(pedido.url().username().isEmpty() && pedido.url().password().isEmpty());
            exigir(("Bearer " + TOKEN).equals(pedido.header("Authorization")));
            exigir(IDEMPOTENCIA.equals(pedido.header("Idempotency-Key")));
            exigir("mailcore-java/0.1.0-alpha.1".equals(pedido.header("User-Agent")));
            exigir("identity".equals(pedido.header("Accept-Encoding")));
            exigir("instalado".equals(pedido.header("X-Consumidor")));
            exigir(pedido.header("Cookie") == null && pedido.header("Proxy-Authorization") == null);
            if (esperaId) {
                exigir(pedido.url().encodedPath().contains(ID_CODIFICADO));
            }
            if (esperaConsulta) {
                exigir("false".equals(pedido.url().queryParameter("ativo")));
                exigir("1".equals(pedido.url().queryParameter("limite")));
                exigir("ação + teste".equals(pedido.url().queryParameter("busca")));
                exigir(!pedido.url().queryParameterNames().contains("ausente"));
            }
            RequestBody envio = pedido.body();
            byte[] enviados = new byte[0];
            if (envio != null) {
                exigir(envio.isOneShot());
                Buffer buffer = new Buffer();
                envio.writeTo(buffer);
                enviados = buffer.readByteArray();
                exigir(envio.contentLength() == enviados.length);
                if (enviados.length > 0) {
                    exigir(envio.contentType() != null && "application".equals(envio.contentType().type())
                            && "json".equals(envio.contentType().subtype()));
                    JsonNode objeto = MAPEADOR.readTree(enviados);
                    exigir(pedido.url().encodedPath().equals("/v1/emails/batch") ? objeto.isArray() : objeto.isObject());
                }
            }
            ultimoAceite = pedido.header("Accept");
            int status = 200;
            String tipo = ultimoAceite;
            byte[] dados;
            if ("erro".equals(modo)) {
                status = 503;
                tipo = "application/json";
                dados = MAPEADOR.writeValueAsBytes(Collections.singletonMap("erro",
                        mapaErro()));
            } else if ("vazio".equals(modo)) {
                status = 204;
                tipo = "application/json";
                dados = new byte[0];
            } else if ("tipado".equals(modo)) {
                JsonNode objeto = MAPEADOR.readTree(enviados);
                exigir("POST".equals(pedido.method()) && "/v1/emails".equals(pedido.url().encodedPath()));
                exigir("origem@example.test".equals(objeto.path("remetente").asText()));
                exigir(objeto.path("destinatarios").isArray() && objeto.path("destinatarios").size() == 1);
                exigir("destino@example.test".equals(objeto.path("destinatarios").get(0).asText()));
                exigir("Assunto sintético".equals(objeto.path("assunto").asText()));
                exigir("Conteúdo sintético".equals(objeto.path("texto").asText()));
                status = 202;
                tipo = "application/json";
                dados = "{\"id\":\"intencao_sintetica\",\"estado\":\"enfileirado\"}".getBytes(StandardCharsets.UTF_8);
            } else if ("eco".equals(modo)) {
                exigir("POST".equals(pedido.method()) && "/v1/events/send".equals(pedido.url().encodedPath()));
                exigir(enviados.length > 0);
                tipo = "application/json";
                dados = enviados;
            } else if ("application/json".equals(tipo)) {
                dados = "{}".getBytes(StandardCharsets.UTF_8);
            } else if ("text/html".equals(tipo)) {
                dados = HTML.clone();
            } else {
                exigir("application/octet-stream".equals(tipo));
                dados = BINARIO.clone();
            }
            final String conteudoTipo = tipo;
            final byte[] conteudo = dados;
            final Buffer fonte = new Buffer().write(conteudo);
            ResponseBody corpo = new ResponseBody() {
                @Override public MediaType contentType() { return MediaType.get(conteudoTipo); }
                @Override public long contentLength() { return conteudo.length; }
                @Override public BufferedSource source() { return fonte; }
            };
            return new Response.Builder()
                    .request(pedido)
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("Simulado")
                    .header("Content-Type", tipo)
                    .header("Content-Length", Integer.toString(dados.length))
                    .header("X-Ensaio", "preservado")
                    .header("Set-Cookie", TOKEN)
                    .header("Authorization", TOKEN)
                    .header("Proxy-Authorization", TOKEN)
                    .body(corpo)
                    .build();
        }

        private Map<String, Object> mapaErro() {
            Map<String, Object> erro = new LinkedHashMap<>();
            erro.put("codigo", "indisponivel");
            erro.put("mensagem", "Informação privada " + TOKEN);
            return erro;
        }
    }
}
