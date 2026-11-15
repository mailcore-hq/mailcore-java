package br.com.mailcore.sdk;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Opções independentes da chamada; cabeçalhos são copiados defensivamente. */
public final class OpcoesRequisicao {
    private final Duration tempoLimite;
    private final Long limiteRespostaBytes;
    private final String chaveIdempotencia;
    private final Map<String,List<String>> cabecalhos;
    private final SinalCancelamento sinalCancelamento;
    private OpcoesRequisicao(Construtor c) { tempoLimite=c.tempoLimite; limiteRespostaBytes=c.limiteRespostaBytes; chaveIdempotencia=c.chaveIdempotencia; cabecalhos=Cabecalhos.copiar(c.cabecalhos); sinalCancelamento=c.sinalCancelamento; }
    public static Construtor construtor() { return new Construtor(); }
    public static OpcoesRequisicao padrao() { return construtor().construir(); }
    public Duration tempoLimite() { return tempoLimite; }
    public Long limiteRespostaBytes() { return limiteRespostaBytes; }
    public String chaveIdempotencia() { return chaveIdempotencia; }
    public Map<String,List<String>> cabecalhos() { return cabecalhos; }
    public SinalCancelamento sinalCancelamento() { return sinalCancelamento; }
    @Override public String toString() { return "OpcoesRequisicao{conteudo=protegido}"; }
    public static final class Construtor {
        private Duration tempoLimite;
        private Long limiteRespostaBytes;
        private String chaveIdempotencia;
        private Map<String,List<String>> cabecalhos=Collections.emptyMap();
        private SinalCancelamento sinalCancelamento;
        private Construtor() {}
        public Construtor tempoLimite(Duration v) { tempoLimite=v; return this; }
        public Construtor limiteRespostaBytes(long v) { limiteRespostaBytes=v; return this; }
        public Construtor chaveIdempotencia(String v) { chaveIdempotencia=v; return this; }
        public Construtor cabecalhos(Map<String,List<String>> v) { cabecalhos=v; return this; }
        public Construtor sinalCancelamento(SinalCancelamento v) { sinalCancelamento=v; return this; }
        public OpcoesRequisicao construir() { return new OpcoesRequisicao(this); }
        @Override public String toString() { return "ConstrutorOpcoes{conteudo=protegido}"; }
    }
}
