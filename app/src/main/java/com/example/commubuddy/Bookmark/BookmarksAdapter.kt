package com.example.commubuddy.Bookmark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.commubuddy.R

class BookmarksAdapter(
    private val bookmarksList: List<BookmarksItem>,
    private val onBookmarkClick: (BookmarksItem) -> Unit
) : RecyclerView.Adapter<BookmarksAdapter.BookmarksViewHolder>() {

    class BookmarksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.bookmarks_item_name)
        private val distanceTextView: TextView = itemView.findViewById(R.id.bookmarks_item_distance)

        fun bind(bookmarksItem: BookmarksItem, onBookmarkClick: (BookmarksItem) -> Unit) {
            nameTextView.text = bookmarksItem.destinationName
            distanceTextView.text = "${bookmarksItem.ringDistance} meters"  // Or any format you prefer

            itemView.setOnClickListener {
                onBookmarkClick(bookmarksItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarksViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmarks, parent, false)
        return BookmarksViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarksViewHolder, position: Int) {
        val bookmarksItem = bookmarksList[position]
        holder.bind(bookmarksItem, onBookmarkClick)
    }

    override fun getItemCount(): Int = bookmarksList.size
}
