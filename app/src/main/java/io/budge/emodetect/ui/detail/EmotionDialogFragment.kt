package io.budge.emodetect.ui.detail

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import io.budge.emodetect.R
import kotlinx.android.synthetic.main.fragment_emotion_dialog.view.*

class EmotionDialogFragment : DialogFragment() {
    interface OnEmotionSelectedListener {
        fun onEmotionSelected(emotion: String, position: Int?)
    }

    private var mEmotionSelectedListener: OnEmotionSelectedListener? = null
    private var position: Int? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mEmotionSelectedListener = targetFragment as OnEmotionSelectedListener?
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder =
            AlertDialog.Builder(context!!)
                .setPositiveButton("Okay") { _, _ ->
                }
                .setNegativeButton(
                    "CANCEL"
                ) { _, _ -> }
        val rootView =
            LayoutInflater.from(activity).inflate(R.layout.fragment_emotion_dialog, null)
        builder.setView(rootView)
        position = arguments!!.getInt("position_key")
        rootView.emotion_radio_group.setOnCheckedChangeListener { _, checkedId ->
            Toast.makeText(context, "click", Toast.LENGTH_SHORT).show()
            onEmotionSelected(checkedId)
        }
        return builder.create()
    }

    private fun onEmotionSelected(id: Int) { // Is the button now checked?
        when (id) {
            R.id.happy_radio_button -> {
                mEmotionSelectedListener!!.onEmotionSelected("happiness", position)
            }
            R.id.sad_radio_button -> {
                mEmotionSelectedListener!!.onEmotionSelected("sadness", position)
            }
            R.id.angry_radio_button -> {
                mEmotionSelectedListener!!.onEmotionSelected("anger", position)
            }
        }
    }

    companion object {
        fun newInstance(position: Int): EmotionDialogFragment {
            val args = Bundle()
            args.putInt("position_key", position)
            val fragment = EmotionDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}