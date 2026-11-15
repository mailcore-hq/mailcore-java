package br.com.mailcore.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Contratos por AST do SDK Node a234dc2, independentes do gerador Java. */
@RunWith(Parameterized.class)
public final class RecursosContratoTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final int indice;
    private final JsonNode caso;

    public RecursosContratoTest(int indice, String rotulo, JsonNode caso) {
        this.indice = indice;
        this.caso = caso;
    }

    @Parameterized.Parameters(name = "{0}: {1}")
    public static List<Object[]> contratos() throws IOException {
        JsonNode casos;
        try (InputStream entrada = RecursosContratoTest.class.getResourceAsStream("/contratos-recursos.json")) {
            if (entrada == null) {
                throw new AssertionError("Os contratos devem acompanhar a suíte.");
            }
            casos = JSON.readTree(entrada);
        }
        assertEquals(151, casos.size());
        List<Object[]> resultado = new ArrayList<Object[]>();
        for (int indice = 0; indice < casos.size(); indice++) {
            JsonNode caso = casos.get(indice);
            resultado.add(new Object[] {indice, rotulo(caso), caso});
        }
        return resultado;
    }

    @Test
    public void executaOperacaoPelaRedeHttpReal() throws Exception {
        final List<Pedido> pedidos = Collections.synchronizedList(new ArrayList<Pedido>());
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        servidor.setExecutor(executor);
        servidor.createContext("/v1/", troca -> atender(troca, pedidos, caso));
        servidor.start();
        Cliente cliente = null;
        try {
            cliente = new Cliente("mc_contrato_sintetico", Configuracao.construtor()
                    .urlBase("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1")
                    .permitirHttpLocal(true).construir());
            Map<String, List<String>> cabecalhos = new LinkedHashMap<String, List<String>>();
            cabecalhos.put("X-Correlacao", Collections.singletonList("sintetica"));
            OpcoesRequisicao opcoes = OpcoesRequisicao.construtor()
                    .chaveIdempotencia("negocio-123").tempoLimite(Duration.ofSeconds(3))
                    .limiteRespostaBytes(4096).cabecalhos(cabecalhos).construir();
            ByteArrayOutputStream partes = new ByteArrayOutputStream();
            ConsumidorFluxo consumir = parte -> partes.write(parte);
            Resposta resposta = executar(indice, cliente, caso.get("argumentos"), opcoes, consumir);
            assertEquals(rotulo(caso), 1, pedidos.size());
            conferir(caso, pedidos.get(0), resposta, partes.toByteArray());
        } finally {
            try {
                if (cliente != null) {
                    cliente.fechar();
                }
            } finally {
                servidor.stop(0);
                executor.shutdownNow();
            }
        }
    }

    private static Resposta executar(int indice, Cliente cliente, JsonNode argumentos,
            OpcoesRequisicao opcoes, ConsumidorFluxo consumir) {
        switch (indice) {
            case 0: return cliente.automacoes().execucoes().listar(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 1: return cliente.automacoes().execucoes().obter(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 2: return cliente.automacoes().criar(objeto(argumentos.get(0)), opcoes);
            case 3: return cliente.automacoes().listar(consulta(argumentos.get(0)), opcoes);
            case 4: return cliente.automacoes().obter(argumentos.get(0).textValue(), opcoes);
            case 5: return cliente.automacoes().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 6: return cliente.automacoes().remover(argumentos.get(0).textValue(), opcoes);
            case 7: return cliente.automacoes().duplicar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 8: return cliente.automacoes().parar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 9: return cliente.caixas().conversas().emails().listar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), consulta(argumentos.get(2)), opcoes);
            case 10: return cliente.caixas().conversas().emails().obter(argumentos.get(0).textValue(), argumentos.get(1).textValue(), argumentos.get(2).textValue(), opcoes);
            case 11: return cliente.caixas().conversas().emails().responder(argumentos.get(0).textValue(), argumentos.get(1).textValue(), argumentos.get(2).textValue(), objeto(argumentos.get(3)), opcoes);
            case 12: return cliente.caixas().conversas().emails().encaminhar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), argumentos.get(2).textValue(), objeto(argumentos.get(3)), opcoes);
            case 13: return cliente.caixas().conversas().etiquetas().adicionar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), ids(argumentos.get(2)), opcoes);
            case 14: return cliente.caixas().conversas().etiquetas().substituir(argumentos.get(0).textValue(), argumentos.get(1).textValue(), ids(argumentos.get(2)), opcoes);
            case 15: return cliente.caixas().conversas().etiquetas().remover(argumentos.get(0).textValue(), argumentos.get(1).textValue(), argumentos.get(2).textValue(), opcoes);
            case 16: return cliente.caixas().conversas().listar(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 17: return cliente.caixas().conversas().buscar(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 18: return cliente.caixas().conversas().obter(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 19: return cliente.caixas().conversas().atualizar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), objeto(argumentos.get(2)), opcoes);
            case 20: return cliente.caixas().conversas().remover(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 21: return cliente.caixas().etiquetas().listar(argumentos.get(0).textValue(), opcoes);
            case 22: return cliente.caixas().etiquetas().obter(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 23: return cliente.caixas().etiquetas().criar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 24: return cliente.caixas().etiquetas().atualizar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), objeto(argumentos.get(2)), opcoes);
            case 25: return cliente.caixas().etiquetas().remover(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 26: return cliente.caixas().rascunhos().listar(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 27: return cliente.caixas().rascunhos().obter(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 28: return cliente.caixas().rascunhos().criar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 29: return cliente.caixas().rascunhos().atualizar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), objeto(argumentos.get(2)), opcoes);
            case 30: return cliente.caixas().rascunhos().remover(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 31: return cliente.caixas().rascunhos().enviar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), objeto(argumentos.get(2)), opcoes);
            case 32: return cliente.caixas().listar(consulta(argumentos.get(0)), opcoes);
            case 33: return cliente.caixas().obter(argumentos.get(0).textValue(), opcoes);
            case 34: return cliente.caixas().criar(objeto(argumentos.get(0)), opcoes);
            case 35: return cliente.caixas().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 36: return cliente.caixas().remover(argumentos.get(0).textValue(), opcoes);
            case 37: return cliente.campanhas().criar(objeto(argumentos.get(0)), opcoes);
            case 38: return cliente.campanhas().listar(consulta(argumentos.get(0)), opcoes);
            case 39: return cliente.campanhas().obter(argumentos.get(0).textValue(), opcoes);
            case 40: return cliente.campanhas().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 41: return cliente.campanhas().remover(argumentos.get(0).textValue(), opcoes);
            case 42: return cliente.campanhas().duplicar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 43: return cliente.campanhas().revisar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 44: return cliente.campanhas().enviar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 45: return cliente.campanhas().cancelar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 46: return cliente.campanhas().prever(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 47: return cliente.campanhas().lerPrevia(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 48: return cliente.campanhas().testar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 49: return cliente.campanhas().obterMetricas(argumentos.get(0).textValue(), opcoes);
            case 50: return cliente.campanhas().listarDestinatarios(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 51: return cliente.campanhas().listarLinksClicados(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 52: return cliente.chavesApi().criar(objeto(argumentos.get(0)), opcoes);
            case 53: return cliente.chavesApi().listar(opcoes);
            case 54: return cliente.chavesApi().renomear(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 55: return cliente.chavesApi().revogar(argumentos.get(0).textValue(), opcoes);
            case 56: return cliente.cli().concessoes().listar(consulta(argumentos.get(0)), opcoes);
            case 57: return cliente.cli().concessoes().revogar(argumentos.get(0).textValue(), opcoes);
            case 58: return cliente.contatos().segmentos().listar(argumentos.get(0).textValue(), opcoes);
            case 59: return cliente.contatos().segmentos().adicionar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 60: return cliente.contatos().segmentos().remover(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 61: return cliente.contatos().topicos().listar(argumentos.get(0).textValue(), opcoes);
            case 62: return cliente.contatos().topicos().atualizar(argumentos.get(0).textValue(), lista(argumentos.get(1)), opcoes);
            case 63: return cliente.contatos().criar(objeto(argumentos.get(0)), opcoes);
            case 64: return cliente.contatos().listar(consulta(argumentos.get(0)), opcoes);
            case 65: return cliente.contatos().obter(argumentos.get(0).textValue(), opcoes);
            case 66: return cliente.contatos().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 67: return cliente.contatos().remover(argumentos.get(0).textValue(), opcoes);
            case 68: return cliente.contatos().adicionarEmLote(objeto(argumentos.get(0)), opcoes);
            case 69: return cliente.contatos().alterarEmLote(objeto(argumentos.get(0)), opcoes);
            case 70: return cliente.contatos().exportacoes().criar(objeto(argumentos.get(0)), opcoes);
            case 71: return cliente.contatos().exportacoes().listar(consulta(argumentos.get(0)), opcoes);
            case 72: return cliente.contatos().exportacoes().obter(argumentos.get(0).textValue(), opcoes);
            case 73: return cliente.contatos().exportacoes().baixar(argumentos.get(0).textValue(), opcoes);
            case 74: return cliente.contatos().exportacoes().baixarFluxo(argumentos.get(0).textValue(), consumir, opcoes);
            case 75: return cliente.contatos().importacoes().prever(objeto(argumentos.get(0)), opcoes);
            case 76: return cliente.contatos().importacoes().criar(objeto(argumentos.get(0)), opcoes);
            case 77: return cliente.contatos().importacoes().obter(argumentos.get(0).textValue(), opcoes);
            case 78: return cliente.segmentos().criar(objeto(argumentos.get(0)), opcoes);
            case 79: return cliente.segmentos().listar(consulta(argumentos.get(0)), opcoes);
            case 80: return cliente.segmentos().obter(argumentos.get(0).textValue(), opcoes);
            case 81: return cliente.segmentos().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 82: return cliente.segmentos().remover(argumentos.get(0).textValue(), opcoes);
            case 83: return cliente.topicos().criar(objeto(argumentos.get(0)), opcoes);
            case 84: return cliente.topicos().listar(consulta(argumentos.get(0)), opcoes);
            case 85: return cliente.topicos().obter(argumentos.get(0).textValue(), opcoes);
            case 86: return cliente.topicos().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 87: return cliente.topicos().remover(argumentos.get(0).textValue(), opcoes);
            case 88: return cliente.propriedadesContatos().criar(objeto(argumentos.get(0)), opcoes);
            case 89: return cliente.propriedadesContatos().listar(consulta(argumentos.get(0)), opcoes);
            case 90: return cliente.propriedadesContatos().obter(argumentos.get(0).textValue(), opcoes);
            case 91: return cliente.propriedadesContatos().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 92: return cliente.propriedadesContatos().remover(argumentos.get(0).textValue(), opcoes);
            case 93: return cliente.dominios().criar(objeto(argumentos.get(0)), opcoes);
            case 94: return cliente.dominios().listar(consulta(argumentos.get(0)), opcoes);
            case 95: return cliente.dominios().obter(argumentos.get(0).textValue(), opcoes);
            case 96: return cliente.dominios().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 97: return cliente.dominios().registrosDns(argumentos.get(0).textValue(), opcoes);
            case 98: return cliente.dominios().verificar(argumentos.get(0).textValue(), opcoes);
            case 99: return cliente.dominios().remover(argumentos.get(0).textValue(), opcoes);
            case 100: return cliente.emails().enviar(objeto(argumentos.get(0)), opcoes);
            case 101: return cliente.emails().listar(consulta(argumentos.get(0)), opcoes);
            case 102: return cliente.emails().obter(argumentos.get(0).textValue(), opcoes);
            case 103: return cliente.emails().reagendar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 104: return cliente.emails().cancelar(argumentos.get(0).textValue(), opcoes);
            case 105: return cliente.emails().obterPorIdempotencia(argumentos.get(0).textValue(), opcoes);
            case 106: return cliente.emails().eventos(argumentos.get(0).textValue(), opcoes);
            case 107: return cliente.emails().baixarAnexo(argumentos.get(0).textValue(), argumentos.get(1).intValue(), opcoes);
            case 108: return cliente.lotes().enviar(objeto(argumentos.get(0)), opcoes);
            case 109: return cliente.lotes().obterPorIdempotencia(argumentos.get(0).textValue(), opcoes);
            case 110: return cliente.eventos().criar(objeto(argumentos.get(0)), opcoes);
            case 111: return cliente.eventos().listar(consulta(argumentos.get(0)), opcoes);
            case 112: return cliente.eventos().obter(argumentos.get(0).textValue(), opcoes);
            case 113: return cliente.eventos().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 114: return cliente.eventos().remover(argumentos.get(0).textValue(), opcoes);
            case 115: return cliente.eventos().enviar(objeto(argumentos.get(0)), opcoes);
            case 116: return cliente.modelos().versoes().listar(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 117: return cliente.modelos().versoes().obter(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 118: return cliente.modelos().versoes().restaurar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), objeto(argumentos.get(2)), opcoes);
            case 119: return cliente.modelos().versoes().prever(argumentos.get(0).textValue(), argumentos.get(1).textValue(), objeto(argumentos.get(2)), opcoes);
            case 120: return cliente.modelos().pastas().listar(opcoes);
            case 121: return cliente.modelos().pastas().criar(objeto(argumentos.get(0)), opcoes);
            case 122: return cliente.modelos().pastas().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 123: return cliente.modelos().pastas().remover(argumentos.get(0).textValue(), opcoes);
            case 124: return cliente.modelos().prever(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 125: return cliente.modelos().obterPrevia(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 126: return cliente.modelos().testar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 127: return cliente.modelos().criar(objeto(argumentos.get(0)), opcoes);
            case 128: return cliente.modelos().listar(consulta(argumentos.get(0)), opcoes);
            case 129: return cliente.modelos().obter(argumentos.get(0).textValue(), opcoes);
            case 130: return cliente.modelos().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 131: return cliente.modelos().remover(argumentos.get(0).textValue(), opcoes);
            case 132: return cliente.modelos().publicar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 133: return cliente.modelos().duplicar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 134: return cliente.recebimento().listar(consulta(argumentos.get(0)), opcoes);
            case 135: return cliente.recebimento().obter(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 136: return cliente.recebimento().listarAnexos(argumentos.get(0).textValue(), opcoes);
            case 137: return cliente.recebimento().obterAnexo(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 138: return cliente.recebimento().baixarOriginal(argumentos.get(0).textValue(), opcoes);
            case 139: return cliente.recebimento().baixarAnexo(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 140: return cliente.recebimento().baixarOriginalFluxo(argumentos.get(0).textValue(), consumir, opcoes);
            case 141: return cliente.recebimento().baixarAnexoFluxo(argumentos.get(0).textValue(), argumentos.get(1).textValue(), consumir, opcoes);
            case 142: return cliente.webhooks().eventos().listar(argumentos.get(0).textValue(), consulta(argumentos.get(1)), opcoes);
            case 143: return cliente.webhooks().eventos().obter(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 144: return cliente.webhooks().eventos().listarTentativas(argumentos.get(0).textValue(), argumentos.get(1).textValue(), consulta(argumentos.get(2)), opcoes);
            case 145: return cliente.webhooks().eventos().reenviar(argumentos.get(0).textValue(), argumentos.get(1).textValue(), opcoes);
            case 146: return cliente.webhooks().criar(objeto(argumentos.get(0)), opcoes);
            case 147: return cliente.webhooks().listar(consulta(argumentos.get(0)), opcoes);
            case 148: return cliente.webhooks().obter(argumentos.get(0).textValue(), opcoes);
            case 149: return cliente.webhooks().atualizar(argumentos.get(0).textValue(), objeto(argumentos.get(1)), opcoes);
            case 150: return cliente.webhooks().remover(argumentos.get(0).textValue(), opcoes);
            default: throw new AssertionError("Contrato desconhecido.");
        }
    }

    private static Object objeto(JsonNode valor) {
        return JSON.convertValue(valor, Object.class);
    }

    private static List<?> lista(JsonNode valor) {
        List<Object> resultado = new ArrayList<Object>();
        for (JsonNode item : valor) {
            resultado.add(objeto(item));
        }
        return resultado;
    }

    private static List<String> ids(JsonNode valor) {
        List<String> resultado = new ArrayList<String>();
        for (JsonNode item : valor) {
            resultado.add(item.textValue());
        }
        return resultado;
    }

    private static Map<String, Object> consulta(JsonNode valor) {
        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        Iterator<Map.Entry<String, JsonNode>> campos = valor.properties().iterator();
        while (campos.hasNext()) {
            Map.Entry<String, JsonNode> campo = campos.next();
            resultado.put(campo.getKey(), objeto(campo.getValue()));
        }
        return resultado;
    }

    private static String rotulo(JsonNode caso) {
        return caso.get("classe").textValue() + "." + caso.get("metodo").textValue();
    }

    private static void conferir(JsonNode caso, Pedido pedido, Resposta resposta, byte[] partes)
            throws IOException {
        String rotulo = rotulo(caso);
        assertEquals(rotulo, caso.get("verbo").textValue(), pedido.metodo);
        assertEquals(rotulo, "/v1" + caso.get("caminho").textValue(), pedido.caminho);
        JsonNode esperado = caso.get("kwargs");
        Map<String, String> consultaEsperada = new TreeMap<String, String>();
        if (esperado.has("consulta")) {
            Iterator<Map.Entry<String, JsonNode>> campos = esperado.get("consulta").properties().iterator();
            while (campos.hasNext()) {
                Map.Entry<String, JsonNode> campo = campos.next();
                if (!campo.getValue().isNull()) {
                    consultaEsperada.put(campo.getKey(), campo.getValue().asText());
                }
            }
        }
        assertEquals(rotulo, consultaEsperada, pedido.consulta);
        if (esperado.has("corpo")) {
            assertEquals(rotulo, esperado.get("corpo"), JSON.readTree(pedido.corpo));
        } else {
            assertEquals(rotulo, 0, pedido.corpo.length);
        }
        assertEquals(rotulo, "Bearer mc_contrato_sintetico", pedido.cabecalhos.get("authorization").get(0));
        assertEquals(rotulo, "negocio-123", pedido.cabecalhos.get("idempotency-key").get(0));
        assertEquals(rotulo, "sintetica", pedido.cabecalhos.get("x-correlacao").get(0));
        String formato = esperado.path("formato").asText("json");
        String tipoAceito = "bytes".equals(formato) || "fluxo".equals(formato)
                ? "application/octet-stream" : "texto".equals(formato) ? "text/html" : "application/json";
        assertEquals(rotulo, tipoAceito, pedido.cabecalhos.get("accept").get(0));
        assertEquals(rotulo, 200, resposta.status());
        if ("fluxo".equals(formato)) {
            assertArrayEquals(rotulo, "abcdef".getBytes(StandardCharsets.UTF_8), partes);
            assertEquals(rotulo, 6, resposta.totalBytes());
            assertEquals(rotulo, 0, resposta.dados().length);
        } else if ("bytes".equals(formato)) {
            assertArrayEquals(rotulo, new byte[] {0, 127, (byte) 255, 10}, resposta.dados());
        } else if ("texto".equals(formato)) {
            assertEquals(rotulo, "<p>ação</p>", new String(resposta.dados(), StandardCharsets.UTF_8));
        }
        if (esperado.path("aceitar_vazio").asBoolean(false)) {
            assertTrue(rotulo, resposta.dados().length == 0);
        }
    }

    private static void atender(HttpExchange troca, List<Pedido> pedidos, JsonNode caso)
            throws IOException {
        try {
            Map<String, List<String>> cabecalhos = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            for (Map.Entry<String, List<String>> campo : troca.getRequestHeaders().entrySet()) {
                cabecalhos.put(campo.getKey(), new ArrayList<String>(campo.getValue()));
            }
            Map<String, String> consulta = new TreeMap<String, String>();
            String consultaBruta = troca.getRequestURI().getRawQuery();
            if (consultaBruta != null) {
                for (String par : consultaBruta.split("&")) {
                    String[] componentes = par.split("=", 2);
                    consulta.put(URLDecoder.decode(componentes[0], "UTF-8"),
                            componentes.length == 2 ? URLDecoder.decode(componentes[1], "UTF-8") : "");
                }
            }
            byte[] corpo;
            try (InputStream entrada = troca.getRequestBody()) {
                corpo = ler(entrada);
            }
            pedidos.add(new Pedido(troca.getRequestMethod(), troca.getRequestURI().getRawPath(),
                    consulta, cabecalhos, corpo));
            JsonNode esperado = caso.get("kwargs");
            String formato = esperado.path("formato").asText("json");
            byte[] dados = "{\"id\":\"sintetico\"}".getBytes(StandardCharsets.UTF_8);
            String mime = "application/json";
            if ("fluxo".equals(formato)) {
                dados = "abcdef".getBytes(StandardCharsets.UTF_8);
                mime = "application/octet-stream";
            } else if ("bytes".equals(formato)) {
                dados = new byte[] {0, 127, (byte) 255, 10};
                mime = "application/octet-stream";
            } else if ("texto".equals(formato)) {
                dados = "<p>ação</p>".getBytes(StandardCharsets.UTF_8);
                mime = "text/html; charset=utf-8";
            }
            troca.getResponseHeaders().set("Content-Type", mime);
            if (esperado.path("aceitar_vazio").asBoolean(false)) {
                troca.sendResponseHeaders(200, -1);
            } else {
                troca.sendResponseHeaders(200, dados.length);
                troca.getResponseBody().write(dados);
            }
        } finally {
            troca.close();
        }
    }

    private static byte[] ler(InputStream entrada) throws IOException {
        ByteArrayOutputStream dados = new ByteArrayOutputStream();
        byte[] parte = new byte[4096];
        int tamanho;
        while ((tamanho = entrada.read(parte)) != -1) {
            dados.write(parte, 0, tamanho);
        }
        return dados.toByteArray();
    }

    private static final class Pedido {
        private final String metodo;
        private final String caminho;
        private final Map<String, String> consulta;
        private final Map<String, List<String>> cabecalhos;
        private final byte[] corpo;

        private Pedido(String metodo, String caminho, Map<String, String> consulta,
                Map<String, List<String>> cabecalhos, byte[] corpo) {
            this.metodo = metodo;
            this.caminho = caminho;
            this.consulta = consulta;
            this.cabecalhos = cabecalhos;
            this.corpo = corpo;
        }
    }
}
