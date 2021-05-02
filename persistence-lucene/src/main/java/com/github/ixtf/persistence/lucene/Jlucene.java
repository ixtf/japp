package com.github.ixtf.persistence.lucene;

import com.github.ixtf.J;
import com.github.ixtf.persistence.IEntity;
import com.github.ixtf.persistence.IEntityLoggable;
import com.github.ixtf.persistence.IEntityLoggable.IOperator;
import io.github.classgraph.ClassGraph;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * @author jzb 2019-05-29
 */
public class Jlucene {
    public static final String ID = "id";
    public static final String NULL = "_NULL_";

    public static List<String> ids(IndexSearcher searcher, TopDocs topDocs) {
        return ofNullable(topDocs).stream()
                .map(it -> it.scoreDocs)
                .flatMap(Stream::of)
                .map(it -> id(searcher, it))
                .collect(toUnmodifiableList());
    }

    public static Pair<Integer, Collection<String>> ids(IndexSearcher searcher, TotalHitCountCollector totalHitCountCollector, TopDocs topDocs, int first) {
        final var totalHits = totalHitCountCollector.getTotalHits();
        if (totalHits < 1) {
            return Pair.of(totalHits, EMPTY_LIST);
        }
        final var ids = Arrays.stream(topDocs.scoreDocs)
                .skip(first)
                .map(scoreDoc -> id(searcher, scoreDoc))
                .collect(toUnmodifiableList());
        return Pair.of(totalHits, ids);
    }

    public static Pair<Long, Collection<String>> ids(FacetResult facetResult) {
        final var ids = ofNullable(facetResult)
                .map(it -> it.labelValues).stream()
                .flatMap(Arrays::stream)
                .map(it -> it.label)
                .distinct()
                .collect(toUnmodifiableList());
        return Pair.of(Long.valueOf(ids.size()), ids);
    }

    @SneakyThrows(IOException.class)
    public static String id(IndexSearcher searcher, ScoreDoc scoreDoc) {
        return searcher.doc(scoreDoc.doc).get(ID);
    }

    public static <T extends IEntity> Document doc(@NotNull T entity) {
        final var doc = new Document();
        addId(doc, entity.getId());
        if (entity instanceof IEntityLoggable loggable) {
            addLoggable(doc, loggable);
        }
        return doc;
    }

    public static void addId(@NotNull Document doc, @NotBlank String id) {
        add(doc, ID, id, Field.Store.YES);
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, IEntity entity) {
        final var v = ofNullable(entity).map(IEntity::getId).filter(J::nonBlank).orElse(NULL);
        add(doc, fieldName, v);
    }

    public static void addFacet(@NotNull Document doc, @NotBlank String fieldName, IEntity entity) {
        final var v = ofNullable(entity).map(IEntity::getId).filter(J::nonBlank).orElse(NULL);
        addFacet(doc, fieldName, v);
    }

    public static void addLoggable(@NotNull Document doc, IEntityLoggable entity) {
        ofNullable(entity.getCreator()).map(IOperator::getId).filter(J::nonBlank).ifPresent(it -> addFacet(doc, "creator", it));
        add(doc, "createDateTime", entity.getCreateDateTime());
        ofNullable(entity.getModifier()).map(IOperator::getId).filter(J::nonBlank).ifPresent(it -> addFacet(doc, "modifier", it));
        add(doc, "modifyDateTime", entity.getModifyDateTime());
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, boolean b) {
        doc.add(new IntPoint(fieldName, b ? 1 : 0));
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, long l) {
        doc.add(new LongPoint(fieldName, l));
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, double d) {
        doc.add(new DoublePoint(fieldName, d));
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, Enum e) {
        final var v = ofNullable(e).map(Enum::name).orElse(NULL);
        add(doc, fieldName, v);
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, Enum e, Field.Store store) {
        final var v = ofNullable(e).map(Enum::name).orElse(NULL);
        add(doc, fieldName, v, store);
    }

    public static void addFacet(@NotNull Document doc, @NotBlank String fieldName, Enum e) {
        final var v = ofNullable(e).map(Enum::name).orElse(NULL);
        addFacet(doc, fieldName, v);
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, Date date) {
        ofNullable(date).map(Date::getTime).ifPresent(it -> {
            doc.add(new LongPoint(fieldName, it));
            doc.add(new NumericDocValuesField(fieldName, it));
        });
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, LocalDate ld) {
        ofNullable(ld).map(J::date).ifPresent(it -> add(doc, fieldName, it));
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, LocalDateTime ldt) {
        ofNullable(ldt).map(J::date).ifPresent(it -> add(doc, fieldName, it));
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, String s) {
        add(doc, fieldName, s, Field.Store.NO);
    }

    public static void add(@NotNull Document doc, @NotBlank String fieldName, String s, Field.Store store) {
        ofNullable(s).filter(J::nonBlank).ifPresent(it -> doc.add(new StringField(fieldName, s, store)));
    }

    public static void addText(@NotNull Document doc, @NotBlank String fieldName, String s) {
        addText(doc, fieldName, s, Field.Store.NO);
    }

    public static void addText(@NotNull Document doc, @NotBlank String fieldName, String s, Field.Store store) {
        ofNullable(s).filter(J::nonBlank).ifPresent(it -> doc.add(new TextField(fieldName, s, store)));
    }

    public static void addFacet(@NotNull Document doc, @NotBlank String fieldName, String... path) {
        if (ArrayUtils.isNotEmpty(path)) {
            doc.add(new FacetField(fieldName, path));
        }
    }

    @SneakyThrows(ParseException.class)
    public static void add(BooleanQuery.Builder builder, Analyzer analyzer, String q, String... fields) {
        if (J.nonBlank(q)) {
            final var parser = new MultiFieldQueryParser(fields, analyzer);
            builder.add(parser.parse(q), BooleanClause.Occur.MUST);
        }
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, String fieldName, boolean b) {
        builder.add(IntPoint.newExactQuery(fieldName, b ? 1 : 0), BooleanClause.Occur.MUST);
        return builder;
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, String fieldName, Enum e) {
        ofNullable(e).map(Enum::name).ifPresent(it -> add(builder, fieldName, it));
        return builder;
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, String fieldName, String s) {
        ofNullable(s).filter(J::nonBlank)
                .map(it -> new TermQuery(new Term(fieldName, s)))
                .ifPresent(it -> builder.add(it, BooleanClause.Occur.MUST));
        return builder;
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, String fieldName, Collection<String> ss) {
        if (J.nonEmpty(ss)) {
            final var subBuilder = new BooleanQuery.Builder();
            ss.stream().filter(J::nonBlank)
                    .map(it -> new TermQuery(new Term(fieldName, it)))
                    .forEach(it -> subBuilder.add(it, BooleanClause.Occur.SHOULD));
            builder.add(subBuilder.build(), BooleanClause.Occur.MUST);
        }
        return builder;
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, @NotBlank String fieldName, long startL, long endL) {
        builder.add(LongPoint.newRangeQuery(fieldName, startL, endL), BooleanClause.Occur.MUST);
        return builder;
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, @NotBlank String fieldName, Date startDate, Date endDate) {
        if (startDate != null && endDate != null) {
            return add(builder, fieldName, startDate.getTime(), endDate.getTime());
        }
        return builder;
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, @NotBlank String fieldName, LocalDate startLd, LocalDate endLd) {
        if (startLd != null && endLd != null) {
            return add(builder, fieldName, J.date(startLd), J.date(endLd));
        }
        return builder;
    }

    public static BooleanQuery.Builder add(BooleanQuery.Builder builder, @NotBlank String fieldName, LocalDateTime startLdt, LocalDateTime endLdt) {
        if (startLdt != null && endLdt != null) {
            return add(builder, fieldName, J.date(startLdt), J.date(endLdt));
        }
        return builder;
    }

    public static BooleanQuery.Builder addWildcard(BooleanQuery.Builder builder, @NotBlank String fieldName, String q) {
        ofNullable(q).filter(J::nonBlank)
                .map(it -> new WildcardQuery(new Term(fieldName, it)))
                .ifPresent(it -> builder.add(it, BooleanClause.Occur.MUST));
        return builder;
    }

    public static Stream<? extends Class<?>> streamBaseLucene(String... pkgNames) {
        @Cleanup final var scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(pkgNames)
                .scan();
        return scanResult.getSubclasses(BaseLucene.class.getName())
                .filter(classInfo -> !classInfo.isAbstract() && !classInfo.isInterface())
                .loadClasses().parallelStream()
                .distinct();
    }
}
