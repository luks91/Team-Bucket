/**
 * Copyright (c) 2017-present, Team Bucket Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.luks91.teambucket.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

internal class ImageViewTarget(private val imageView: ImageView): Target {

    init {
        //Picasso holds a WeakReference to the target, we need to strongly hold it here
        imageView.tag = this
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        imageView.setImageBitmap(bitmap)
    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
        imageView.setImageDrawable(errorDrawable)
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        imageView.setImageDrawable(placeHolderDrawable)
    }
}
