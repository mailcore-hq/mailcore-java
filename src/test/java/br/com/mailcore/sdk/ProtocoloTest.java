package br.com.mailcore.sdk;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Protocolo real em HTTPS privado; nenhum serviço externo ou dado pessoal. */
public class ProtocoloTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static Laboratorio laboratorio;
    private Cliente cliente;

    @BeforeClass public static void iniciarLaboratorio() throws Exception {
        laboratorio = new Laboratorio();
    }

    @AfterClass public static void encerrarLaboratorio() throws Exception {
        if (laboratorio != null) { laboratorio.close(); }
    }

    @Before public void preparar() throws Exception {
        laboratorio.configurar(mapa("modo", "normal"));
        cliente = new Cliente("mc_chave_sintetica_local", laboratorio.configuracao().construir());
    }

    @After public void encerrarCliente() {
        if (cliente != null) { cliente.close(); }
    }

    @Test public void envioPreservaJsonIdempotenciaEMetadados() throws Exception {
        Map<String, Object> pedido = mapa("texto", "Ol\u00e1 \ud83d\udce8", "objeto", mapa(),
                "lista", Collections.emptyList(), "nulo", null, "ativo", false);
        Resposta resposta = cliente.emails().enviar(pedido, idempotencia());
        assertEquals(200, resposta.status());
        assertEquals("email-sintetico", resposta.json().get("id").asText());
        JsonNode registro = laboratorio.registros().get(0);
        assertEquals("POST", registro.get("metodo").asText());
        assertEquals("/v1/emails", registro.get("alvo").asText());
        assertEquals("intencao-sintetica-estavel", registro.at("/cabecalhos/idempotency-key").asText());
        assertEquals("identity", registro.at("/cabecalhos/accept-encoding").asText());
        assertTrue(registro.at("/jsonCorpo/objeto").isObject());
        assertTrue(registro.at("/jsonCorpo/lista").isArray());
        assertTrue(registro.at("/jsonCorpo/nulo").isNull());
        assertFalse(registro.at("/jsonCorpo/ativo").asBoolean());
        assertEquals("Ol\u00e1 \ud83d\udce8", registro.at("/jsonCorpo/texto").asText());
    }

    @Test public void respostaMantemBytesNumerosGrandesECopiasDefensivas() throws Exception {
        byte[] original = bytes(" {\"inteiro\":123456789012345678901234567890,\"decimal\":0.12345678901234567890123456789,\"nulo\":null,\"ativo\":false,\"objeto\":{},\"lista\":[]} ");
        laboratorio.configurar(mapa("modo", "resposta", "corpo", base64(original)));
        Resposta resposta = cliente.emails().obter("id");
        assertArrayEquals(original, resposta.dados());
        Map<String, Object> dados = resposta.decodificar(new TypeReference<Map<String, Object>>() {});
        assertEquals("123456789012345678901234567890", dados.get("inteiro").toString());
        assertEquals("0.12345678901234567890123456789", dados.get("decimal").toString());
        assertTrue(dados.containsKey("nulo"));
        assertNull(dados.get("nulo"));
        byte[] copia = resposta.dados();
        copia[0] = '!';
        assertArrayEquals(original, resposta.dados());
        try {
            resposta.cabecalhos().put("x-alterado", Arrays.asList("valor"));
            fail("Headers da resposta ficaram mutáveis.");
        } catch (UnsupportedOperationException esperado) { }
    }

    @Test public void pedidoPreservaBigIntegerBigDecimalERecusaFloatsNaoFinitos() throws Exception {
        cliente.emails().enviar(mapa("inteiro", new java.math.BigInteger("123456789012345678901234567890"),
                "decimal", new java.math.BigDecimal("0.12345678901234567890123456789")));
        JsonNode corpo = laboratorio.registros().get(0).get("jsonCorpo");
        assertEquals("123456789012345678901234567890", corpo.get("inteiro").asText());
        // O controle Python usa float; confira o decimal pelos bytes efetivamente recebidos.
        JsonNode recebido = JSON.readTree(Base64.getDecoder().decode(laboratorio.registros().get(0).get("corpoBase64").asText()));
        assertEquals("0.12345678901234567890123456789", recebido.get("decimal").asText());
        laboratorio.configurar(mapa("modo", "normal"));
        for (double valor : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertEquals("configuracao_invalida", erro(() -> cliente.emails().enviar(mapa("numero", valor))).codigo());
        }
        assertEquals(0, laboratorio.registros().size());
    }

    @Test public void jsonEUnicodeInvalidosFalhamSemExporCorpos() throws Exception {
        for (byte[] invalido : new byte[][] {bytes("{} {}"), bytes("{\"incompleto\":"),
                new byte[] {'{', '"', 'x', '"', ':', '"', (byte) 255, '"', '}'}}) {
            laboratorio.configurar(mapa("modo", "resposta", "corpo", base64(invalido)));
            privado(erro(() -> cliente.emails().obter("id")));
        }
        laboratorio.configurar(mapa("modo", "normal"));
        erro(() -> cliente.emails().enviar(mapa("texto", "surrogate-\ud800")));
        assertEquals(0, laboratorio.registros().size());
    }

    @Test public void limitesJsonRespostaECabecalhosSaoAplicados() throws Exception {
        erro(() -> cliente.emails().enviar(new PedidoExtenso()));
        assertEquals(0, laboratorio.registros().size());
        laboratorio.configurar(mapa("modo", "respostaGrande", "tamanho", 16 * 1024 * 1024 + 1));
        assertEquals("resposta_excedida", erro(() -> cliente.emails().obter("id")).codigo());
        laboratorio.configurar(mapa("modo", "cabecalhosGrandes"));
        assertEquals("resposta_excedida", erro(() -> cliente.emails().obter("id")).codigo());
    }

    @Test public void tlsExigeAutoridadeEHostnameCorretos() throws Exception {
        try (Cliente outra = new Cliente("chave_sintetica", laboratorio.configuracao()
                .certificadoCa(Files.readAllBytes(Paths.get(laboratorio.info.get("outraCa").asText()))).construir())) {
            privado(erro(() -> outra.emails().obter("id")));
        }
        try (Cliente outra = new Cliente("chave_sintetica", laboratorio.configuracao()
                .urlBase(laboratorio.info.get("base").asText().replace("127.0.0.1", "localhost")).construir())) {
            privado(erro(() -> outra.emails().obter("id")));
        }
        assertEquals(200, cliente.emails().obter("id").status());
    }

    @Test public void prazoTotalIncluiHandshakeTls() throws Exception {
        try (Cliente parado = new Cliente("chave_sintetica", laboratorio.configuracao()
                .urlBase(laboratorio.info.get("tlsParado").asText())
                .tempoConexao(Duration.ofMillis(100)).tempoLimite(Duration.ofMillis(150)).construir())) {
            long inicio = System.nanoTime();
            assertEquals("prazo_excedido", erro(() -> parado.emails().obter("id")).codigo());
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio) < 1200);
        }
    }

    @Test public void errosHttpPreservamStatusHeadersESanitizamRepresentacao() throws Exception {
        for (int status : new int[] {422, 429, 503}) {
            laboratorio.configurar(mapa("modo", "erro", "status", status, "json", false));
            ErroMailcore erro = erro(() -> cliente.emails().enviar(mapa("texto", "sintetico"), idempotencia()));
            assertEquals(Integer.valueOf(status), erro.status());
            assertEquals("7", cabecalho(erro.cabecalhos(), "retry-after"));
            assertEquals("requisicao-sintetica", cabecalho(erro.cabecalhos(), "x-request-id"));
            assertNull(cabecalho(erro.cabecalhos(), "set-cookie"));
            assertNull(cabecalho(erro.cabecalhos(), "authorization"));
            privado(erro);
            assertEquals(1, laboratorio.registros().size());
        }
    }

    @Test public void mutacoesNaoRepetem503RetryAfterZero408Ou421() throws Exception {
        for (int status : new int[] {503, 408, 421}) {
            for (boolean comCorpo : new boolean[] {true, false}) {
                laboratorio.configurar(mapa("modo", "erro", "status", status, "repetirApos", "0"));
                ErroMailcore falha = erro(() -> {
                    if (comCorpo) { cliente.emails().enviar(mapa("texto", "sintetico"), idempotencia()); }
                    else { cliente.emails().cancelar("id", idempotencia()); }
                });
                assertEquals(Integer.valueOf(status), falha.status());
                assertEquals("0", cabecalho(falha.cabecalhos(), "retry-after"));
                assertEquals(1, laboratorio.registros().size());
            }
        }
    }

    @Test public void poolReutilizaConexaoESemRespostaNaoRepetePost() throws Exception {
        cliente.emails().obter("aquecer");
        cliente.emails().obter("aquecer");
        JsonNode registros = laboratorio.registros();
        assertEquals(registros.get(0).get("conexao"), registros.get(1).get("conexao"));
        for (boolean comCorpo : new boolean[] {true, false}) {
            laboratorio.configurar(mapa("modo", "quedaAquecida"));
            cliente.emails().obter("aquecer");
            erro(() -> {
                if (comCorpo) { cliente.emails().enviar(mapa("texto", "sintetico"), idempotencia()); }
                else { cliente.emails().cancelar("id", idempotencia()); }
            });
            int postagens = 0;
            for (JsonNode r : laboratorio.registros()) { if ("POST".equals(r.get("metodo").asText())) { postagens++; } }
            assertEquals("POST ambíguo foi repetido.", 1, postagens);
        }
    }

    @Test public void redirecionamentosECookiesNaoSaoSeguidos() throws Exception {
        for (int status : new int[] {301, 302, 303, 307, 308}) {
            laboratorio.configurar(mapa("modo", "redirecionamento", "status", status));
            assertEquals("redirecionamento_recusado", erro(() -> cliente.emails().enviar(mapa("texto", "sintetico"))).codigo());
            assertEquals(1, laboratorio.registros().size());
        }
        laboratorio.configurar(mapa("modo", "cookies"));
        cliente.emails().obter("id");
        cliente.emails().obter("id");
        for (JsonNode registro : laboratorio.registros()) { assertFalse(registro.get("cabecalhos").has("cookie")); }
    }

    @Test public void proxyDaJvmNaoAfetaClientePadrao() throws Exception {
        if ("1".equals(System.getProperty("mailcore.teste.proxy"))) {
            assertEquals(200, cliente.emails().obter("id").status());
            return;
        }
        // O subprocesso tem propriedades próprias; outros testes nunca recebem
        // alterações globais em ProxySelector ou System.setProperty.
        String caminhoJava = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        String classes = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Process processo = new ProcessBuilder(caminhoJava,
                "-Dmailcore.teste.proxy=1", "-Dhttp.proxyHost=127.0.0.1", "-Dhttp.proxyPort=9",
                "-Dhttps.proxyHost=127.0.0.1", "-Dhttps.proxyPort=9", "-Dhttp.nonProxyHosts=",
                "-cp", classes, SondaProxy.class.getName()).redirectErrorStream(true).start();
        try {
            assertTrue("A sonda isolada não terminou.", processo.waitFor(10, TimeUnit.SECONDS));
            assertEquals("O proxy da JVM afetou o cliente padrão.", 0, processo.exitValue());
        } finally { processo.destroyForcibly(); processo.getInputStream().close(); }
    }

    @Test public void fluxoOitoMiBProgressivoNaoAcumulaResposta() throws Exception {
        laboratorio.configurar(mapa("modo", "fluxoGrande", "esperarConsumidor", true));
        AtomicLong total = new AtomicLong();
        Resposta resposta = cliente.recebimento().baixarOriginalFluxo("id", parte -> {
            if (total.get() == 0) { laboratorio.comando(mapa("acao", "liberar")); }
            assertTrue(parte.length > 0);
            for (byte valor : parte) { assertEquals('a', valor); }
            total.addAndGet(parte.length);
        });
        assertEquals(8 * 1024 * 1024, total.get());
        assertEquals(total.get(), resposta.totalBytes());
        assertEquals(0, resposta.dados().length);
    }

    @Test public void blocosDoFluxoPertencemAoChamador() throws Exception {
        laboratorio.configurar(mapa("modo", "fluxo", "esperarConsumidor", true));
        List<byte[]> partes = new ArrayList<>();
        cliente.recebimento().baixarOriginalFluxo("id", parte -> {
            partes.add(parte);
            if (partes.size() == 1) { laboratorio.comando(mapa("acao", "liberar")); }
        });
        assertTrue(partes.size() >= 2);
        assertArrayEquals(bytes("abc"), partes.get(0));
        assertNotSame(partes.get(0), partes.get(1));
    }

    @Test public void fluxoRecusaTruncamentoCodificacaoELimite() throws Exception {
        for (String moldura : Arrays.asList("tamanho", "chunked")) {
            laboratorio.configurar(mapa("modo", "truncada", "moldura", moldura, "corpo", base64(bytes("curto"))));
            erro(() -> cliente.recebimento().baixarOriginalFluxo("id", parte -> { }));
        }
        for (Object codificacao : Arrays.<Object>asList(Arrays.asList("identity", "gzip"), "identity, gzip")) {
            laboratorio.configurar(mapa("modo", "resposta", "corpo", base64(bytes("dados")),
                    "cabecalhos", mapa("Content-Encoding", codificacao)));
            AtomicInteger chamadas = new AtomicInteger();
            assertEquals("codificacao_inesperada", erro(() -> cliente.recebimento().baixarOriginalFluxo("id", p -> chamadas.incrementAndGet())).codigo());
            assertEquals(0, chamadas.get());
        }
        laboratorio.configurar(mapa("modo", "fluxoGrande"));
        AtomicLong consumidos = new AtomicLong();
        assertEquals("resposta_excedida", erro(() -> cliente.recebimento().baixarOriginalFluxo("id",
                p -> consumidos.addAndGet(p.length), OpcoesRequisicao.construtor().limiteRespostaBytes(9000).construir())).codigo());
        assertTrue(consumidos.get() <= 9000);
    }

    @Test public void callbackPreservaErroDoSdkESanitizaExcecaoExterna() throws Exception {
        laboratorio.configurar(mapa("modo", "fluxoGrande"));
        ErroMailcore original = ErroMailcore.configuracao("interrupcao_sintetica");
        assertSame(original, erro(() -> cliente.recebimento().baixarOriginalFluxo("id", p -> { throw original; })));
        laboratorio.configurar(mapa("modo", "fluxoGrande"));
        ErroMailcore falha = erro(() -> cliente.recebimento().baixarOriginalFluxo("id", p -> {
            throw new IOException("detalhe-privado-sintetico");
        }));
        assertEquals("erro_consumidor", falha.codigo());
        privado(falha);
        laboratorio.configurar(mapa("modo", "normal"));
        assertEquals(200, cliente.emails().obter("recuperacao").status());
    }

    @Test public void prazoInterrompeLeituraDoCorpo() throws Exception {
        laboratorio.configurar(mapa("modo", "fluxo", "pausa", 0.5));
        assertEquals("prazo_excedido", erro(() -> cliente.recebimento().baixarOriginal("id",
                OpcoesRequisicao.construtor().tempoLimite(Duration.ofMillis(120)).construir())).codigo());
    }

    @Test public void sinalCancelaAntesDaRedeEDuranteLeitura() throws Exception {
        SinalCancelamento antecipado = new SinalCancelamento();
        antecipado.cancelar();
        assertTrue(antecipado.cancelado());
        assertEquals("cancelado", erro(() -> cliente.emails().enviar(mapa("texto", "sintetico"),
                OpcoesRequisicao.construtor().sinalCancelamento(antecipado).construir())).codigo());
        assertEquals(0, laboratorio.registros().size());
        laboratorio.configurar(mapa("modo", "fluxo", "esperarConsumidor", true));
        SinalCancelamento sinal = new SinalCancelamento();
        CountDownLatch primeiro = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<ErroMailcore> futuro = executor.submit(() -> erro(() -> cliente.recebimento()
                    .baixarOriginalFluxo("id", p -> primeiro.countDown(),
                            OpcoesRequisicao.construtor().sinalCancelamento(sinal).construir())));
            assertTrue(primeiro.await(2, TimeUnit.SECONDS));
            sinal.cancelar();
            ErroMailcore interrompido = futuro.get(2, TimeUnit.SECONDS);
            assertEquals("cancelado", interrompido.codigo());
            privado(interrompido);
            assertTrue(sinal.cancelado());
        } finally {
            sinal.cancelar();
            laboratorio.comando(mapa("acao", "liberar"));
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        }
    }

    @Test public void concorrenciaIsolaOpcoesEForneceChavePorOperacao() throws Exception {
        AtomicInteger resolucoes = new AtomicInteger();
        try (Cliente paralelo = new Cliente(null, laboratorio.configuracao().fornecedorChave(() -> {
            resolucoes.incrementAndGet();
            return "mc_chave_resolvida_sintetica";
        }).construir())) {
            ExecutorService executor = Executors.newFixedThreadPool(4);
            try {
                List<Future<Resposta>> tarefas = new ArrayList<>();
                for (int i = 0; i < 16; i++) {
                    final String id = Integer.toString(i);
                    tarefas.add(executor.submit(() -> paralelo.emails().obter(id,
                            OpcoesRequisicao.construtor().cabecalhos(cabecalhos("x-operacao", id)).construir())));
                }
                for (Future<Resposta> tarefa : tarefas) { assertEquals(200, tarefa.get(3, TimeUnit.SECONDS).status()); }
            } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS)); }
        }
        assertEquals(16, resolucoes.get());
        assertEquals(16, laboratorio.registros().size());
        for (JsonNode r : laboratorio.registros()) {
            assertEquals("/v1/emails/" + r.at("/cabecalhos/x-operacao").asText(), r.get("alvo").asText());
        }
        try (Cliente invalido = new Cliente(null, laboratorio.configuracao().fornecedorChave(() -> null).construir())) {
            laboratorio.configurar(mapa("modo", "normal"));
            assertEquals("configuracao_invalida", erro(() -> invalido.emails().obter("id")).codigo());
            assertEquals(0, laboratorio.registros().size());
        }
    }

    @Test public void fecharRecusaNovasOperacoesEPreservaProvedorInjetado() throws Exception {
        cliente.close();
        cliente.close();
        assertEquals("cliente_fechado", erro(() -> cliente.emails().obter("id")).codigo());
        assertEquals(0, laboratorio.registros().size());
        laboratorio.configurar(mapa("modo", "fluxo", "esperarConsumidor", true));
        Cliente proprio = new Cliente("chave_sintetica", laboratorio.configuracao().construir());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch lendo = new CountDownLatch(1);
        try {
            Future<ErroMailcore> futuro = executor.submit(() -> erro(() -> proprio.recebimento()
                    .baixarOriginalFluxo("id", parte -> lendo.countDown())));
            assertTrue(lendo.await(2, TimeUnit.SECONDS));
            proprio.close();
            privado(futuro.get(2, TimeUnit.SECONDS));
        } finally {
            proprio.close(); laboratorio.comando(mapa("acao", "liberar"));
            executor.shutdownNow(); assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        }
        laboratorio.configurar(mapa("modo", "normal"));
        OkHttpClient provedor = laboratorio.provedor();
        try {
            Cliente injetado = new Cliente("chave_sintetica", laboratorio.configuracao().certificadoCa(null).clienteHttp(provedor).construir());
            assertEquals(200, injetado.emails().obter("id").status());
            injetado.close();
            try (Response resposta = provedor.newCall(new Request.Builder()
                    .url(laboratorio.info.get("base").asText() + "/emails/ok").build()).execute()) {
                assertEquals(200, resposta.code());
            }
            assertFalse(provedor.dispatcher().executorService().isShutdown());
        } finally { provedor.connectionPool().evictAll(); provedor.dispatcher().executorService().shutdown(); }
    }

    @Test public void runtimeDoProvedorInjetadoNaoExpoeMensagemOuCausa() throws Exception {
        OkHttpClient[] provedores = new OkHttpClient[] {
            new OkHttpClient.Builder().addInterceptor(cadeia -> {
                throw new IllegalStateException("detalhe-privado-sintetico");
            }).build(),
            new OkHttpClient() {
                @Override public okhttp3.Call newCall(Request pedido) {
                    throw new IllegalStateException("detalhe-privado-sintetico");
                }
            }
        };
        for (OkHttpClient provedor : provedores) {
            try (Cliente injetado = new Cliente("chave_sintetica", laboratorio.configuracao()
                    .certificadoCa(null).clienteHttp(provedor).construir())) {
                ErroMailcore falha = erro(() -> injetado.emails().obter("id"));
                assertEquals("falha_de_rede", falha.codigo());
                privado(falha);
            } finally { provedor.connectionPool().evictAll(); provedor.dispatcher().executorService().shutdown(); }
        }
        assertEquals(0, laboratorio.registros().size());
    }

    @Test public void caminhoComMultiplosControlesNaoSofreNormalizacaoSilenciosa() throws Exception {
        try (br.com.mailcore.sdk.interno.Transporte transporte = new br.com.mailcore.sdk.interno.Transporte(
                "chave_sintetica", laboratorio.configuracao().construir())) {
            for (String caminho : Arrays.asList("/emails/a\n\nb", "/emails/a\r\nb", "/emails/a\u0000b",
                    "/emails/a\u007fb", "/emails/a\\b", "/emails/a?b", "/emails/a#b")) {
                assertEquals("configuracao_invalida", erro(() -> transporte.executar("GET", caminho,
                        null, null, br.com.mailcore.sdk.interno.Formato.JSON, false, null, null)).codigo());
            }
        }
        assertEquals(0, laboratorio.registros().size());
    }

    @Test public void urlBaseRecusaPortaForaDoIntervaloNoConstrutor() throws Exception {
        for (String porta : Arrays.asList("0", "65536", "99999", "-1", "invalida")) {
            ErroMailcore falha = erro(() -> {
                try (Cliente invalido = new Cliente("chave_sintetica", Configuracao.construtor()
                        .urlBase("https://example.test:" + porta + "/detalhe-privado-sintetico").construir())) {
                    assertNotNull(invalido);
                }
            });
            assertEquals("configuracao_invalida", falha.codigo());
            privado(falha);
        }
        assertEquals(0, laboratorio.registros().size());
    }

    @Test public void filtrosJsonNumericosNaoFinitosFalhamAntesDaRede() throws Exception {
        for (JsonNode numero : Arrays.asList(
                com.fasterxml.jackson.databind.node.DoubleNode.valueOf(Double.NaN),
                com.fasterxml.jackson.databind.node.DoubleNode.valueOf(Double.POSITIVE_INFINITY),
                com.fasterxml.jackson.databind.node.FloatNode.valueOf(Float.NaN),
                com.fasterxml.jackson.databind.node.FloatNode.valueOf(Float.NEGATIVE_INFINITY))) {
            assertEquals("configuracao_invalida", erro(() -> cliente.emails().listar(mapa("limite", numero))).codigo());
        }
        assertEquals(0, laboratorio.registros().size());
    }

    /** Executa uma única sonda em outra JVM, sem produzir logs de requisições. */
    public static final class SondaProxy {
        private SondaProxy() { }
        public static void main(String[] argumentos) {
            org.junit.runner.Result resultado = new org.junit.runner.JUnitCore().run(
                    org.junit.runner.Request.method(ProtocoloTest.class, "proxyDaJvmNaoAfetaClientePadrao"));
            if (!resultado.wasSuccessful()) { System.exit(1); }
        }
    }

    private interface Acao { void executar() throws Exception; }
    private static ErroMailcore erro(Acao acao) throws Exception {
        try { acao.executar(); } catch (ErroMailcore esperado) { return esperado; }
        throw new AssertionError("A operação inválida foi aceita.");
    }
    private static void privado(ErroMailcore erro) {
        StringWriter pilha = new StringWriter();
        erro.printStackTrace(new PrintWriter(pilha));
        for (String representacao : Arrays.asList(erro.getMessage(), erro.toString(), pilha.toString())) {
            assertFalse(representacao.contains("detalhe-privado-sintetico"));
            assertFalse(representacao.contains("mc_chave_sintetica_local"));
        }
        assertNull(erro.getCause());
    }
    private static OpcoesRequisicao idempotencia() {
        return OpcoesRequisicao.construtor().chaveIdempotencia("intencao-sintetica-estavel").construir();
    }
    private static byte[] bytes(String texto) { return texto.getBytes(StandardCharsets.UTF_8); }
    private static String base64(byte[] dados) { return Base64.getEncoder().encodeToString(dados); }
    private static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) { resultado.put((String) pares[i], pares[i + 1]); }
        return resultado;
    }
    private static Map<String, List<String>> cabecalhos(String nome, String valor) {
        return Collections.singletonMap(nome, Collections.singletonList(valor));
    }
    private static String cabecalho(Map<String, List<String>> campos, String nome) {
        for (Map.Entry<String, List<String>> campo : campos.entrySet()) {
            if (campo.getKey().equalsIgnoreCase(nome)) { return campo.getValue().get(0); }
        }
        return null;
    }

    /** Produz JSON maior que 50MiB sem manter uma String gigante do lado do chamador. */
    private static final class PedidoExtenso implements JsonSerializable {
        @Override public void serialize(JsonGenerator gerador, SerializerProvider provedor) throws IOException {
            gerador.writeStartObject();
            gerador.writeFieldName("texto");
            gerador.writeString(new Reader() {
                private int restantes = 50 * 1024 * 1024;
                @Override public int read(char[] destino, int inicio, int tamanho) {
                    if (restantes == 0) { return -1; }
                    int quantidade = Math.min(tamanho, restantes);
                    Arrays.fill(destino, inicio, inicio + quantidade, 'a');
                    restantes -= quantidade;
                    return quantidade;
                }
                @Override public void close() { }
            }, 50 * 1024 * 1024);
            gerador.writeEndObject();
        }
        @Override public void serializeWithType(JsonGenerator g, SerializerProvider p, TypeSerializer t) throws IOException { serialize(g, p); }
    }

    private static final class Laboratorio implements java.io.Closeable {
        private final Process processo;
        private final OutputStreamWriter entrada;
        private final BufferedReader saida;
        private final JsonNode info;
        Laboratorio() throws Exception {
            processo = new ProcessBuilder("python3", Paths.get("src", "test", "recursos", "servidor_https.py").toString())
                    .redirectError(ProcessBuilder.Redirect.INHERIT).start();
            entrada = new OutputStreamWriter(processo.getOutputStream(), StandardCharsets.UTF_8);
            saida = new BufferedReader(new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8));
            info = ler();
        }
        private JsonNode ler() throws IOException {
            String linha = saida.readLine();
            if (linha == null) { throw new IOException("Laboratório local encerrado inesperadamente."); }
            return JSON.readTree(linha);
        }
        synchronized JsonNode comando(Map<String, Object> comando) throws IOException {
            entrada.write(JSON.writeValueAsString(comando)); entrada.write('\n'); entrada.flush(); return ler();
        }
        void configurar(Map<String, Object> configuracao) throws IOException { assertTrue(comando(mapa("acao", "configurar", "configuracao", configuracao)).get("ok").asBoolean()); }
        JsonNode registros() throws IOException { return comando(mapa("acao", "registros")); }
        Configuracao.Construtor configuracao() throws IOException {
            return Configuracao.construtor().urlBase(info.get("base").asText())
                    .certificadoCa(Files.readAllBytes(Paths.get(info.get("ca").asText())));
        }
        OkHttpClient provedor() throws Exception {
            KeyStore confianca = KeyStore.getInstance(KeyStore.getDefaultType()); confianca.load(null);
            try (java.io.InputStream certificado = Files.newInputStream(Paths.get(info.get("ca").asText()))) {
                confianca.setCertificateEntry("ca-sintetica", CertificateFactory.getInstance("X.509").generateCertificate(certificado));
            }
            TrustManagerFactory fabrica = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); fabrica.init(confianca);
            X509TrustManager gestor = (X509TrustManager) fabrica.getTrustManagers()[0];
            SSLContext contexto = SSLContext.getInstance("TLS"); contexto.init(null, new javax.net.ssl.TrustManager[] {gestor}, null);
            return new OkHttpClient.Builder().sslSocketFactory(contexto.getSocketFactory(), gestor)
                    .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).proxy(Proxy.NO_PROXY).build();
        }
        @Override public void close() throws IOException {
            try { comando(mapa("acao", "encerrar")); }
            finally {
                try {
                    if (!processo.waitFor(3, TimeUnit.SECONDS)) { processo.destroyForcibly(); processo.waitFor(3, TimeUnit.SECONDS); }
                } catch (InterruptedException interrompido) {
                    processo.destroyForcibly();
                    Thread.currentThread().interrupt();
                    throw new IOException("Finalização do laboratório interrompida.");
                } finally { entrada.close(); saida.close(); }
            }
        }
    }
}
