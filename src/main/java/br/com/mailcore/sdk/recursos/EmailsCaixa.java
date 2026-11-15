// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;
import java.util.Collections;
import java.util.Map;

/** Operações do recurso EmailsCaixa no contrato público Mailcore. */
public final class EmailsCaixa {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public EmailsCaixa(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(String caixaId, String conversaId, Map<String, ?> filtros) {
        return listar(caixaId, conversaId, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listar(String caixaId, String conversaId) {
        return listar(caixaId, conversaId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(String caixaId, String conversaId, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId) + "/emails",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String caixaId, String conversaId, String emailId) {
        return obter(caixaId, conversaId, emailId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String caixaId, String conversaId, String emailId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId) + "/emails/" + Json.segmento(emailId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta responder(String caixaId, String conversaId, String emailId, Object pedido) {
        return responder(caixaId, conversaId, emailId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta responder(String caixaId, String conversaId, String emailId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId) + "/emails/" + Json.segmento(emailId) + "/reply",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta encaminhar(String caixaId, String conversaId, String emailId, Object pedido) {
        return encaminhar(caixaId, conversaId, emailId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta encaminhar(String caixaId, String conversaId, String emailId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId) + "/emails/" + Json.segmento(emailId) + "/forward",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }
}
