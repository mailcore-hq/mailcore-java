// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ConsumidorFluxo;
import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;
import java.util.Collections;
import java.util.Map;

/** Operações do recurso Recebimento no contrato público Mailcore. */
public final class Recebimento {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public Recebimento(Transporte transporte) {
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
                "GET", "/receiving",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String id, Map<String, ?> filtros) {
        return obter(id, filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta obter(String id) {
        return obter(id, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String id, Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/receiving/" + Json.segmento(id),
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listarAnexos(String id) {
        return listarAnexos(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listarAnexos(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/receiving/" + Json.segmento(id) + "/attachments",
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obterAnexo(String id, String anexoId) {
        return obterAnexo(id, anexoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obterAnexo(String id, String anexoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/receiving/" + Json.segmento(id) + "/attachments/" + Json.segmento(anexoId),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta baixarOriginal(String id) {
        return baixarOriginal(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta baixarOriginal(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/receiving/" + Json.segmento(id) + "/raw",
                null, null, Formato.BYTES, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta baixarAnexo(String id, String anexoId) {
        return baixarAnexo(id, anexoId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta baixarAnexo(String id, String anexoId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/receiving/" + Json.segmento(id) + "/attachments/" + Json.segmento(anexoId) + "/download",
                null, null, Formato.BYTES, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta baixarOriginalFluxo(String id, ConsumidorFluxo consumir) {
        return baixarOriginalFluxo(id, consumir, null);
    }

    /** Entrega blocos próprios ao consumidor sem acumular o download na resposta. */
    public Resposta baixarOriginalFluxo(String id, ConsumidorFluxo consumir, OpcoesRequisicao opcoes) {
        if (consumir == null) {
            throw ErroMailcore.configuracao("Informe um consumidor para o download.");
        }
        return transporte.executar(
                "GET", "/receiving/" + Json.segmento(id) + "/raw",
                null, null, Formato.BYTES, false, opcoes, consumir);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta baixarAnexoFluxo(String id, String anexoId, ConsumidorFluxo consumir) {
        return baixarAnexoFluxo(id, anexoId, consumir, null);
    }

    /** Entrega blocos próprios ao consumidor sem acumular o download na resposta. */
    public Resposta baixarAnexoFluxo(String id, String anexoId, ConsumidorFluxo consumir, OpcoesRequisicao opcoes) {
        if (consumir == null) {
            throw ErroMailcore.configuracao("Informe um consumidor para o download.");
        }
        return transporte.executar(
                "GET", "/receiving/" + Json.segmento(id) + "/attachments/" + Json.segmento(anexoId) + "/download",
                null, null, Formato.BYTES, false, opcoes, consumir);
    }
}
