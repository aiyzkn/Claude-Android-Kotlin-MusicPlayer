package io.github.aiyzkn.lyra.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.aiyzkn.lyra.MainActivity
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.ui.theme.HoloBlueLight
import io.github.aiyzkn.lyra.ui.theme.HoloTextPrimary
import io.github.aiyzkn.lyra.ui.theme.HoloTextSecondary
import kotlinx.coroutines.flow.first

// Holo の暗いテーマのウィジェット（半透明の黒地・水色の線）に寄せる
private val WidgetBackground = Color(0xE6101010)
private val Primary = ColorProvider(HoloTextPrimary)
private val Secondary = ColorProvider(HoloTextSecondary)
private val Accent = ColorProvider(HoloBlueLight)

private val SMALL = DpSize(250.dp, 40.dp)
private val LARGE = DpSize(250.dp, 110.dp)

/** いま再生している曲と、前・再生/一時停止・次。4×1 と 4×2 で、同じウィジェットの形を替える。 */
class NowPlayingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val updates = NowPlayingStore.flow(context)
        val initial = NowPlayingStore.read(context)
        provideContent {
            val nowPlaying by updates.collectAsState(initial)
            NowPlayingContent(nowPlaying)
        }
    }
}

@Composable
private fun NowPlayingContent(nowPlaying: NowPlaying) {
    val context = LocalContext.current
    val title = nowPlaying.title ?: context.getString(R.string.widget_nothing_playing)
    val artist = nowPlaying.artist ?: if (nowPlaying.hasSong) "" else context.getString(R.string.widget_open_app)
    val openApp = actionStartActivity<MainActivity>()
    val large = LocalSize.current.height >= LARGE.height

    if (large) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(WidgetBackground).cornerRadius(16.dp).padding(12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight().clickable(openApp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = GlanceModifier.size(48.dp),
                )
                Spacer(GlanceModifier.width(12.dp))
                SongLines(title, artist, titleSize = 16)
            }
            Spacer(GlanceModifier.height(4.dp))
            Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(HoloBlueLight))
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Controls(nowPlaying, buttonSize = 48)
            }
        }
    } else {
        Row(
            modifier = GlanceModifier.fillMaxSize().background(WidgetBackground).cornerRadius(16.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.defaultWeight().clickable(openApp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = GlanceModifier.size(36.dp),
                )
                Spacer(GlanceModifier.width(8.dp))
                SongLines(title, artist, titleSize = 14)
            }
            Controls(nowPlaying, buttonSize = 40)
        }
    }
}

@Composable
private fun SongLines(title: String, artist: String, titleSize: Int) {
    Column {
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(color = Primary, fontSize = titleSize.sp, fontWeight = FontWeight.Medium),
        )
        if (artist.isNotEmpty()) {
            Text(text = artist, maxLines = 1, style = TextStyle(color = Secondary, fontSize = (titleSize - 2).sp))
        }
    }
}

@Composable
private fun Controls(nowPlaying: NowPlaying, buttonSize: Int) {
    val context = LocalContext.current
    ControlButton(R.drawable.ic_skip_previous, context.getString(R.string.action_previous), buttonSize, actionRunCallback<PreviousAction>())
    if (nowPlaying.isPlaying) {
        ControlButton(R.drawable.ic_pause, context.getString(R.string.action_pause), buttonSize, actionRunCallback<PlayPauseAction>())
    } else {
        ControlButton(R.drawable.ic_play, context.getString(R.string.action_play), buttonSize, actionRunCallback<PlayPauseAction>())
    }
    ControlButton(R.drawable.ic_skip_next, context.getString(R.string.action_next), buttonSize, actionRunCallback<NextAction>())
}

@Composable
private fun ControlButton(icon: Int, description: String, size: Int, action: Action) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = description,
        modifier = GlanceModifier.size(size.dp).padding((size / 5).dp).cornerRadius((size / 2).dp).clickable(action),
    )
}

/** 最近再生した曲の一覧。押すと、その曲から鳴らす。 */
class RecentWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = context.widgetEntryPoint()
        val history = entry.userLibrary().recentlyPlayed
        val initialIds = history.first()
        val byId = runCatching { entry.musicLibrary().songs() }.getOrDefault(emptyList()).associateBy { it.id }
        provideContent {
            // 描画のセッションの間に履歴が増えても、描き直しに反映させる
            val ids by history.collectAsState(initialIds)
            val songs = remember(ids) { recentSongsForWidget(ids, byId) }
            RecentContent(songs)
        }
    }
}

@Composable
private fun RecentContent(songs: List<Song>) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize().background(WidgetBackground).cornerRadius(16.dp).padding(8.dp)) {
        Text(
            text = context.getString(R.string.tab_recent),
            style = TextStyle(color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                .clickable(actionStartActivity<MainActivity>()),
        )
        Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(HoloBlueLight))
        if (songs.isEmpty()) {
            Text(
                text = context.getString(R.string.recent_empty),
                style = TextStyle(color = Secondary, fontSize = 13.sp),
                modifier = GlanceModifier.padding(8.dp),
            )
        } else {
            LazyColumn {
                items(songs, itemId = { it.id }) { song ->
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp).clickable(
                            actionRunCallback<PlayRecentAction>(actionParametersOf(PlayRecentAction.SONG_ID to song.id)),
                        ),
                    ) {
                        Text(text = song.title, maxLines = 1, style = TextStyle(color = Primary, fontSize = 14.sp))
                        Text(text = song.artist, maxLines = 1, style = TextStyle(color = Secondary, fontSize = 12.sp))
                    }
                }
            }
        }
    }
}

/** 4×1 の再生のウィジェット。 */
class NowPlayingSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}

/** 4×2 の再生のウィジェット（中身は 4×1 と同じ。大きさで形が替わる）。 */
class NowPlayingLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}

class RecentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentWidget()
}
