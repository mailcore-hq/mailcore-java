package br.com.mailcore.sdk;

import br.com.mailcore.sdk.interno.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Autentica webhooks com HMAC-SHA256 sem alterar ou recodificar o corpo. */
public final class Assinatura {
    private Assinatura() {}
    public static EventoWebhook verificar(byte[] corpo, Map<String,List<String>> cabecalhos, String segredo) {
        return verificar(corpo,cabecalhos,segredo,OpcoesAssinatura.construtor().construir());
    }
    public static EventoWebhook verificar(byte[] corpo, Map<String,List<String>> cabecalhos, String segredo, OpcoesAssinatura opcoes) {
        try {
            if (corpo==null || corpo.length>8*1024*1024 || cabecalhos==null || segredo==null || opcoes==null) { throw invalida(); }
            Duration tolerancia=opcoes.tolerancia();
            if (tolerancia==null || tolerancia.isZero() || tolerancia.isNegative() || tolerancia.compareTo(Duration.ofSeconds(300))>0) { throw invalida(); }
            String id=unico(cabecalhos,"svix-id",256), instante=unico(cabecalhos,"svix-timestamp",16), assinaturas=unico(cabecalhos,"svix-signature",4096);
            if (!instante.matches("[1-9][0-9]{0,15}")) { throw invalida(); }
            long segundos=Long.parseLong(instante);
            Instant agora=opcoes.agora()==null ? Instant.now() : opcoes.agora();
            Duration diferenca=Duration.between(Instant.ofEpochSecond(segundos),agora).abs();
            if (diferenca.compareTo(tolerancia)>0) { throw invalida(); }
            if (!segredo.startsWith("whsec_") || segredo.length()>1030) { throw invalida(); }
            byte[] chave=base64(segredo.substring(6));
            if (chave.length<16 || chave.length>768) { throw invalida(); }
            Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(chave,"HmacSHA256"));
            mac.update((id+"."+instante+".").getBytes(StandardCharsets.US_ASCII));
            byte[] esperado=mac.doFinal(corpo);
            String[] candidatos=assinaturas.split(" ",-1);
            if (candidatos.length>32) { throw invalida(); }
            boolean aceita=false;
            for (String candidato:candidatos) {
                if (!candidato.startsWith("v1,")) { continue; }
                try { byte[] valor=base64(candidato.substring(3)); aceita|=MessageDigest.isEqual(esperado,valor); }
                catch (ErroMailcore ignorada) { /* Candidatos inválidos não impedem a rotação da chave. */ }
            }
            if (!aceita) { throw invalida(); }
            JsonNode evento=Json.ler(corpo);
            if (!evento.isObject() || !evento.path("type").isTextual() || evento.path("type").asText().isEmpty()
                    || !evento.path("created_at").isTextual() || evento.path("created_at").asText().isEmpty() || !evento.has("data")) { throw invalida(); }
            return new EventoWebhook(evento.get("type").asText(),evento.get("created_at").asText(),evento.get("data"));
        } catch (GeneralSecurityException | RuntimeException falha) { throw invalida(); }
    }
    private static String unico(Map<String,List<String>> campos,String nome,int limite) {
        String valor=null;
        for (Map.Entry<String,List<String>> campo:campos.entrySet()) {
            if (campo.getKey()!=null && campo.getKey().equalsIgnoreCase(nome)) {
                if (valor!=null || campo.getValue()==null || campo.getValue().size()!=1) { throw invalida(); }
                valor=campo.getValue().get(0);
                if (valor==null || valor.isEmpty() || valor.length()>limite) { throw invalida(); }
                for (int i=0;i<valor.length();i++) { if (valor.charAt(i)<32 || valor.charAt(i)>126) { throw invalida(); } }
            }
        }
        if (valor==null) { throw invalida(); }
        return valor;
    }
    private static byte[] base64(String texto) {
        byte[] dados;
        try { dados=Base64.getDecoder().decode(texto); } catch (IllegalArgumentException falha) { throw invalida(); }
        if (!Base64.getEncoder().encodeToString(dados).equals(texto)) { throw invalida(); }
        return dados;
    }
    private static ErroMailcore invalida() { return new ErroMailcore("assinatura_invalida","Não foi possível autenticar o webhook.",null,null); }
}
