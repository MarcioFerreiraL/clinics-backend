package br.com.clinics.config;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import br.com.clinics.controller.FieldNames;
import org.springframework.stereotype.Component;

/**
 * A API serializa em {@code snake_case} (Jackson da aplicação), mas o springdoc gera os schemas com o Jackson 2
 * padrão, em camelCase. Trocar a estratégia do mapper do springdoc também alteraria o próprio documento OpenAPI
 * ({@code securitySchemes} viraria {@code security_schemes}), então renomeamos apenas as propriedades dos schemas.
 */
@Component
public class SnakeCaseModelConverter implements ModelConverter {

    @Override
    @SuppressWarnings("rawtypes")
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }
        Schema schema = chain.next().resolve(type, context, chain);
        if (schema != null) {
            rename(schema);
        }
        // O resolvedor devolve só um $ref e registra o modelo real no contexto; a operação é idempotente.
        context.getDefinedModels().values().forEach(SnakeCaseModelConverter::rename);
        return schema;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void rename(Schema schema) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            Map<String, Schema> renamed = new LinkedHashMap<>();
            properties.forEach((name, value) -> renamed.put(FieldNames.toSnakeCase(name), value));
            schema.setProperties(renamed);
        }
        List<String> required = schema.getRequired();
        if (required != null) {
            schema.setRequired(required.stream().map(FieldNames::toSnakeCase).toList());
        }
    }
}
