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

/** Operações do recurso ExportacoesContatos no contrato público Mailcore. */
public final class ExportacoesContatos {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public ExportacoesContatos(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta criar(Object filtros) {
        return criar(filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta criar() {
        return criar(Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta criar(Object filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/contacts/exports",
                Json.objeto(filtros), null, Formato.JSON, false, opcoes, null);
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
                "GET", "/contacts/exports",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String id) {
        return obter(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/contacts/exports/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta baixar(String id) {
        return baixar(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta baixar(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/contacts/exports/" + Json.segmento(id) + "/download",
                null, null, Formato.BYTES, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta baixarFluxo(String id, ConsumidorFluxo consumir) {
        return baixarFluxo(id, consumir, null);
    }

    /** Entrega blocos próprios ao consumidor sem acumular o download na resposta. */
    public Resposta baixarFluxo(String id, ConsumidorFluxo consumir, OpcoesRequisicao opcoes) {
        if (consumir == null) {
            throw ErroMailcore.configuracao("Informe um consumidor para o download.");
        }
        return transporte.executar(
                "GET", "/contacts/exports/" + Json.segmento(id) + "/download",
                null, null, Formato.BYTES, false, opcoes, consumir);
    }
}
