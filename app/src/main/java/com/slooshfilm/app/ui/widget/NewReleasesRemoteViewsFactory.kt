package com.slooshfilm.app.ui.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.repository.ContentCategory
import com.slooshfilm.app.data.repository.ContentType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class NewReleasesRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var movies = listOf<Movie>()
    private val repository by lazy { (context.applicationContext as SlooshApplication).hdRezkaRepository }

    override fun onCreate() {
    }

    override fun onDataSetChanged() {
        try {
            runBlocking {
                coroutineScope {
                    val filmsDeferred = async { repository.getContent(ContentType.FILMS, ContentCategory.NEW, 1) }
                    val seriesDeferred = async { repository.getContent(ContentType.SERIES, ContentCategory.NEW, 1) }

                    val filmsResult = filmsDeferred.await()
                    val seriesResult = seriesDeferred.await()
                    
                    movies = filmsResult.first + seriesResult.first
                }
            }
        } catch (e: Exception) {
            movies = emptyList()
        }
    }

    override fun onDestroy() {
        movies = emptyList()
    }

    override fun getCount(): Int = movies.size

    override fun getViewAt(position: Int): RemoteViews {
        val movie = movies.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_list_item)
        
        // Создаем fill-in intent, который будет объединен с PendingIntent-шаблоном
        val fillInIntent = Intent().apply {
            val extras = Bundle()
            extras.putString("movie_url", movie.url) // Передаем URL фильма
            putExtras(extras)
        }

        return RemoteViews(context.packageName, R.layout.widget_list_item).apply {
            setTextViewText(R.id.widget_item_title, movie.title)
            // Устанавливаем fill-in intent для корневого элемента элемента списка
            setOnClickFillInIntent(R.id.widget_item_image, fillInIntent)

            try {
                val bitmap: Bitmap = Glide.with(context)
                    .asBitmap()
                    .load(movie.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .submit()
                    .get()
                setImageViewBitmap(R.id.widget_item_image, bitmap)
            } catch (e: Exception) {
                setImageViewResource(R.id.widget_item_image, R.drawable.ic_launcher_background)
            }
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.new_releases_widget_layout).apply {
            setTextViewText(R.id.widget_empty_view, context.getString(R.string.widget_loading))
        }
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = movies.getOrNull(position)?.url?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
