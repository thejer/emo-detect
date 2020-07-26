package io.budge.emodetect.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.budge.emodetect.R
import io.budge.emodetect.data.models.Inference
import kotlinx.android.synthetic.main.inference_item_view.view.*
import java.util.*

class InferenceHistoryAdapter(
    private val context: Context,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<InferenceHistoryAdapter.ViewHolder>() {
    private var list: ArrayList<Inference>? = null

    fun swapData(list: ArrayList<Inference>) {
        this.list = list
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(model: Inference,
            listener: OnItemClickListener
        ) {
            itemView.face_image.setImageBitmap(model.image)
            itemView.emotion.text = model.emotion
            itemView.setOnClickListener { v -> listener.onItemClick(layoutPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.inference_item_view,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list!![position]
        holder.bind(item, onItemClickListener)
    }


    override fun getItemCount(): Int {
        return list!!.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    companion object {
        private val TAG = InferenceHistoryAdapter::class.java.simpleName
    }

}