package io.github.aiyzkn.lyra.data

/**
 * アクセントの色。Apollo のテーマエンジン（外部のテーマのパッケージ）は再現しない代わりに、
 * Holo の公開の色（android.R.color.holo_*_light / holo_*_dark）から選べるようにする。
 * [key] は保存に使う名前なので、変えない。
 */
enum class AccentColor(val key: String, val light: Long, val dark: Long) {
    Blue("blue", 0xFF33B5E5, 0xFF0099CC),
    Green("green", 0xFF99CC00, 0xFF669900),
    Orange("orange", 0xFFFFBB33, 0xFFFF8800),
    Purple("purple", 0xFFAA66CC, 0xFF9933CC),
    Red("red", 0xFFFF4444, 0xFFCC0000);

    companion object {
        val DEFAULT = Blue

        /** 保存した名前から戻す。知らない名前・未保存なら既定（水色）。 */
        fun fromKey(key: String?): AccentColor = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** アルバムのタブの表示。Apollo の格子・一覧の切り替え。 */
enum class AlbumLayout(val key: String) {
    Grid("grid"),
    List("list");

    companion object {
        val DEFAULT = Grid

        fun fromKey(key: String?): AlbumLayout = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

data class Settings(
    val accent: AccentColor = AccentColor.DEFAULT,
    val albumLayout: AlbumLayout = AlbumLayout.DEFAULT,
)
