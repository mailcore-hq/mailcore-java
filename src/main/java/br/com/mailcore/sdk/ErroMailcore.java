package br.com.mailcore.sdk;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Erro com representação textual sanitizada; mensagem de negócio é privada. */
public final class ErroMailcore extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String codigo;
    private final String mensagem;
    private final Integer status;
    private final Map<String,List<String>> cabecalhos;

    public ErroMailcore(String codigo, String mensagem, Integer status, Map<String,List<String>> cabecalhos) {
        super(status == null ? "Mailcore: não foi possível concluir a operação." : "Mailcore: operação recusada (HTTP " + status + ").", null, false, true);
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.status = status;
        this.cabecalhos = Cabecalhos.copiar(cabecalhos);
    }
    public static ErroMailcore configuracao(String mensagem) { return new ErroMailcore("configuracao_invalida", mensagem, null, Collections.<String,List<String>>emptyMap()); }
    public String codigo() { return codigo; }
    public String mensagem() { return mensagem; }
    public Integer status() { return status; }
    public Map<String,List<String>> cabecalhos() { return cabecalhos; }
}
