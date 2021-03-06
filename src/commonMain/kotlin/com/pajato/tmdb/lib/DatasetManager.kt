package com.pajato.tmdb.lib

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTime.Companion.now
import com.soywiz.klock.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Compute the export date for today: if the current time is before 8:00am UTC, use the previous export date, otheriwse
 * use today's export data.
 */
fun getLastExportDate(timestamp: DateTime): String =
    if (timestamp.isAfter(8)) timestamp.toTmdbFormat() else (timestamp - 24.hours).toTmdbFormat()

fun Int.toTmdbFormat() = if (this > 9) "$this" else "0$this"
fun DateTime.toTmdbFormat() = "${this.month1.toTmdbFormat()}_${this.dayOfMonth.toTmdbFormat()}_${this.yearInt}"
fun DateTime.isAfter(time: Int): Boolean = this.hours > time

expect fun dailyExportTask(data: MutableMap<String, List<TmdbData>>)

fun getLinesUrl(listName: String): String {
    val result = "http://files.tmdb.org/p/exports/${listName}_${getLastExportDate(now())}.json.gz"
    return result
}

fun parse(listName: String, line: String): TmdbData =
    when (listName) {
        Collection.listName -> Collection.create(line)
        Keyword.listName -> Keyword.create(line)
        Movie.listName -> Movie.create(line)
        Network.listName -> Network.create(line)
        Person.listName -> Person.create(line)
        ProductionCompany.listName -> ProductionCompany.create(line)
        TvSeries.listName -> TvSeries.create(line)
        else -> TmdbError("unsupported type: $listName.")
    }

/** Manage a global collection of TMDB data sets that are updated daily. */
@ExperimentalCoroutinesApi
object DatasetManager {
    private val data = mutableMapOf<String, List<TmdbData>>()

    /** Set up the map associating list names to actual lists of TMDB data (dataset) for that list name. */
    init {
        dailyExportTask(data)
    }

    /** Return a dataset for a given list name. */
    fun getDataset(listName: String): List<TmdbData> = data[listName] ?: listOf(TmdbError(listName.getErrorMessage()))

    private fun String.getErrorMessage(): String = "Empty list or invalid list name: '$this!"
}
