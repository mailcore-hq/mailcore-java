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

/** Operações do recurso Campanhas no contrato público Mailcore. */
public final class Campanhas {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public Campanhas(Transporte transporte) {
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
                "POST", "/broadcasts",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(Map<String, ?> filtros) {
        return listar(filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listar() {
        return listar(Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/broadcasts",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String id) {
        return obter(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/broadcasts/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String id, Object pedido) {
        return atualizar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/broadcasts/" + Json.segmento(id),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String id) {
        return remover(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/broadcasts/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta duplicar(String id, Object pedido) {
        return duplicar(id, pedido, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta duplicar(String id) {
        return duplicar(id, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta duplicar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/broadcasts/" + Json.segmento(id) + "/duplicate",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta revisar(String id, Object pedido) {
        return revisar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta revisar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/broadcasts/" + Json.segmento(id) + "/review",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta enviar(String id, Object pedido) {
        return enviar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta enviar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/broadcasts/" + Json.segmento(id) + "/send",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta cancelar(String id, Object pedido) {
        return cancelar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta cancelar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/broadcasts/" + Json.segmento(id) + "/cancel",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta prever(String id, Object pedido) {
        return prever(id, pedido, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta prever(String id) {
        return prever(id, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta prever(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/broadcasts/" + Json.segmento(id) + "/preview",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta testar(String id, Object pedido) {
        return testar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta testar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/broadcasts/" + Json.segmento(id) + "/test",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta lerPrevia(String organizacaoId, String previaId) {
        return lerPrevia(organizacaoId, previaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta lerPrevia(String organizacaoId, String previaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/broadcast-previews/" + Json.segmento(organizacaoId) + "/" + Json.segmento(previaId),
                null, null, Formato.TEXTO, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obterMetricas(String id) {
        return obterMetricas(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obterMetricas(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/broadcasts/" + Json.segmento(id) + "/metrics",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listarDestinatarios(String id, Map<String, ?> filtros) {
        return listarDestinatarios(id, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listarDestinatarios(String id) {
        return listarDestinatarios(id, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listarDestinatarios(String id, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/broadcasts/" + Json.segmento(id) + "/recipients",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listarLinksClicados(String id, Map<String, ?> filtros) {
        return listarLinksClicados(id, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listarLinksClicados(String id) {
        return listarLinksClicados(id, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listarLinksClicados(String id, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/broadcasts/" + Json.segmento(id) + "/clicked-links",
                null, filtros, Formato.JSON, false, opcoes, null);
    }
}
