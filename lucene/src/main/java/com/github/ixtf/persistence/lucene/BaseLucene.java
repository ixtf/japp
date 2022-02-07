package com.github.ixtf.persistence.lucene;

/** @author jzb 2018-08-24 */
import com.github.ixtf.J;
import com.github.ixtf.persistence.IEntity;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.Collection;

import static com.github.ixtf.persistence.lucene.Jlucene.ID;

public abstract class BaseLucene<T extends IEntity> {
  @Getter protected final Class<T> entityClass;
  protected final IndexWriter indexWriter;
  protected final DirectoryTaxonomyWriter taxoWriter;
  protected final FacetsConfig facetsConfig;

  @SneakyThrows(IOException.class)
  protected BaseLucene(Path rootPath) {
    final var parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
    entityClass = (Class<T>) parameterizedType.getActualTypeArguments()[0];
    final var indexPath = rootPath.resolve(entityClass.getSimpleName());
    indexWriter =
        new IndexWriter(
            FSDirectory.open(indexPath), new IndexWriterConfig(new SmartChineseAnalyzer()));
    final var taxoPath = rootPath.resolve(entityClass.getSimpleName() + "_Taxonomy");
    taxoWriter = new DirectoryTaxonomyWriter(FSDirectory.open(taxoPath));
    facetsConfig = facetsConfig();
  }

  protected BaseLucene(String root) {
    this(Path.of(root));
  }

  protected abstract FacetsConfig facetsConfig();

  protected abstract Document document(T entity);

  @SneakyThrows(IOException.class)
  public IndexReader indexReader() {
    return DirectoryReader.open(indexWriter);
  }

  @SneakyThrows(IOException.class)
  public DirectoryTaxonomyReader taxoReader() {
    return new DirectoryTaxonomyReader(taxoWriter);
  }

  @SneakyThrows(IOException.class)
  private void _index(T entity) {
    final Term term = new Term(ID, entity.getId());
    if (entity.isDeleted()) {
      indexWriter.deleteDocuments(term);
    } else {
      indexWriter.updateDocument(term, facetsConfig.build(taxoWriter, document(entity)));
    }
  }

  @SneakyThrows(IOException.class)
  public void index(T entity) {
    _index(entity);
    indexWriter.commit();
    taxoWriter.commit();
  }

  @SneakyThrows(IOException.class)
  public void index(Collection<T> entities) {
    if (J.isEmpty(entities)) {
      return;
    }
    entities.parallelStream().forEach(this::_index);
    indexWriter.commit();
    taxoWriter.commit();
  }

  @SneakyThrows(IOException.class)
  public void remove(String id) {
    final Term term = new Term(ID, id);
    indexWriter.deleteDocuments(term);
    indexWriter.commit();
    taxoWriter.commit();
  }

  @SneakyThrows(IOException.class)
  public void remove(Collection<String> ids) {
    if (J.isEmpty(ids)) {
      return;
    }
    final var terms = ids.parallelStream().map(id -> new Term(ID, id)).toArray(Term[]::new);
    indexWriter.deleteDocuments(terms);
    indexWriter.commit();
    taxoWriter.commit();
  }

  @SneakyThrows(IOException.class)
  public void close() {
    taxoWriter.close();
    indexWriter.close();
  }

  @SneakyThrows(IOException.class)
  public Collection<String> query(Query query) {
    @Cleanup final var indexReader = indexReader();
    final var searcher = new IndexSearcher(indexReader);
    final var topDocs = searcher.search(query, Integer.MAX_VALUE);
    return Jlucene.ids(searcher, topDocs);
  }

  @SneakyThrows(IOException.class)
  public Pair<Integer, Collection<String>> query(Query query, int first, int pageSize) {
    @Cleanup final var indexReader = indexReader();
    final var searcher = new IndexSearcher(indexReader);
    final var totalHitCountCollector = new TotalHitCountCollector();
    searcher.search(query, totalHitCountCollector);
    final var topDocs = searcher.search(query, first + pageSize);
    return Jlucene.ids(searcher, totalHitCountCollector, topDocs, first);
  }

  @SneakyThrows(IOException.class)
  public Collection<String> query(Query query, Sort sort) {
    @Cleanup final var indexReader = indexReader();
    final var searcher = new IndexSearcher(indexReader);
    final var topDocs = searcher.search(query, Integer.MAX_VALUE, sort);
    return Jlucene.ids(searcher, topDocs);
  }

  @SneakyThrows(IOException.class)
  public Pair<Integer, Collection<String>> query(Query query, Sort sort, int first, int pageSize) {
    @Cleanup final var indexReader = indexReader();
    final var searcher = new IndexSearcher(indexReader);
    final var totalHitCountCollector = new TotalHitCountCollector();
    searcher.search(query, totalHitCountCollector);
    final var topDocs = searcher.search(query, first + pageSize, sort);
    return Jlucene.ids(searcher, totalHitCountCollector, topDocs, first);
  }

  @SneakyThrows(IOException.class)
  public Pair<Long, Collection<String>> queryFacet(BooleanQuery query, String indexFieldName) {
    @Cleanup final var indexReader = indexReader();
    @Cleanup final var taxoReader = taxoReader();
    final var fc = new FacetsCollector();
    final var searcher = new IndexSearcher(indexReader);
    searcher.search(query, fc);
    final var facets = new FastTaxonomyFacetCounts(indexFieldName, taxoReader, facetsConfig(), fc);
    final var facetResult = facets.getTopChildren(Integer.MAX_VALUE, indexFieldName);
    return Jlucene.ids(facetResult);
  }
}
