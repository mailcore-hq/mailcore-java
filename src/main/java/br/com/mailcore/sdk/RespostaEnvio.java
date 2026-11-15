package br.com.mailcore.sdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Intenção de envio aceita; estado não equivale à confirmação de entrega. */
@JsonIgnoreProperties(ignoreUnknown=true)
public final class RespostaEnvio {
    private final String id;
    private final String estado;
    @JsonCreator public RespostaEnvio(@JsonProperty("id") String id, @JsonProperty("estado") String estado) {
        this.id=id; this.estado=estado;
    }
    @JsonProperty("id") public String id() { return id; }
    @JsonProperty("estado") public String estado() { return estado; }
    @Override public String toString() { return "RespostaEnvio{conteudo=protegido}"; }
}
