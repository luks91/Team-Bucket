/**
 * Copyright (c) 2017-present, PR Paradise Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 *
 * The code snippet below comes from https://gist.github.com/aprock/6213395.
 */

package com.github.luks91.teambucket.util

import android.graphics.*
import com.squareup.picasso.Transformation

class PicassoCircleTransformation(private val radius: Float? = null, private val margin: Float = 0f) : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawRoundRect(margin, margin, source.width - margin, source.height - margin,
                radius ?: source.width.toFloat() / 2, radius ?: source.height.toFloat() / 2, paint)
        if (source != output) {
            source.recycle()
        }
        return output
    }

    override fun key(): String {
        return "rounded(radius=$radius, margin=$margin)"
    }
}