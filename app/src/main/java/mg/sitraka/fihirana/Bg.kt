package mg.sitraka.fihirana

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Sary fond (hero) : sary isaky ny boky ao amin'ny Baiboly, ary 8 ho an'ny hira. */
object Bg {
    val bibleFond: Int = R.drawable.bg_fond1
    val books: Map<Int, Int> = mapOf(
        10 to R.drawable.bg_genesisy,
        20 to R.drawable.bg_eksodosy,
        30 to R.drawable.bg_levitikosy,
        40 to R.drawable.bg_nomery,
        50 to R.drawable.bg_deotoronomia,
        60 to R.drawable.bg_josoa,
        70 to R.drawable.bg_mpitsara,
        80 to R.drawable.bg_rota,
        90 to R.drawable.bg_1_samoela,
        100 to R.drawable.bg_2_samoela,
        110 to R.drawable.bg_1_mpanjaka,
        120 to R.drawable.bg_2_mpanjaka,
        130 to R.drawable.bg_1_tantara,
        140 to R.drawable.bg_2_tantara,
        150 to R.drawable.bg_ezra,
        160 to R.drawable.bg_nehemia,
        170 to R.drawable.bg_estera,
        180 to R.drawable.bg_joba,
        190 to R.drawable.bg_salamo,
        200 to R.drawable.bg_ohabolana,
        210 to R.drawable.bg_mpitoriteny,
        220 to R.drawable.bg_solomona,
        230 to R.drawable.bg_isaia,
        240 to R.drawable.bg_jeremia,
        250 to R.drawable.bg_fitomaniana,
        260 to R.drawable.bg_ezekiela,
        270 to R.drawable.bg_daniela,
        280 to R.drawable.bg_hosea,
        290 to R.drawable.bg_joela,
        300 to R.drawable.bg_amosa,
        310 to R.drawable.bg_obadia,
        320 to R.drawable.bg_jona,
        330 to R.drawable.bg_mika,
        340 to R.drawable.bg_nahoma,
        350 to R.drawable.bg_habakoka,
        360 to R.drawable.bg_zefania,
        370 to R.drawable.bg_hagay,
        380 to R.drawable.bg_zakaria,
        390 to R.drawable.bg_malakia,
        400 to R.drawable.bg_matio,
        410 to R.drawable.bg_marka,
        420 to R.drawable.bg_lioka,
        430 to R.drawable.bg_jaona,
        440 to R.drawable.bg_asa_apostoly,
        450 to R.drawable.bg_romana,
        460 to R.drawable.bg_1_korintiana,
        470 to R.drawable.bg_2_korintiana,
        480 to R.drawable.bg_galatiana,
        490 to R.drawable.bg_efesiana,
        500 to R.drawable.bg_filipiana,
        510 to R.drawable.bg_kolosiana,
        520 to R.drawable.bg_1_tesaloniana,
        530 to R.drawable.bg_2_tesaloniana,
        540 to R.drawable.bg_1_timoty,
        550 to R.drawable.bg_2_timoty,
        560 to R.drawable.bg_titosy,
        570 to R.drawable.bg_filemona,
        580 to R.drawable.bg_hebreo,
        590 to R.drawable.bg_jakoba,
        600 to R.drawable.bg_1_petera,
        610 to R.drawable.bg_2_petera,
        620 to R.drawable.bg_1_jaona,
        630 to R.drawable.bg_2_jaona,
        640 to R.drawable.bg_3_jaona,
        650 to R.drawable.bg_joda,
        660 to R.drawable.bg_apokalipsy,
    )
    val hymns: IntArray = intArrayOf(R.drawable.hf_fond1, R.drawable.hf_fond2, R.drawable.hf_fond3, R.drawable.hf_fond4, R.drawable.hf_fond5, R.drawable.hf_fond6, R.drawable.hf_fond7, R.drawable.hf_fond8)

    fun book(b: Int): Int = books[b] ?: bibleFond
    fun hymn(lang: String, num: Int): Int {
        val off = when (lang) { "mg" -> 0; "fr" -> 3; else -> 5 }
        return hymns[(num + off) % hymns.size]
    }

    private val cache = object : LruCache<Int, ImageBitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** Décode en réduisant (largeur cible ~1100 px) pour économiser la mémoire. */
    fun load(ctx: Context, res: Int): ImageBitmap? {
        cache.get(res)?.let { return it }
        return try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeResource(ctx.resources, res, o)
            var s = 1
            while (o.outWidth / (s * 2) >= 1100) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            o2.inScaled = false
            val bmp = BitmapFactory.decodeResource(ctx.resources, res, o2)
            val img = bmp?.asImageBitmap()
            if (img != null) cache.put(res, img)
            img
        } catch (e: Throwable) {
            null
        }
    }
}
