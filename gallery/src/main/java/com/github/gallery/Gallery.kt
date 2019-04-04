package com.github.gallery

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import java.io.File
import kotlin.math.max
import kotlin.math.min


/**
 * 支持缩放查看的图片界面
 */

class Gallery : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(context!!, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return RecyclerView(context!!).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            layoutManager = GridLayoutManager(context!!, 1, RecyclerView.HORIZONTAL, false)
            PagerSnapHelper().attachToRecyclerView(this)
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val uris = arguments?.getParcelableArray("ne_gallery_items") as? Array<Uri> ?: arrayOf()
        var position = arguments?.getInt("ne_gallery_position") ?: 0
        position = min(uris.size - 1, max(0, position))

        val recyclerView = view as RecyclerView
        recyclerView.adapter = NEAdapter(uris)
        recyclerView.scrollToPosition(position)
    }

    private inner class NEAdapter(val items: Array<Uri>) : RecyclerView.Adapter<NEAdapter.NEHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): NEHolder {
            return NEHolder(ImageView(p0.context!!).apply {
                layoutParams = ViewGroup.LayoutParams(-1, -1)
                scaleType = ImageView.ScaleType.MATRIX
            })
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: NEHolder, position: Int) {
            val imageView = holder.itemView as ImageView
            val helper = holder.helper

            val uri = items[position]
            val scheme = uri.scheme
            val item =
                if ("http".equals(scheme, true) || "file".equals(scheme, true)) uri
                else Uri.fromFile(File(uri.path))
            val options = RequestOptions()
//                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
//                .skipMemoryCache(true)
            Glide.with(context!!)
                .load(item)
                .apply(options)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        imageView.setImageDrawable(resource)
                        helper.reset()
                        return true
                    }
                })
                .into(imageView)
            helper.reset()
        }

        inner class NEHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val helper: ImageHelper = ImageHelper(itemView as ImageView)

            init {
                itemView.setBackgroundColor(Color.BLACK)
                helper.listener = object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                        dismiss()
                        return super.onSingleTapConfirmed(e)
                    }
                }
            }
        }
    }

    companion object {
        fun showGallery(fm: FragmentManager, position: Int, uris: Array<Uri>): Gallery {
            val gallery = Gallery()
            val args = Bundle()
            args.putInt("ne_gallery_position", position)
            args.putParcelableArray("ne_gallery_items", uris)
            gallery.arguments = args
            gallery.show(fm, null)
            return gallery
        }
    }
}
