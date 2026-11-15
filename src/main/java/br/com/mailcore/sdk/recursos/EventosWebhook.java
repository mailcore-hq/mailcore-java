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

/** Operações do recurso EventosWebhook no contrato público Mailcore. */
public final class EventosWebhook {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public EventosWebhook(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(String webhookId, Map<String, ?> filtros) {
        return listar(webhookId, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listar(String webhookId) {
        return listar(webhookId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(String webhookId, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/webhooks/" + Json.segmento(webhookId) + "/events",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String webhookId, String eventoId) {
        return obter(webhookId, eventoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String webhookId, String eventoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/webhooks/" + Json.segmento(webhookId) + "/events/" + Json.segmento(eventoId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listarTentativas(String webhookId, String eventoId, Map<String, ?> filtros) {
        return listarTentativas(webhookId, eventoId, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listarTentativas(String webhookId, String eventoId) {
        return listarTentativas(webhookId, eventoId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listarTentativas(String webhookId, String eventoId, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/webhooks/" + Json.segmento(webhookId) + "/events/" + Json.segmento(eventoId) + "/attempts",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta reenviar(String webhookId, String eventoId) {
        return reenviar(webhookId, eventoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta reenviar(String webhookId, String eventoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/webhooks/" + Json.segmento(webhookId) + "/events/" + Json.segmento(eventoId) + "/replay",
                null, null, Formato.JSON, false, opcoes, null);
    }
}
