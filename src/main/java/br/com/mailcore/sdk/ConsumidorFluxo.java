package br.com.mailcore.sdk;

/** Recebe partes próprias do download; pode retê-las após o retorno. */
@FunctionalInterface
public interface ConsumidorFluxo {
    void receber(byte[] parte) throws Exception;
}
