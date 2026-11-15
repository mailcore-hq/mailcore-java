package br.com.mailcore.sdk;

import br.com.mailcore.sdk.interno.Json;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Anexo cujo conteúdo usa Base64 padrão; idConteudo permite referência CID no HTML. */
public final class AnexoEmail {
    private final JsonNode dados;
    private AnexoEmail(Map<String,Object> valores) { dados=Json.ler(Json.objeto(valores)); }
    public static Construtor construtor() { return new Construtor(); }
    /** Retorna uma cópia da representação JSON do pedido. */
    @JsonValue public JsonNode dados() { return dados.deepCopy(); }
    @Override public String toString() { return "AnexoEmail{conteudo=protegido}"; }
    public static final class Construtor {
        private final Map<String,Object> valores=new LinkedHashMap<>();
        private Construtor() {}
        public Construtor nome(String valor) { valores.put("nome",valor); return this; }
        public Construtor tipoConteudo(String valor) { valores.put("tipo_conteudo",valor); return this; }
        public Construtor conteudoBase64(String valor) { valores.put("conteudo_base64",valor); return this; }
        public Construtor idConteudo(String valor) { valores.put("id_conteudo",valor); return this; }
        public AnexoEmail construir() { return new AnexoEmail(valores); }
        @Override public String toString() { return "ConstrutorAnexoEmail{conteudo=protegido}"; }
    }
}
