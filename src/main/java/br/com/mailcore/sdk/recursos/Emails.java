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

/** Operações do recurso Emails no contrato público Mailcore. */
public final class Emails {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public Emails(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta enviar(Object dados) {
        return enviar(dados, null);
    }

    /** Aceita a intenção de envio; acompanhe a entrega separadamente. */
    public Resposta enviar(Object dados, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/emails",
                Json.objeto(dados), null, Formato.JSON, false, opcoes, null);
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
                "GET", "/emails",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String id) {
        return obter(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/emails/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta reagendar(String id, Object pedido) {
        return reagendar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta reagendar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/emails/" + Json.segmento(id),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta cancelar(String id) {
        return cancelar(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta cancelar(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/emails/" + Json.segmento(id) + "/cancel",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obterPorIdempotencia(String chave) {
        return obterPorIdempotencia(chave, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obterPorIdempotencia(String chave, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/emails/idempotency/" + Json.segmento(chave),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta eventos(String id) {
        return eventos(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta eventos(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/emails/" + Json.segmento(id) + "/events",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta baixarAnexo(String id, int indice) {
        return baixarAnexo(id, indice, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta baixarAnexo(String id, int indice, OpcoesRequisicao opcoes) {
        if (indice < 0) {
            throw ErroMailcore.configuracao("O índice do anexo deve ser não negativo.");
        }
        return transporte.executar(
                "GET", "/emails/" + Json.segmento(id) + "/attachments/" + Integer.toString(indice),
                null, null, Formato.BYTES, false, opcoes, null);
    }
}
