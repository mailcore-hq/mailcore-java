// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;

/** Operações do recurso EtiquetasCaixa no contrato público Mailcore. */
public final class EtiquetasCaixa {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public EtiquetasCaixa(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(String caixaId) {
        return listar(caixaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(String caixaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/labels",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String caixaId, String etiquetaId) {
        return obter(caixaId, etiquetaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String caixaId, String etiquetaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/labels/" + Json.segmento(etiquetaId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta criar(String caixaId, Object pedido) {
        return criar(caixaId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta criar(String caixaId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/inboxes/" + Json.segmento(caixaId) + "/labels",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String caixaId, String etiquetaId, Object pedido) {
        return atualizar(caixaId, etiquetaId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String caixaId, String etiquetaId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/inboxes/" + Json.segmento(caixaId) + "/labels/" + Json.segmento(etiquetaId),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String caixaId, String etiquetaId) {
        return remover(caixaId, etiquetaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String caixaId, String etiquetaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/inboxes/" + Json.segmento(caixaId) + "/labels/" + Json.segmento(etiquetaId),
                null, null, Formato.JSON, false, opcoes, null);
    }
}
