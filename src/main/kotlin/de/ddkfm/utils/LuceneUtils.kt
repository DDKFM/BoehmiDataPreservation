package de.ddkfm.utils

import com.google.common.cache.Cache
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField

fun Document.appendOrCreate(fieldName : String, value : Any) {
    val existing = this.getField(fieldName)?.stringValue()
    if(existing != null)
        this.removeField(fieldName)
    this.add(TextField(fieldName, "$existing $value", Field.Store.YES))
}

fun Document.create(fieldName : String, value : Any) {
    val existing = this.getField(fieldName)
    if(existing != null)
        this.removeField(fieldName)
    val field = TextField(fieldName, "$value", Field.Store.YES)
    this.add(field)
}
