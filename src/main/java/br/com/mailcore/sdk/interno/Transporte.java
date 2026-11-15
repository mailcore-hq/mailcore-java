package br.com.mailcore.sdk.interno;

import br.com.mailcore.sdk.Configuracao;
import br.com.mailcore.sdk.ConsumidorFluxo;
import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.SinalCancelamento;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

/** Implementação compartilhada. Prefira construir recursos por meio de Cliente. */
public final class Transporte implements AutoCloseable {
    private static final Set<String> RESERVADOS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "authorization","proxy-authorization","cookie","host","content-length","content-type","accept","connection","transfer-encoding","idempotency-key","user-agent","accept-encoding","trailer","te","upgrade")));
    private final Configuracao configuracao;
    private final String chave;
    private final OkHttpClient http;
    private final boolean proprio;
    private final Set<Call> ativas = new HashSet<>();
    private boolean fechado;

    public Transporte(String chave, Configuracao configuracao) {
        if (configuracao == null) { throw ErroMailcore.configuracao("Informe uma configuração."); }
        validarConfiguracao(chave, configuracao);
        this.configuracao=configuracao; this.chave=chave;
        proprio=configuracao.clienteHttp() == null;
        if (proprio) {
            OkHttpClient.Builder construtor=new OkHttpClient.Builder()
                    .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
                    .proxy(Proxy.NO_PROXY).cookieJar(CookieJar.NO_COOKIES)
                    .connectionSpecs(Arrays.asList(ConnectionSpec.RESTRICTED_TLS, ConnectionSpec.CLEARTEXT))
                    .connectTimeout(configuracao.tempoConexao().toNanos(),TimeUnit.NANOSECONDS)
                    .readTimeout(0,TimeUnit.MILLISECONDS).writeTimeout(0,TimeUnit.MILLISECONDS);
            if (configuracao.certificadoCa()!=null) { configurarCa(construtor,configuracao.certificadoCa()); }
            http=construtor.build();
        } else { http=configuracao.clienteHttp(); }
    }

    public Resposta executar(String metodo,String caminho,byte[] corpo,Map<String,?> consulta,Formato formato,boolean aceitarVazio,OpcoesRequisicao opcoes,ConsumidorFluxo consumidor) {
        long inicio=System.nanoTime();
        synchronized (this) { if (fechado) { throw erro("cliente_fechado","O cliente já foi encerrado.",null,null); } }
        if (!Arrays.asList("GET","POST","PATCH","PUT","DELETE").contains(metodo) || caminho==null || !caminho.startsWith("/") || caminho.startsWith("//")) { throw ErroMailcore.configuracao("Método ou caminho de recurso inválido."); }
        for (int i=0;i<caminho.length();i++) {
            char caractere=caminho.charAt(i);
            if (caractere<=32 || caractere==127 || caractere=='?' || caractere=='#' || caractere=='\\') { throw ErroMailcore.configuracao("Caminho de recurso inválido."); }
        }
        for (String parte:caminho.split("/")) { if (parte.equals(".") || parte.equals("..")) { throw ErroMailcore.configuracao("Caminho de recurso inválido."); } }
        if (formato==null) { throw ErroMailcore.configuracao("Informe o formato da resposta."); }
        OpcoesRequisicao opcao=opcoes==null ? OpcoesRequisicao.padrao() : opcoes;
        Duration tempo=opcao.tempoLimite()==null ? configuracao.tempoLimite() : opcao.tempoLimite();
        validarTempo(tempo);
        long limite=opcao.limiteRespostaBytes()==null ? configuracao.limiteRespostaBytes() : opcao.limiteRespostaBytes();
        if (limite<1) { throw ErroMailcore.configuracao("O limite de resposta deve ser positivo."); }
        SinalCancelamento sinal=opcao.sinalCancelamento();
        if (sinal!=null && sinal.cancelado()) { throw erro("cancelado","A operação foi cancelada.",null,null); }
        StringBuilder endereco=new StringBuilder(configuracao.urlBase().replaceAll("/+$","")).append(caminho);
        boolean primeiro=true;
        if (consulta!=null) {
            for (Map.Entry<String,?> entrada:consulta.entrySet()) {
                Object valor=entrada.getValue();
                if (valor==null) { continue; }
                String texto;
                if (valor instanceof JsonNode) {
                    JsonNode no=(JsonNode)valor;
                    if (no.isNull()) { continue; }
                    if (!no.isValueNode() || no.isBinary()) { throw ErroMailcore.configuracao("Filtros aceitam valores simples."); }
                    if ((no.isDouble() && !Double.isFinite(no.doubleValue())) || (no.isFloat() && !Float.isFinite(no.floatValue()))) { throw ErroMailcore.configuracao("Filtros numéricos devem ser finitos."); }
                    texto=no.asText();
                } else if (valor instanceof String || valor instanceof Number || valor instanceof Boolean || valor instanceof Character) {
                    if ((valor instanceof Double && !Double.isFinite((Double)valor)) || (valor instanceof Float && !Float.isFinite((Float)valor))) { throw ErroMailcore.configuracao("Filtros numéricos devem ser finitos."); }
                    texto=valor.toString();
                } else { throw ErroMailcore.configuracao("Filtros aceitam valores simples."); }
                endereco.append(primeiro?'?':'&').append(Json.escapar(entrada.getKey())).append('=').append(Json.escapar(texto)); primeiro=false;
            }
        }
        Request.Builder pedido=new Request.Builder().url(endereco.toString());
        for (Map.Entry<String,List<String>> entrada:opcao.cabecalhos().entrySet()) {
            if (RESERVADOS.contains(entrada.getKey().toLowerCase(Locale.ROOT))) { throw ErroMailcore.configuracao("Cabeçalho reservado ao SDK."); }
            validarNomeCabecalho(entrada.getKey());
            for (String valor:entrada.getValue()) { validarValorCabecalho(valor); pedido.addHeader(entrada.getKey(),valor); }
        }
        String resolvida;
        if (configuracao.fornecedorChave()!=null) {
            try { resolvida=configuracao.fornecedorChave().get(); }
            catch (RuntimeException falha) { throw ErroMailcore.configuracao("Não foi possível obter a chave de API."); }
        } else { resolvida=chave==null ? System.getenv("MAILCORE_API_KEY") : chave; }
        if (!chaveValida(resolvida)) { throw ErroMailcore.configuracao("Informe uma chave válida ou configure MAILCORE_API_KEY."); }
        pedido.header("Authorization","Bearer "+resolvida).header("User-Agent","mailcore-java/0.1.0-alpha.1")
                .header("Accept-Encoding","identity").header("Accept",formato==Formato.JSON ? "application/json" : formato==Formato.TEXTO ? "text/html" : "application/octet-stream");
        if (opcao.chaveIdempotencia()!=null) {
            String idempotencia=opcao.chaveIdempotencia();
            if (!chaveValida(idempotencia) || idempotencia.length()>256) { throw ErroMailcore.configuracao("Use até 256 caracteres ASCII visíveis na idempotência."); }
            pedido.header("Idempotency-Key",idempotencia);
        }
        // Um corpo one-shot bloqueia follow-ups HTTP 503/421 do OkHttp. Inclusive
        // mutações sem payload recebem um corpo explícito de comprimento zero.
        RequestBody corpoHttp=null;
        if (corpo!=null || !metodo.equals("GET")) { corpoHttp=new CorpoUnico(corpo==null ? new byte[0] : corpo,corpo!=null); }
        pedido.method(metodo,corpoHttp);
        long restante=tempo.toNanos()-(System.nanoTime()-inicio);
        if (restante<=0) { throw erro("prazo_excedido","A operação excedeu o prazo.",null,null); }
        final Call chamada;
        try {
            chamada=http.newCall(pedido.build());
            chamada.timeout().timeout(restante,TimeUnit.NANOSECONDS);
        } catch (RuntimeException falha) { throw erro("falha_de_rede","O cliente HTTP não iniciou a operação.",null,null); }
        synchronized (this) { if (fechado) { throw erro("cliente_fechado","O cliente já foi encerrado.",null,null); } ativas.add(chamada); }
        if (sinal!=null && !sinal.registrar(chamada)) { synchronized(this) {ativas.remove(chamada);} chamada.cancel(); throw erro("cancelado","A operação foi cancelada.",null,null); }
        Integer status=null;
        Map<String,List<String>> cabecalhos=Collections.emptyMap();
        try (Response resposta=chamada.execute()) {
            status=resposta.code(); cabecalhos=limparCabecalhos(resposta.headers());
            if (status>=300 && status<400) { throw erro("redirecionamento_recusado","A API retornou redirecionamento recusado.",status,cabecalhos); }
            validarMetadados(resposta.headers(),limite,status,cabecalhos);
            ResponseBody conteudo=resposta.body();
            if (conteudo==null) { throw erro("resposta_invalida","A API não forneceu um corpo válido.",status,cabecalhos); }
            boolean fluxo=consumidor!=null && status>=200 && status<300;
            long total=0;
            ByteArrayOutputStream acumulado=new ByteArrayOutputStream();
            try (InputStream entrada=conteudo.byteStream()) {
                byte[] buffer=new byte[32768];
                while (true) {
                    conferirEstado(sinal,chamada,inicio,tempo,status);
                    int tamanho=entrada.read(buffer);
                    if (tamanho<0) { break; }
                    if (tamanho==0) { continue; }
                    if (tamanho>limite-total) { throw erro("resposta_excedida","A resposta ultrapassa o limite de bytes.",status,cabecalhos); }
                    total+=tamanho;
                    if (fluxo) {
                        try { consumidor.receber(Arrays.copyOf(buffer,tamanho)); }
                        catch (ErroMailcore falha) { throw falha; }
                        catch (Exception falha) { throw erro("erro_consumidor","Não foi possível consumir o download.",status,cabecalhos); }
                    } else { acumulado.write(buffer,0,tamanho); }
                }
            }
            conferirEstado(sinal,chamada,inicio,tempo,status);
            long anunciado=conteudo.contentLength();
            if (anunciado>=0 && anunciado!=total) { throw erro("resposta_incompleta","A resposta terminou incompleta.",status,cabecalhos); }
            byte[] dados=acumulado.toByteArray();
            if (status<200 || status>=300) {
                String codigo="erro_http",mensagem="A API retornou erro HTTP.";
                try {
                    JsonNode envelope=Json.ler(dados), falha=envelope.get("erro");
                    if (falha!=null && falha.path("codigo").isTextual() && falha.path("mensagem").isTextual()) { codigo=falha.get("codigo").textValue(); mensagem=falha.get("mensagem").textValue(); }
                } catch (ErroMailcore ignorado) { /* Corpos de proxies não entram nas exceções. */ }
                throw erro(codigo,mensagem,status,cabecalhos);
            }
            if (!fluxo) {
                if (formato==Formato.JSON && !(dados.length==0 && (status==204 || aceitarVazio))) {
                    try { Json.validar(dados); } catch (ErroMailcore invalido) { throw erro("resposta_invalida","A API retornou JSON inválido.",status,cabecalhos); }
                } else if (formato==Formato.TEXTO) {
                    try { Json.validarUtf8(dados); } catch (ErroMailcore invalido) { throw erro("resposta_invalida","A API retornou texto UTF-8 inválido.",status,cabecalhos); }
                }
            }
            conferirEstado(sinal,chamada,inicio,tempo,status);
            return new Resposta(dados,status,cabecalhos,total);
        } catch (IOException falha) {
            synchronized(this) { if (fechado) { throw erro("cancelado","O cliente foi encerrado durante a operação.",status,cabecalhos); } }
            if (sinal!=null && sinal.cancelado()) { throw erro("cancelado","A operação foi cancelada.",status,cabecalhos); }
            if (falha instanceof InterruptedIOException || System.nanoTime()-inicio>=tempo.toNanos()) { throw erro("prazo_excedido","A operação excedeu o prazo.",status,cabecalhos); }
            throw erro(status==null ? "falha_de_rede" : "resposta_incompleta","Não foi possível concluir a comunicação com a API.",status,cabecalhos);
        } catch (ErroMailcore falha) {
            throw falha;
        } catch (RuntimeException falha) {
            throw erro("falha_de_rede","O cliente HTTP não concluiu a operação.",status,cabecalhos);
        } finally {
            if (sinal!=null) { sinal.remover(chamada); }
            synchronized(this) { ativas.remove(chamada); }
        }
    }
    private void conferirEstado(SinalCancelamento sinal,Call chamada,long inicio,Duration tempo,Integer status) {
        if ((sinal!=null && sinal.cancelado()) || chamada.isCanceled()) {
            if (System.nanoTime()-inicio>=tempo.toNanos() && !(sinal!=null && sinal.cancelado())) { throw erro("prazo_excedido","A operação excedeu o prazo.",status,null); }
            throw erro("cancelado","A operação foi cancelada.",status,null);
        }
        if (System.nanoTime()-inicio>=tempo.toNanos()) { chamada.cancel(); throw erro("prazo_excedido","A operação excedeu o prazo.",status,null); }
    }
    private static void validarConfiguracao(String chave,Configuracao c) {
        if (chave!=null && !chaveValida(chave)) { throw ErroMailcore.configuracao("Informe uma chave válida."); }
        if (chave!=null && c.fornecedorChave()!=null) { throw ErroMailcore.configuracao("Escolha chave explícita ou fornecedor, sem ambiguidade."); }
        try {
            Json.validarTexto(c.urlBase());
            URI url=new URI(c.urlBase());
            HttpUrl.get(c.urlBase());
            String host=url.getHost();
            if (host==null || url.getPort()==0 || url.getPort()>65535 || url.getRawUserInfo()!=null || url.getRawQuery()!=null || url.getRawFragment()!=null || c.urlBase().indexOf('\\')>=0) { throw ErroMailcore.configuracao("URL base inválida."); }
            boolean local=host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("[::1]") || host.equals("::1");
            if (!"https".equalsIgnoreCase(url.getScheme()) && !("http".equalsIgnoreCase(url.getScheme()) && local && c.permitirHttpLocal())) { throw ErroMailcore.configuracao("HTTPS é obrigatório; HTTP de teste se limita a loopback."); }
        } catch (URISyntaxException | IllegalArgumentException falha) { throw ErroMailcore.configuracao("URL base inválida."); }
        validarTempo(c.tempoLimite()); validarTempo(c.tempoConexao());
        if (c.limiteRespostaBytes()<1) { throw ErroMailcore.configuracao("O limite de resposta deve ser positivo."); }
        if (c.clienteHttp()!=null && c.certificadoCa()!=null) { throw ErroMailcore.configuracao("Configure a autoridade certificadora no cliente HTTP injetado."); }
    }
    private static void validarTempo(Duration tempo) { if (tempo==null || tempo.isZero() || tempo.isNegative() || tempo.compareTo(Duration.ofHours(24))>0) { throw ErroMailcore.configuracao("Use prazo positivo de até 24 horas."); } }
    private static boolean chaveValida(String v) { if (v==null || v.isEmpty()) {return false;} for (int i=0;i<v.length();i++) {if(v.charAt(i)<33 || v.charAt(i)>126) {return false;}} return true; }
    private static void validarNomeCabecalho(String v) { if (v.isEmpty() || !v.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {throw ErroMailcore.configuracao("Nome de cabeçalho inválido.");} }
    private static void validarValorCabecalho(String v) { for(int i=0;i<v.length();i++) {if(v.charAt(i)<32 || v.charAt(i)>126) {throw ErroMailcore.configuracao("Valor de cabeçalho inválido.");}} }
    private static ErroMailcore erro(String codigo,String mensagem,Integer status,Map<String,List<String>> headers) {return new ErroMailcore(codigo,mensagem,status,headers);}
    private static Map<String,List<String>> limparCabecalhos(Headers origem) {
        Map<String,List<String>> copia=new LinkedHashMap<>();
        for(String nome:origem.names()) {if(!Arrays.asList("set-cookie","authorization","proxy-authorization").contains(nome.toLowerCase(Locale.ROOT))) {copia.put(nome,origem.values(nome));}}
        return copia;
    }
    private static void validarMetadados(Headers headers,long limite,Integer status,Map<String,List<String>> copia) {
        long tamanho=0;
        for(int i=0;i<headers.size();i++) {tamanho+=headers.name(i).length()+headers.value(i).length()+4;}
        if(tamanho>65536) {throw erro("resposta_excedida","Cabeçalhos excederam o limite.",status,copia);}
        for(String valor:headers.values("Content-Encoding")) {for(String token:valor.split(",",-1)) {if(!token.trim().isEmpty() && !token.trim().equalsIgnoreCase("identity")) {throw erro("codificacao_inesperada","A resposta usa codificação não negociada.",status,copia);}}}
        Long anunciado=null;
        for(String valor:headers.values("Content-Length")) {
            long n;
            try {if(!valor.matches("[0-9]+")) {throw new NumberFormatException();} n=Long.parseLong(valor);}
            catch(NumberFormatException falha) {throw erro("resposta_invalida","Tamanho HTTP inválido.",status,copia);}
            if(n<0 || (anunciado!=null && anunciado!=n)) {throw erro("resposta_invalida","Tamanhos HTTP inconsistentes.",status,copia);}
            if(n>limite) {throw erro("resposta_excedida","A resposta ultrapassa o limite.",status,copia);}
            anunciado=n;
        }
    }
    private static void configurarCa(OkHttpClient.Builder builder,byte[] pem) {
        try {
            TrustManagerFactory padrao=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); padrao.init((KeyStore)null);
            KeyStore certificados=KeyStore.getInstance(KeyStore.getDefaultType()); certificados.load(null);
            int i=0;
            for(Certificate certificado:CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(pem))) {certificados.setCertificateEntry("ca-"+(i++),certificado);}
            if(i==0) {throw ErroMailcore.configuracao("A autoridade certificadora não contém certificados.");}
            TrustManagerFactory extra=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); extra.init(certificados);
            final X509TrustManager primeiro=x509(padrao), segundo=x509(extra);
            X509TrustManager composto=new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] cadeia,String tipo) throws CertificateException {try {primeiro.checkClientTrusted(cadeia,tipo);} catch(CertificateException falha) {segundo.checkClientTrusted(cadeia,tipo);}}
                @Override public void checkServerTrusted(X509Certificate[] cadeia,String tipo) throws CertificateException {try {primeiro.checkServerTrusted(cadeia,tipo);} catch(CertificateException falha) {segundo.checkServerTrusted(cadeia,tipo);}}
                @Override public X509Certificate[] getAcceptedIssuers() {List<X509Certificate> cas=new ArrayList<>(Arrays.asList(primeiro.getAcceptedIssuers())); cas.addAll(Arrays.asList(segundo.getAcceptedIssuers())); return cas.toArray(new X509Certificate[0]);}
            };
            SSLContext contexto=SSLContext.getInstance("TLS"); contexto.init(null,new TrustManager[]{composto},null);
            builder.sslSocketFactory(contexto.getSocketFactory(),composto);
        } catch(GeneralSecurityException | IOException falha) {throw ErroMailcore.configuracao("Não foi possível configurar a autoridade certificadora.");}
    }
    private static X509TrustManager x509(TrustManagerFactory fabrica) {for(TrustManager manager:fabrica.getTrustManagers()) {if(manager instanceof X509TrustManager) {return (X509TrustManager)manager;}} throw ErroMailcore.configuracao("O provedor TLS não oferece validação X.509.");}
    private static final class CorpoUnico extends RequestBody {
        private final byte[] dados; private final boolean json;
        CorpoUnico(byte[] dados,boolean json) {this.dados=dados;this.json=json;}
        @Override public MediaType contentType() {return json ? MediaType.get("application/json; charset=utf-8") : null;}
        @Override public long contentLength() {return dados.length;}
        @Override public boolean isOneShot() {return true;}
        @Override public void writeTo(BufferedSink destino) throws IOException {destino.write(dados);}
    }
    @Override public void close() {
        List<Call> cancelar;
        synchronized(this) {if(fechado) {return;} fechado=true; cancelar=new ArrayList<>(ativas); ativas.clear();}
        for(Call call:cancelar) {call.cancel();}
        if(proprio) {http.connectionPool().evictAll();http.dispatcher().executorService().shutdown();}
    }
    @Override public String toString() {return "Transporte{credenciais=protegidas}";}
}
