// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Operações do recurso EtiquetasConversaCaixa no contrato público Mailcore. */
public final class EtiquetasConversaCaixa {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public EtiquetasConversaCaixa(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta adicionar(String caixaId, String conversaId, List<String> ids) {
        return adicionar(caixaId, conversaId, ids, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta adicionar(String caixaId, String conversaId, List<String> ids, OpcoesRequisicao opcoes) {
        Map<String, Object> corpo = new LinkedHashMap<String, Object>();
        corpo.put("ids", ids == null ? Collections.emptyList() : ids);
        return transporte.executar(
                "POST", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId) + "/labels",
                Json.objeto(corpo), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta substituir(String caixaId, String conversaId, List<String> ids) {
        return substituir(caixaId, conversaId, ids, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta substituir(String caixaId, String conversaId, List<String> ids, OpcoesRequisicao opcoes) {
        Map<String, Object> corpo = new LinkedHashMap<String, Object>();
        corpo.put("ids", ids == null ? Collections.emptyList() : ids);
        return transporte.executar(
                "PUT", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId) + "/labels",
                Json.objeto(corpo), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String caixaId, String conversaId, String etiquetaId) {
        return remover(caixaId, conversaId, etiquetaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String caixaId, String conversaId, String etiquetaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/inboxes/" + Json.segmento(caixaId) + "/threads/" + Json.segmento(conversaId) + "/labels/" + Json.segmento(etiquetaId),
                null, null, Formato.JSON, false, opcoes, null);
    }
}
