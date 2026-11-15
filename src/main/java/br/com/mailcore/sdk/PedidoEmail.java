package br.com.mailcore.sdk;

import br.com.mailcore.sdk.interno.Json;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pedido de envio direto ou por modelo; o servidor valida as combinações de campos. */
public final class PedidoEmail {
    private final JsonNode dados;
    private PedidoEmail(Map<String,Object> valores) { dados=Json.ler(Json.objeto(valores)); }
    public static Construtor construtor() { return new Construtor(); }
    /** Retorna uma cópia da representação JSON do pedido. */
    @JsonValue public JsonNode dados() { return dados.deepCopy(); }
    @Override public String toString() { return "PedidoEmail{conteudo=protegido}"; }
    public static final class Construtor {
        private final Map<String,Object> valores=new LinkedHashMap<>();
        private Construtor() {}
        public Construtor remetente(String valor) { valores.put("remetente",valor); return this; }
        public Construtor destinatarios(java.util.List<String> valor) { valores.put("destinatarios",valor); return this; }
        public Construtor assunto(String valor) { valores.put("assunto",valor); return this; }
        public Construtor html(String valor) { valores.put("html",valor); return this; }
        public Construtor texto(String valor) { valores.put("texto",valor); return this; }
        public Construtor cc(java.util.List<String> valor) { valores.put("cc",valor); return this; }
        public Construtor cco(java.util.List<String> valor) { valores.put("cco",valor); return this; }
        public Construtor responderPara(String valor) { valores.put("responder_para",valor); return this; }
        public Construtor agendadaEm(String valor) { valores.put("agendada_em",valor); return this; }
        public Construtor tags(java.util.Map<String,String> valor) { valores.put("tags",valor); return this; }
        public Construtor cabecalhos(java.util.Map<String,String> valor) { valores.put("cabecalhos",valor); return this; }
        public Construtor anexos(java.util.List<AnexoEmail> valor) { valores.put("anexos",valor); return this; }
        public Construtor modelo(ModeloEmail valor) { valores.put("modelo",valor); return this; }
        public PedidoEmail construir() { return new PedidoEmail(valores); }
        @Override public String toString() { return "ConstrutorPedidoEmail{conteudo=protegido}"; }
    }
}
