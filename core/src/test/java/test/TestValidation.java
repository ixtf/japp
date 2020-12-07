package test;

import com.github.ixtf.J;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

import static com.github.ixtf.Constant.MAPPER;

public class TestValidation {
    @Data
    public static class Command implements Serializable {
        @NotBlank
        private String id;
    }

    public static void main(String[] args) {
        final var jsonNode = MAPPER.createObjectNode().set("id", null);
        final var command = J.checkAndGetCommand(Command.class, jsonNode);
    }
}
