package ec.edu.uteq.pfcbackend.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

// Open Library devuelve "description" en dos formas segun la edicion del libro:
// a veces un String plano, a veces un objeto {"type": "/type/text", "value": "..."}.
// Verificado con una llamada real: GET /isbn/9780451524935.json devuelve la forma objeto.
public class DescripcionOpenLibraryDeserializer extends StdDeserializer<String> {

    public DescripcionOpenLibraryDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.has("value")) {
            return node.get("value").asText();
        }
        return null;
    }
}
