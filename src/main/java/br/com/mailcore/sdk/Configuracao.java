package br.com.mailcore.sdk;

import java.time.Duration;
import java.util.function.Supplier;
import okhttp3.OkHttpClient;

/** Configuração imutável. A política de um cliente HTTP injetado pertence à aplicação. */
public final class Configuracao {
    private final String urlBase;
    private final Duration tempoLimite;
    private final Duration tempoConexao;
    private final long limiteRespostaBytes;
    private final byte[] certificadoCa;
    private final boolean permitirHttpLocal;
    private final Supplier<String> fornecedorChave;
    private final OkHttpClient clienteHttp;
    private Configuracao(Construtor c) {
        urlBase=c.urlBase; tempoLimite=c.tempoLimite; tempoConexao=c.tempoConexao; limiteRespostaBytes=c.limiteRespostaBytes;
        certificadoCa=c.certificadoCa == null ? null : c.certificadoCa.clone(); permitirHttpLocal=c.permitirHttpLocal;
        fornecedorChave=c.fornecedorChave; clienteHttp=c.clienteHttp;
    }
    public static Construtor construtor() { return new Construtor(); }
    public static Configuracao padrao() { return construtor().construir(); }
    public String urlBase() { return urlBase; }
    public Duration tempoLimite() { return tempoLimite; }
    public Duration tempoConexao() { return tempoConexao; }
    public long limiteRespostaBytes() { return limiteRespostaBytes; }
    public byte[] certificadoCa() { return certificadoCa == null ? null : certificadoCa.clone(); }
    public boolean permitirHttpLocal() { return permitirHttpLocal; }
    public Supplier<String> fornecedorChave() { return fornecedorChave; }
    public OkHttpClient clienteHttp() { return clienteHttp; }
    @Override public String toString() { return "Configuracao{conteudo=protegido}"; }
    public static final class Construtor {
        private String urlBase="https://mailcore.com.br/v1";
        private Duration tempoLimite=Duration.ofSeconds(30), tempoConexao=Duration.ofSeconds(10);
        private long limiteRespostaBytes=16*1024*1024;
        private byte[] certificadoCa;
        private boolean permitirHttpLocal;
        private Supplier<String> fornecedorChave;
        private OkHttpClient clienteHttp;
        private Construtor() {}
        public Construtor urlBase(String v) { urlBase=v; return this; }
        public Construtor tempoLimite(Duration v) { tempoLimite=v; return this; }
        public Construtor tempoConexao(Duration v) { tempoConexao=v; return this; }
        public Construtor limiteRespostaBytes(long v) { limiteRespostaBytes=v; return this; }
        public Construtor certificadoCa(byte[] v) { certificadoCa=v == null ? null : v.clone(); return this; }
        public Construtor permitirHttpLocal(boolean v) { permitirHttpLocal=v; return this; }
        public Construtor fornecedorChave(Supplier<String> v) { fornecedorChave=v; return this; }
        public Construtor clienteHttp(OkHttpClient v) { clienteHttp=v; return this; }
        public Configuracao construir() { return new Configuracao(this); }
        @Override public String toString() { return "ConstrutorConfiguracao{conteudo=protegido}"; }
    }
}
