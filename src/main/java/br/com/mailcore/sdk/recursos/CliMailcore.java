// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.interno.Transporte;

/** Operações do recurso CliMailcore no contrato público Mailcore. */
public final class CliMailcore {
    private final ConcessoesCli concessoes;

    /** Compartilha o transporte configurado pelo cliente. */
    public CliMailcore(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.concessoes = new ConcessoesCli(transporte);
    }

    /** Retorna as operações de concessoes. */
    public ConcessoesCli concessoes() {
        return concessoes;
    }
}
