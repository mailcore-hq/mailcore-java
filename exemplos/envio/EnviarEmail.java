import br.com.mailcore.sdk.Cliente;
import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.PedidoEmail;
import br.com.mailcore.sdk.RespostaEnvio;
import java.util.Collections;

/** Exemplo de servidor: configure a chave em MAILCORE_API_KEY, fora do código. */
public final class EnviarEmail {
    private EnviarEmail() {}
    public static void main(String[] argumentos) {
        if (argumentos.length != 3) {
            System.err.println("Informe remetente verificado, destinatário e chave de idempotência estável para esta intenção.");
            System.exit(2);
        }
        PedidoEmail pedido=PedidoEmail.construtor()
                .remetente(argumentos[0])
                .destinatarios(Collections.singletonList(argumentos[1]))
                .assunto("Olá pelo Mailcore")
                .html("<strong>Sua integração está funcionando.</strong>")
                .construir();
        // Em uma aplicação, compartilhe esta instância e feche-a ao encerrar o processo.
        try (Cliente cliente=new Cliente()) {
            RespostaEnvio envio=cliente.emails().enviar(pedido, OpcoesRequisicao.construtor()
                    .chaveIdempotencia(argumentos[2]).construir()).decodificar(RespostaEnvio.class);
            System.out.println("Intenção aceita: " + envio.id() + "; estado: " + envio.estado());
        } catch (ErroMailcore falha) {
            System.err.println(falha.getMessage());
            System.exit(1);
        }
    }
}
