package json;

import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.json.JsonObject;
import lombok.Data;

import static com.github.ixtf.japp.core.Constant.MAPPER;

/**
 * @author jzb 2018-11-07
 */
public class JsonTest {
    public static void main(String[] args) throws Exception {
        final JsonObject jsonObject = new JsonObject().put("id", "test");
        final Object s = MAPPER.readValue(jsonObject.encode(), JsonNode.class);
        System.out.println(s);
        final TestDTO testDTO = MAPPER.convertValue(s, TestDTO.class);
        System.out.println(testDTO);
    }

    @Data
    public static class TestDTO {
        private String id;
    }
}
