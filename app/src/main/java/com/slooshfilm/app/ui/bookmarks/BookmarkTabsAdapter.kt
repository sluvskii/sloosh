package com.slooshfilm.app.ui.bookmarks

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.slooshfilm.app.data.models.Bookmark
import com.slooshfilm.app.ui.profile.BookmarkTabFragment

class BookmarkTabsAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private var bookmarks: List<Bookmark> = emptyList()

    fun setBookmarks(newBookmarks: List<Bookmark>) {
        if (bookmarks != newBookmarks) {
            bookmarks = newBookmarks
            notifyDataSetChanged()
        }
    }

    fun getBookmarkAt(position: Int): Bookmark? {
        return bookmarks.getOrNull(position)
    }

    override fun getItemId(position: Int): Long {
        return bookmarks[position].id.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return bookmarks.any { it.id.hashCode().toLong() == itemId }
    }

    override fun getItemCount(): Int = bookmarks.size

    override fun createFragment(position: Int): Fragment {
        return BookmarkTabFragment.newInstance(bookmarks[position].id)
    }
}