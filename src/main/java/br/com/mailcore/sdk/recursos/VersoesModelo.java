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

/** Operações do recurso VersoesModelo no contrato público Mailcore. */
public final class VersoesModelo {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public VersoesModelo(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(String modeloId, Map<String, ?> filtros) {
        return listar(modeloId, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listar(String modeloId) {
        return listar(modeloId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(String modeloId, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/templates/" + Json.segmento(modeloId) + "/versions",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String modeloId, String versaoId) {
        return obter(modeloId, versaoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String modeloId, String versaoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/templates/" + Json.segmento(modeloId) + "/versions/" + Json.segmento(versaoId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta restaurar(String modeloId, String versaoId, Object pedido) {
        return restaurar(modeloId, versaoId, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta restaurar(String modeloId, String versaoId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/templates/" + Json.segmento(modeloId) + "/versions/" + Json.segmento(versaoId) + "/restore",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta prever(String modeloId, String versaoId, Object pedido) {
        return prever(modeloId, versaoId, pedido, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta prever(String modeloId, String versaoId) {
        return prever(modeloId, versaoId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta prever(String modeloId, String versaoId, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/templates/" + Json.segmento(modeloId) + "/versions/" + Json.segmento(versaoId) + "/preview",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }
}
