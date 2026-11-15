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

/** Operações do recurso ConversasCaixa no contrato público Mailcore. */
public final class ConversasCaixa {
    private final Transporte transporte;
    private final EmailsCaixa emails;
    private final EtiquetasConversaCaixa etiquetas;

    /** Compartilha o transporte configurado pelo cliente. */
    public ConversasCaixa(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
        this.emails = new EmailsCaixa(transporte);
        this.etiquetas = new EtiquetasConversaCaixa(transporte);
    }

    /** Retorna as operações de emails. */
    public EmailsCaixa emails() {
        return emails;
    }

    /** Retorna as operações de etiquetas. */
    public EtiquetasConversaCaixa etiquetas() {
        return etiquetas;
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
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/threads",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta buscar(String caixaId, Map<String, ?> filtros) {
        return buscar(caixaId, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta buscar(String caixaId) {
        return buscar(caixaId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta buscar(String caixaId, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/threads/search",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String caixaId, String conversaId) {
        return obter(caixaId, conversaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String caixaId, String conversaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String caixaId, String conversaId, Object pedido) {
        return atualizar(caixaId, conversaId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String caixaId, String conversaId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String caixaId, String conversaId) {
        return remover(caixaId, conversaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String caixaId, String conversaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId),
                null, null, Formato.JSON, false, opcoes, null);
    }
}
