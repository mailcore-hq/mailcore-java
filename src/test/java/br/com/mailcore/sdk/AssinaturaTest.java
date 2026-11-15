package br.com.mailcore.sdk;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;

/** Vetores HMAC independentes e determinísticos, sem credenciais externas. */
public class AssinaturaTest {
    private static final Instant AGORA = Instant.ofEpochSecond(1794614400L);
    private static final byte[] CHAVE = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CORPO = ("{\"type\":\"email.received\",\"created_at\":\"2026-11-14T00:00:00Z\","
            + "\"data\":{\"email_id\":\"mensagem-sintetica\",\"to\":[\"destino@example.test\"],"
            + "\"texto\":\"Ol\u00e1 \ud83d\udce8\",\"ativo\":false,\"nulo\":null,\"objeto\":{},\"lista\":[]}}")
            .getBytes(StandardCharsets.UTF_8);

    @Test public void autenticaBytesOriginaisEPreservaDadosDoEvento() throws Exception {
        Vetor vetor = vetor(CORPO, AGORA.getEpochSecond(), CHAVE);
        EventoWebhook evento = Assinatura.verificar(CORPO, vetor.cabecalhos, vetor.segredo, opcoes());
        assertEquals("email.received", evento.tipo());
        assertEquals("2026-11-14T00:00:00Z", evento.criadoEm());
        assertFalse(evento.dados().get("ativo").asBoolean());
        assertTrue(evento.dados().get("nulo").isNull());
        assertTrue(evento.dados().get("objeto").isObject());
        assertTrue(evento.dados().get("lista").isArray());
        assertEquals("Ol\u00e1 \ud83d\udce8", evento.dados().get("texto").asText());
        assertFalse(evento.toString().contains("destino@example.test"));
        byte[] alterado = Arrays.copyOf(CORPO, CORPO.length + 1);
        alterado[alterado.length - 1] = ' ';
        recusar(alterado, vetor.cabecalhos, vetor.segredo, opcoes());
    }

    @Test public void janelaDeTrezentosSegundosESimetricaELimitada() throws Exception {
        for (long diferenca : new long[] {-301, -300, -299, 0, 299, 300, 301}) {
            Vetor vetor = vetor(CORPO, AGORA.getEpochSecond() + diferenca, CHAVE);
            if (Math.abs(diferenca) <= 300) {
                assertNotNull(Assinatura.verificar(CORPO, vetor.cabecalhos, vetor.segredo, opcoes()));
            } else { recusar(CORPO, vetor.cabecalhos, vetor.segredo, opcoes()); }
        }
        Vetor vetor = vetor(CORPO, AGORA.getEpochSecond() + 31, CHAVE);
        recusar(CORPO, vetor.cabecalhos, vetor.segredo,
                OpcoesAssinatura.construtor().agora(AGORA).tolerancia(Duration.ofSeconds(30)).construir());
        for (Duration tolerancia : Arrays.asList(Duration.ZERO, Duration.ofSeconds(301))) {
            recusar(CORPO, vetor.cabecalhos, vetor.segredo,
                    OpcoesAssinatura.construtor().agora(AGORA).tolerancia(tolerancia).construir());
        }
    }

    @Test public void recusaCabecalhosAusentesDuplicadosEAmbiguos() throws Exception {
        Vetor original = vetor(CORPO, AGORA.getEpochSecond(), CHAVE);
        for (String nome : Arrays.asList("svix-id", "svix-timestamp", "svix-signature")) {
            Map<String, List<String>> ausentes = copiar(original.cabecalhos);
            ausentes.remove(nome);
            recusar(CORPO, ausentes, original.segredo, opcoes());
            Map<String, List<String>> duplicados = copiar(original.cabecalhos);
            duplicados.get(nome).add(duplicados.get(nome).get(0));
            recusar(CORPO, duplicados, original.segredo, opcoes());
            Map<String, List<String>> ambiguos = copiar(original.cabecalhos);
            ambiguos.put(nome.toUpperCase(java.util.Locale.ROOT), Arrays.asList(original.cabecalhos.get(nome).get(0)));
            recusar(CORPO, ambiguos, original.segredo, opcoes());
        }
        Map<String, List<String>> extenso = copiar(original.cabecalhos);
        extenso.put("svix-id", Arrays.asList(repetir("m", 257)));
        recusar(CORPO, extenso, original.segredo, opcoes());
        Map<String, List<String>> naoAscii = copiar(original.cabecalhos);
        naoAscii.put("svix-signature", Arrays.asList("v1,\u00ff"));
        recusar(CORPO, naoAscii, original.segredo, opcoes());
    }

    @Test public void timestampNaoPermiteOverflowSinalOuFormatoAlternativo() throws Exception {
        for (String instante : Arrays.asList("0", "-1", "001794614400", "1794614400.0", "1e10",
                " 1794614400", "9223372036854775807", "-9223372036854775808", "17946144000000000")) {
            Vetor vetor = vetor(CORPO, instante, CHAVE);
            recusar(CORPO, vetor.cabecalhos, vetor.segredo, opcoes());
        }
    }

    @Test public void exigeVersaoBase64CanonicoEPrefixoDeSegredo() throws Exception {
        Vetor original = vetor(CORPO, AGORA.getEpochSecond(), CHAVE);
        String assinatura = original.cabecalhos.get("svix-signature").get(0).substring(3);
        for (String valor : Arrays.asList("v2," + assinatura, assinatura, "v1," + assinatura.replace("=", ""),
                "v1,%%%", "v1," + repetir("a", 4097))) {
            Map<String, List<String>> alterados = copiar(original.cabecalhos);
            alterados.put("svix-signature", Arrays.asList(valor));
            recusar(CORPO, alterados, original.segredo, opcoes());
        }
        for (String segredo : Arrays.asList("", original.segredo.substring(6), "whsec_%%%",
                original.segredo + "\n", original.segredo.replace("=", ""), "whsec_" + repetir("a", 1025))) {
            recusar(CORPO, original.cabecalhos, segredo, opcoes());
        }
        for (int tamanho : new int[] {15, 16}) {
            byte[] chave = new byte[tamanho]; Arrays.fill(chave, (byte) 'a');
            Vetor vetor = vetor(CORPO, AGORA.getEpochSecond(), chave);
            if (tamanho == 16) { assertNotNull(Assinatura.verificar(CORPO, vetor.cabecalhos, vetor.segredo, opcoes())); }
            else { recusar(CORPO, vetor.cabecalhos, vetor.segredo, opcoes()); }
        }
    }

    @Test public void rotacaoConsideraNoMaximoTrintaEDoisCandidatos() throws Exception {
        Vetor original = vetor(CORPO, AGORA.getEpochSecond(), CHAVE);
        String correta = original.cabecalhos.get("svix-signature").get(0);
        Map<String, List<String>> rotacao = copiar(original.cabecalhos);
        rotacao.put("svix-signature", Arrays.asList("v2,invalida v1,invalida " + correta));
        assertNotNull(Assinatura.verificar(CORPO, rotacao, original.segredo, opcoes()));
        rotacao.put("svix-signature", Arrays.asList(repetir("v1,invalida ", 32) + correta));
        recusar(CORPO, rotacao, original.segredo, opcoes());
    }

    @Test public void assinaturaValidaAindaExigeJsonUtf8EEnvelopeValidos() throws Exception {
        List<byte[]> invalidos = new ArrayList<>();
        for (String texto : Arrays.asList("[]", "null", "{}", "{} {}",
                "{\"type\":\"email.received\",\"created_at\":\"data\"}",
                "{\"type\":3,\"created_at\":\"data\",\"data\":{}}",
                "{\"type\":\"\",\"created_at\":\"data\",\"data\":{}}")) {
            invalidos.add(texto.getBytes(StandardCharsets.UTF_8));
        }
        invalidos.add(new byte[] {'{', '"', 'x', '"', ':', '"', (byte) 255, '"', '}'});
        for (byte[] corpo : invalidos) {
            Vetor vetor = vetor(corpo, AGORA.getEpochSecond(), CHAVE);
            recusar(corpo, vetor.cabecalhos, vetor.segredo, opcoes());
        }
        byte[] nulo = "{\"type\":\"evento\",\"created_at\":\"data\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        Vetor vetor = vetor(nulo, AGORA.getEpochSecond(), CHAVE);
        assertTrue(Assinatura.verificar(nulo, vetor.cabecalhos, vetor.segredo, opcoes()).dados().isNull());
    }

    @Test public void limiteDoCorpoEOitoMiBInclusive() throws Exception {
        byte[] prefixo = "{\"type\":\"evento\",\"created_at\":\"data\",\"data\":{\"texto\":\"".getBytes(StandardCharsets.US_ASCII);
        byte[] sufixo = "\"}}".getBytes(StandardCharsets.US_ASCII);
        byte[] corpo = new byte[8 * 1024 * 1024];
        Arrays.fill(corpo, (byte) 'a');
        System.arraycopy(prefixo, 0, corpo, 0, prefixo.length);
        System.arraycopy(sufixo, 0, corpo, corpo.length - sufixo.length, sufixo.length);
        Vetor vetor = vetor(corpo, AGORA.getEpochSecond(), CHAVE);
        assertNotNull(Assinatura.verificar(corpo, vetor.cabecalhos, vetor.segredo, opcoes()));
        corpo = Arrays.copyOf(corpo, corpo.length + 1); corpo[corpo.length - 1] = ' ';
        vetor = vetor(corpo, AGORA.getEpochSecond(), CHAVE);
        recusar(corpo, vetor.cabecalhos, vetor.segredo, opcoes());
    }

    private static OpcoesAssinatura opcoes() { return OpcoesAssinatura.construtor().agora(AGORA).construir(); }
    private static void recusar(byte[] corpo, Map<String, List<String>> cabecalhos, String segredo, OpcoesAssinatura opcoes) {
        try { Assinatura.verificar(corpo, cabecalhos, segredo, opcoes); fail("Assinatura inválida foi aceita."); }
        catch (ErroMailcore esperado) {
            assertEquals("assinatura_invalida", esperado.codigo());
            assertNull(esperado.getCause());
            for (String texto : Arrays.asList(esperado.toString(), esperado.getMessage())) {
                assertTrue(segredo.isEmpty() || !texto.contains(segredo));
                assertFalse(texto.contains("destino@example.test"));
                assertFalse(texto.contains("mensagem-sintetica"));
            }
        }
    }
    private static Vetor vetor(byte[] corpo, long instante, byte[] chave) throws Exception {
        return vetor(corpo, Long.toString(instante), chave);
    }
    private static Vetor vetor(byte[] corpo, String instante, byte[] chave) throws Exception {
        String id = "msg_sintetico_independente";
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(chave, "HmacSHA256"));
        mac.update((id + "." + instante + ".").getBytes(StandardCharsets.US_ASCII));
        mac.update(corpo);
        Map<String, List<String>> campos = new LinkedHashMap<>();
        campos.put("svix-id", new ArrayList<>(Arrays.asList(id)));
        campos.put("svix-timestamp", new ArrayList<>(Arrays.asList(instante)));
        campos.put("svix-signature", new ArrayList<>(Arrays.asList("v1," + Base64.getEncoder().encodeToString(mac.doFinal()))));
        return new Vetor(campos, "whsec_" + Base64.getEncoder().encodeToString(chave));
    }
    private static Map<String, List<String>> copiar(Map<String, List<String>> original) {
        Map<String, List<String>> copia = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> campo : original.entrySet()) { copia.put(campo.getKey(), new ArrayList<>(campo.getValue())); }
        return copia;
    }
    private static String repetir(String valor, int quantidade) {
        StringBuilder texto = new StringBuilder(); for (int i = 0; i < quantidade; i++) { texto.append(valor); } return texto.toString();
    }
    private static final class Vetor {
        private final Map<String, List<String>> cabecalhos;
        private final String segredo;
        Vetor(Map<String, List<String>> cabecalhos, String segredo) { this.cabecalhos = cabecalhos; this.segredo = segredo; }
    }
}
