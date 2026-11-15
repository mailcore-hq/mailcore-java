package br.com.mailcore.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Cabecalhos {
    private Cabecalhos() {}
    static Map<String,List<String>> copiar(Map<String,List<String>> origem) {
        Map<String,List<String>> copia = new LinkedHashMap<>();
        if (origem != null) {
            for (Map.Entry<String,List<String>> entrada : origem.entrySet()) {
                if (entrada.getKey() == null || entrada.getValue() == null) { throw ErroMailcore.configuracao("Informe nomes e listas de cabeçalhos válidos."); }
                List<String> valores = new ArrayList<>(entrada.getValue());
                if (valores.contains(null)) { throw ErroMailcore.configuracao("Cabeçalhos não podem conter valores nulos."); }
                copia.put(entrada.getKey(), Collections.unmodifiableList(valores));
            }
        }
        return Collections.unmodifiableMap(copia);
    }
}
