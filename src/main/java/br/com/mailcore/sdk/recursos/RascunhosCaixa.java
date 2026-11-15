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

/** Operações do recurso RascunhosCaixa no contrato público Mailcore. */
public final class RascunhosCaixa {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public RascunhosCaixa(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(String caixaId, Map<String, ?> filtros) {
        return listar(caixaId, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listar(String caixaId) {
        return listar(caixaId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(String caixaId, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/drafts",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String caixaId, String rascunhoId) {
        return obter(caixaId, rascunhoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String caixaId, String rascunhoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/drafts/" + Json.segmento(rascunhoId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta criar(String caixaId, Object pedido) {
        return criar(caixaId, pedido, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta criar(String caixaId) {
        return criar(caixaId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta criar(String caixaId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/inboxes/" + Json.segmento(caixaId) + "/drafts",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String caixaId, String rascunhoId, Object pedido) {
        return atualizar(caixaId, rascunhoId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String caixaId, String rascunhoId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/inboxes/" + Json.segmento(caixaId) + "/drafts/" + Json.segmento(rascunhoId),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String caixaId, String rascunhoId) {
        return remover(caixaId, rascunhoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String caixaId, String rascunhoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/inboxes/" + Json.segmento(caixaId) + "/drafts/" + Json.segmento(rascunhoId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta enviar(String caixaId, String rascunhoId, Object pedido) {
        return enviar(caixaId, rascunhoId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta enviar(String caixaId, String rascunhoId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/inboxes/" + Json.segmento(caixaId) + "/drafts/" + Json.segmento(rascunhoId) + "/send",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }
}
