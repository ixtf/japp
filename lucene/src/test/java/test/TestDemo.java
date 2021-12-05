package test;

import org.apache.lucene.demo.facet.SimpleFacetsExample;

import java.io.IOException;

public class TestDemo {
    public static void main(String[] args) throws IOException {
        final var example = new SimpleFacetsExample();
        final var results = example.runFacetOnly();
        System.out.println(results);
    }
}
