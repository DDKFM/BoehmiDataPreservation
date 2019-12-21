package de.ddkfm.repositories

import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths

object LuceneIndex  {
    val luceneLocation = System.getenv("LUCENE_INDEX_LOCATION") ?: "./lucene_index"
    val path = Paths.get(luceneLocation)
    private val directory = FSDirectory.open(path)
    val analyzer = GermanAnalyzer()
    fun searchForKeyword(keyword : String, limit : Int, offset : Int) {
        val reader = DirectoryReader.open(directory)
        reader.use {
            val searcher = IndexSearcher(it)
            val query = TermQuery(Term("keywords", keyword))
            val searchResult = searcher.search(query, limit)
            val pagedResults = searchResult
                .scoreDocs
                .drop(offset)
                .take(limit)

        }
    }

    fun query(query : String, limit : Int, offset : Int) : LuceneResponse<List<Document>> {
        val reader = DirectoryReader.open(directory)
        return reader.use {
            val searcher = IndexSearcher(it)
            val query = StandardQueryParser(analyzer).apply {
                allowLeadingWildcard = true
            }.parse(query, "tweet")
            val searchResult = searcher.search(query, Integer.MAX_VALUE)
            println(searchResult.scoreDocs.size)
            val pagedResults = searchResult
                .scoreDocs
                .drop(offset)
                .take(limit)
                .map { reader.document(it.doc) }
            return@use LuceneResponse(searchResult.totalHits.value, pagedResults)
        }
    }

    fun searchForId(id : Long) : Document? {
        val reader = try {
            DirectoryReader.open(directory)
        } catch (e: IndexNotFoundException) {
            null
        }
        if(reader == null)
            return null
        return reader.use {
            val searcher = IndexSearcher(it)
            val query = TermQuery(Term("twitterId", "$id"))
            val searchResult = searcher.search(query, 1)
            val docId = searchResult
                .scoreDocs
                .firstOrNull()
                ?.doc
                ?: return@use null
            it.document(docId)
        }
    }

    fun addOrUpdate(id : Long, fields : Map<String, String>) : Document? {
        val writer = IndexWriter(directory, IndexWriterConfig(analyzer))
        val existing = searchForId(id)
        return writer.use { lucene ->
            val documentFields = listOf<IndexableField>(
                StringField("twitterId", "$id", Field.Store.YES)
            ).plus(fields.map { TextField(it.key, it.value, Field.Store.YES) })
            val documentId = if(existing == null)
                lucene.addDocument(documentFields)
            else
                lucene.updateDocument(Term("twitterId", "$id"), documentFields)
            return@use searchForId(id)
        }
    }
}


data class LuceneResponse<T>(
    val hits : Long,
    val content : T
)
