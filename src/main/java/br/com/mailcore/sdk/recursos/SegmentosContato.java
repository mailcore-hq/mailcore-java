// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;
import java.util.LinkedHashMap;
import java.util.Map;

/** Operações do recurso SegmentosContato no contrato público Mailcore. */
public final class SegmentosContato {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public SegmentosContato(Transporte transporte) {
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
                "GET", "/contacts/" + Json.segmento(contatoId) + "/segments",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta adicionar(String contatoId, String segmentoId) {
        return adicionar(contatoId, segmentoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta adicionar(String contatoId, String segmentoId, OpcoesRequisicao opcoes) {
        Map<String, Object> corpo = new LinkedHashMap<String, Object>();
        corpo.put("segmento_id", segmentoId);
        return transporte.executar(
                "POST", "/contacts/" + Json.segmento(contatoId) + "/segments",
                Json.objeto(corpo), null, Formato.JSON, true, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String contatoId, String segmentoId) {
        return remover(contatoId, segmentoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String contatoId, String segmentoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/contacts/" + Json.segmento(contatoId) + "/segments/" + Json.segmento(segmentoId),
                null, null, Formato.JSON, false, opcoes, null);
    }
}
