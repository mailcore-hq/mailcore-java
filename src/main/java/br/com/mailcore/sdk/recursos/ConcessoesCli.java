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

/** Operações do recurso ConcessoesCli no contrato público Mailcore. */
public final class ConcessoesCli {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public ConcessoesCli(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
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
                "GET", "/cli/oauth/grants",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta revogar(String id) {
        return revogar(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta revogar(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/cli/oauth/grants/" + Json.segmento(id) + "/revoke",
                null, null, Formato.JSON, false, opcoes, null);
    }
}
