package de.ddkfm.repositories

import com.google.common.cache.CacheBuilder
import de.ddkfm.utils.create
import org.apache.lucene.analysis.custom.CustomAnalyzer
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser
import org.apache.lucene.search.*
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock

object LuceneRepository  {
    val urlCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build<Long, String?>()
    val posterCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build<Long, String?>()
    val luceneLocation = System.getenv("LUCENE_INDEX_LOCATION") ?: "./lucene_index"
    val path = Paths.get(luceneLocation)
    private val directory = FSDirectory.open(path)
    val analyzer by lazy {
        val builder = CustomAnalyzer
            .builder()
            .withTokenizer("standard")
            .addTokenFilter("lowercase")
            .addTokenFilter("hyphenationCompoundWord", mapOf(
                "hyphenator" to "lucene_config/de_DR.xml",
                "dictionary" to "lucene_config/dictionary-de.txt",
                "onlyLongestMatch" to "true",
                "minSubwordSize" to "4"
            ))
            .build()
        builder
    }

    private fun search(query : Query, limit : Int = Integer.MAX_VALUE, offset : Int = 0) : List<ScoreDoc> {
        return doWithReader {
            val searchResult = query(query)
            searchResult
                .scoreDocs
                .limitAndOffset(limit, offset)
        }
    }

    private fun <T> Array<T>.limitAndOffset(limit : Int = Integer.MAX_VALUE, offset : Int = 0) : List<T> {
        return this.drop(offset).take(limit)
    }

    private fun DirectoryReader.query(query: Query) : TopDocs {
        val searcher = IndexSearcher(this)
        return searcher.search(query, Integer.MAX_VALUE)
    }

    private fun <T> doWithReader(func : DirectoryReader.() -> T) : T {
        val reader = try {
            DirectoryReader.open(directory)
        } catch (e: IndexNotFoundException) {
            doWithWriter {  }
            DirectoryReader.open(directory)
        }
        return reader.use(func)
    }

    private fun <T> doWithWriter(func : IndexWriter.() -> T) : T {
        val writer = IndexWriter(directory, IndexWriterConfig(analyzer))
        val lock = ReentrantLock()
        lock.lock()
        try {
            return writer.use(func)
        } finally {
            lock.unlock()
        }
    }

    fun query(query : String, limit : Int, offset : Int) : LuceneResponse<List<Document>> {
        val query1 = StandardQueryParser(analyzer).apply {
            allowLeadingWildcard = true
        }.parse(query, "tweet")
        return doWithReader {
            val searchResult = query(query1)
            val pagedResults = searchResult
                .scoreDocs
                .drop(offset)
                .take(limit)
                .map { this.document(it.doc) }
            return@doWithReader LuceneResponse(searchResult.totalHits.value, pagedResults)
        }

    }

    fun searchForId(id : Long) : Document? {
        return doWithReader {
            query(TermQuery(Term("twitterId", "$id")))
                .scoreDocs
                .firstOrNull()
                ?.doc
                ?.let { this.document(it) }
        }
    }

    fun searchForAnyId(id : Long) : Document? {
        return doWithReader {
            query(
                query = BooleanQuery.Builder()
                    .add(BooleanClause(TermQuery(Term("twitterId", "$id")), BooleanClause.Occur.SHOULD))
                    .add(BooleanClause(TermQuery(Term("sameTweetIds", "$id")), BooleanClause.Occur.SHOULD))
                    .build()
            )
                .scoreDocs
                .firstOrNull()
                ?.doc
                ?.let { this.document(it) }
        }
    }

    fun searchForHash(hash : String) : Document? {
        return doWithReader {
            query(TermQuery(Term("hash", "$hash")))
                .scoreDocs
                .firstOrNull()
                ?.doc
                ?.let { this.document(it) }
        }
    }

    fun update(id : Long, func : Document.() -> Unit) : Document? {
        val existing = searchForId(id)
            ?: return null
        existing.apply(func)
        return doWithWriter {
            val documentId = this.updateDocument(Term("twitterId", "$id"), existing)
            searchForId(id)
        }
    }

    fun delete(id : Long) : Document? {
        return update(id) {
            create("deleted", "true")
        }
    }

    fun create(id : Long, func : Document.() -> Unit) : Document? {
        val document = Document().apply(func)
        return doWithWriter {
            val documentId = this.addDocument(document)
            searchForId(id)
        }
    }
}


data class LuceneResponse<T>(
    val hits : Long,
    val content : T
)
