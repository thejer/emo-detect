package io.budge.emodetect.data.models

import android.graphics.Bitmap

class Inference(public val image: Bitmap,
                public val emotion: String,
                public val timeStamp: String)
