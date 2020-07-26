package io.budge.emodetect.ui.detail

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.budge.emodetect.R
import io.budge.emodetect.data.models.FaceInference
import kotlinx.android.synthetic.main.inference_item_view.view.*
import java.io.File

class FaceRecyclerViewAdapter(
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<FaceRecyclerViewAdapter.FaceViewHolder>() {

    private var faceInferences = mutableListOf<FaceInference>()

    class FaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            model: FaceInference,
            listener: OnItemClickListener
        ) {
            itemView.face_image.setImageURI(Uri.fromFile(File(model.faceFilePath)))
            val faceEmotion = model.faceEmotion
            itemView.emotion.text = if (faceEmotion == "no") "I am not sure" else faceEmotion.capitalize()
            val backgroundColor =
                when (faceEmotion.toLowerCase()) {
                    "happiness" -> R.color.happy_color
                    "sadness" -> R.color.sad_color
                    "anger" -> R.color.angry_color
                    else -> R.color.white
                }
            if (faceEmotion == "no") {
                itemView.right_button.visibility = View.GONE
                itemView.wrong_button.visibility = View.GONE
                itemView.help_button.visibility = View.VISIBLE
            } else {
                itemView.right_button.visibility = View.VISIBLE
                itemView.wrong_button.visibility = View.VISIBLE
                itemView.help_button.visibility = View.GONE
            }
            itemView.setBackgroundColor(
                ContextCompat.getColor(itemView.context, backgroundColor))
            itemView.help_button.setOnClickListener { listener.onWrongClicked(layoutPosition) }
            itemView.right_button.setOnClickListener { listener.onRightClicked(layoutPosition) }
            itemView.wrong_button.setOnClickListener { listener.onWrongClicked(layoutPosition) }
        }
    }

    fun swapItems(facesInferences: MutableList<FaceInference>) {
        this.faceInferences = facesInferences
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceViewHolder {
        return FaceViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.inference_item_view,
                    parent,
                    false
                )
        )
    }

    override fun getItemCount(): Int {
        return faceInferences.size
    }

    override fun onBindViewHolder(holder: FaceViewHolder, position: Int) {
        holder.bind(faceInferences[position], onItemClickListener)
    }

    interface OnItemClickListener {
        fun onRightClicked(position: Int)

        fun onWrongClicked(position: Int)
    }
}