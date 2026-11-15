// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;

/** Operações do recurso Lotes no contrato público Mailcore. */
public final class Lotes {
    private final Transporte transporte;

    /** Compartilha o transporte configurado pelo cliente. */
    public Lotes(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta enviar(Object dados) {
        return enviar(dados, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta enviar(Object dados, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/emails/batch",
                Json.lista(dados), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obterPorIdempotencia(String chave) {
        return obterPorIdempotencia(chave, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obterPorIdempotencia(String chave, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/emails/batch/idempotency/" + Json.segmento(chave),
                null, null, Formato.JSON, false, opcoes, null);
    }
}
