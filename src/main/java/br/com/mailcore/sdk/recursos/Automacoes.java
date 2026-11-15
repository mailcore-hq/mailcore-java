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

/** Operações do recurso Automacoes no contrato público Mailcore. */
public final class Automacoes {
    private final Transporte transporte;
    private final ExecucoesAutomacoes execucoes;

    /** Compartilha o transporte configurado pelo cliente. */
    public Automacoes(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
        this.execucoes = new ExecucoesAutomacoes(transporte);
    }

    /** Retorna as operações de execucoes. */
    public ExecucoesAutomacoes execucoes() {
        return execucoes;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta criar(Object pedido) {
        return criar(pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta criar(Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/automations",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
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
                "GET", "/automations",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String id) {
        return obter(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/automations/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String id, Object pedido) {
        return atualizar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/automations/" + Json.segmento(id),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String id) {
        return remover(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/automations/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta duplicar(String id, Object pedido) {
        return duplicar(id, pedido, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta duplicar(String id) {
        return duplicar(id, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta duplicar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/automations/" + Json.segmento(id) + "/duplicate",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta parar(String id, Object pedido) {
        return parar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta parar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/automations/" + Json.segmento(id) + "/stop",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }
}
