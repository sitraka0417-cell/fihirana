package mg.sitraka.fihirana

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Données personnelles : favoris (hira + andininy), loko (surlignage) et naoty. */
object UserStore {
    private class Helper(ctx: Context) : SQLiteOpenHelper(ctx, "user.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE fav_hymn(key TEXT PRIMARY KEY, ts INTEGER)")
            db.execSQL("CREATE TABLE fav_verse(key INTEGER PRIMARY KEY, ts INTEGER)")
            db.execSQL("CREATE TABLE mark(key INTEGER PRIMARY KEY, color INTEGER DEFAULT 0, note TEXT)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    private lateinit var helper: Helper

    var favHymns by mutableStateOf(listOf<String>())
        private set
    var favVerses by mutableStateOf(listOf<Long>())
        private set
    var colors by mutableStateOf(mapOf<Long, Int>())
        private set
    var notes by mutableStateOf(mapOf<Long, String>())
        private set

    fun init(ctx: Context) {
        helper = Helper(ctx)
        val db = helper.writableDatabase
        db.rawQuery("SELECT key FROM fav_hymn ORDER BY ts DESC", null).use { c ->
            val l = ArrayList<String>()
            while (c.moveToNext()) l.add(c.getString(0))
            favHymns = l
        }
        db.rawQuery("SELECT key FROM fav_verse ORDER BY ts DESC", null).use { c ->
            val l = ArrayList<Long>()
            while (c.moveToNext()) l.add(c.getLong(0))
            favVerses = l
        }
        val col = HashMap<Long, Int>()
        val nt = HashMap<Long, String>()
        db.rawQuery("SELECT key,color,note FROM mark", null).use { c ->
            while (c.moveToNext()) {
                val k = c.getLong(0)
                val cl = c.getInt(1)
                val n = c.getString(2)
                if (cl > 0) col[k] = cl
                if (!n.isNullOrBlank()) nt[k] = n
            }
        }
        colors = col
        notes = nt
    }

    fun isFavHymn(h: Hymn) = h.id in favHymns

    fun toggleHymn(h: Hymn) {
        val db = helper.writableDatabase
        if (h.id in favHymns) {
            db.delete("fav_hymn", "key=?", arrayOf(h.id))
            favHymns = favHymns - h.id
        } else {
            val cv = ContentValues()
            cv.put("key", h.id)
            cv.put("ts", System.currentTimeMillis())
            db.insertWithOnConflict("fav_hymn", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            favHymns = listOf(h.id) + favHymns
        }
    }

    fun isFavVerse(k: Long) = k in favVerses

    /** Si tous sont déjà favoris -> retire ; sinon ajoute les manquants. */
    fun toggleVerses(keys: List<Long>) {
        val db = helper.writableDatabase
        val allFav = keys.all { it in favVerses }
        if (allFav) {
            keys.forEach { db.delete("fav_verse", "key=?", arrayOf(it.toString())) }
            favVerses = favVerses - keys.toSet()
        } else {
            val add = keys.filter { it !in favVerses }
            add.forEach {
                val cv = ContentValues()
                cv.put("key", it)
                cv.put("ts", System.currentTimeMillis())
                db.insertWithOnConflict("fav_verse", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            favVerses = add.reversed() + favVerses
        }
    }

    private fun writeMark(k: Long, color: Int, note: String?) {
        val db = helper.writableDatabase
        if (color <= 0 && note.isNullOrBlank()) {
            db.delete("mark", "key=?", arrayOf(k.toString()))
        } else {
            val cv = ContentValues()
            cv.put("key", k)
            cv.put("color", color)
            cv.put("note", note ?: "")
            db.insertWithOnConflict("mark", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    /** color = 0 -> enlève le surlignage. */
    fun setColor(keys: Collection<Long>, color: Int) {
        val c = HashMap(colors)
        for (k in keys) {
            writeMark(k, color, notes[k])
            if (color > 0) c[k] = color else c.remove(k)
        }
        colors = c
    }

    fun setNote(k: Long, text: String) {
        writeMark(k, colors[k] ?: 0, text)
        val n = HashMap(notes)
        if (text.isBlank()) n.remove(k) else n[k] = text.trim()
        notes = n
    }
}
