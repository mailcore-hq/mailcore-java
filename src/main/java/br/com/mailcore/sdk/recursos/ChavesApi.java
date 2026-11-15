// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;

/** Operações do recurso ChavesApi no contrato público Mailcore. */
public final class ChavesApi {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public ChavesApi(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta criar(Object pedido) {
        return criar(pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta criar(Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/api-keys",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar() {
        return listar(null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/api-keys",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta renomear(String id, Object pedido) {
        return renomear(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta renomear(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/api-keys/" + Json.segmento(id),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta revogar(String id) {
        return revogar(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta revogar(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/api-keys/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }
}
