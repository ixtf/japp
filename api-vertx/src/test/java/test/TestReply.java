package test;

import java.util.concurrent.CompletableFuture;

public class TestReply {
    public static void main(String[] args) {
        final var completedStage = CompletableFuture.completedStage(null);
        completedStage.whenComplete((v, e) -> {
            System.out.println(e);
            System.out.println(v);
        });
    }
}
