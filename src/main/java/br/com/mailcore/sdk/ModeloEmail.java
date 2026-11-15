package br.com.mailcore.sdk;

import br.com.mailcore.sdk.interno.Json;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Referência a um modelo salvo e suas variáveis de substituição. */
public final class ModeloEmail {
    private final JsonNode dados;
    private ModeloEmail(Map<String,Object> valores) { dados=Json.ler(Json.objeto(valores)); }
    public static Construtor construtor() { return new Construtor(); }
    /** Retorna uma cópia da representação JSON do pedido. */
    @JsonValue public JsonNode dados() { return dados.deepCopy(); }
    @Override public String toString() { return "ModeloEmail{conteudo=protegido}"; }
    public static final class Construtor {
        private final Map<String,Object> valores=new LinkedHashMap<>();
        private Construtor() {}
        public Construtor id(String valor) { valores.put("id",valor); return this; }
        public Construtor variaveis(java.util.Map<String,?> valor) { valores.put("variaveis",valor); return this; }
        public ModeloEmail construir() { return new ModeloEmail(valores); }
        @Override public String toString() { return "ConstrutorModeloEmail{conteudo=protegido}"; }
    }
}
