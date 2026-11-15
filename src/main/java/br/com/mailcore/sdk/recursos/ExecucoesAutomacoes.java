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

/** Operações do recurso ExecucoesAutomacoes no contrato público Mailcore. */
public final class ExecucoesAutomacoes {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public ExecucoesAutomacoes(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(String automacaoId, Map<String, ?> filtros) {
        return listar(automacaoId, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listar(String automacaoId) {
        return listar(automacaoId, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(String automacaoId, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/automations/" + Json.segmento(automacaoId) + "/runs",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String automacaoId, String execucaoId) {
        return obter(automacaoId, execucaoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String automacaoId, String execucaoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/automations/" + Json.segmento(automacaoId) + "/runs/" + Json.segmento(execucaoId),
                null, null, Formato.JSON, false, opcoes, null);
    }
}
