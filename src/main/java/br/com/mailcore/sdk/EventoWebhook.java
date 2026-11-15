package br.com.mailcore.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/** Evento autenticado a partir dos bytes originais recebidos. */
public final class EventoWebhook {
    private final String tipo;
    private final String criadoEm;
    private final JsonNode dados;
    EventoWebhook(String tipo, String criadoEm, JsonNode dados) {
        this.tipo=tipo; this.criadoEm=criadoEm; this.dados=dados.deepCopy();
    }
    public String tipo() { return tipo; }
    public String criadoEm() { return criadoEm; }
    public JsonNode dados() { return dados.deepCopy(); }
    @Override public String toString() { return "EventoWebhook{conteudo=protegido}"; }
}
