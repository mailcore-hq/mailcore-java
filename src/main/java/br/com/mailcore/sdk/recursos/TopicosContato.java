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

/** Operações do recurso TopicosContato no contrato público Mailcore. */
public final class TopicosContato {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public TopicosContato(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(String contatoId) {
        return listar(contatoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(String contatoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/contacts/" + Json.segmento(contatoId) + "/topics",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String contatoId, List<?> topicos) {
        return atualizar(contatoId, topicos, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String contatoId, List<?> topicos, OpcoesRequisicao opcoes) {
        Map<String, Object> corpo = new LinkedHashMap<String, Object>();
        corpo.put("topicos", topicos == null ? Collections.emptyList() : topicos);
        return transporte.executar(
                "PATCH", "/contacts/" + Json.segmento(contatoId) + "/topics",
                Json.objeto(corpo), null, Formato.JSON, false, opcoes, null);
    }
}
