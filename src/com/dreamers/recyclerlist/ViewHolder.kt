package com.dreamers.recyclerlist

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.appinventor.components.runtime.AndroidViewComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.HorizontalArrangement

class ViewHolder(val component: AndroidViewComponent) : RecyclerView.ViewHolder(component.view) {

    companion object {
        @JvmStatic
        fun create(container: ComponentContainer): ViewHolder {
            val root = HorizontalArrangement(container).apply {
                // Remove root vertical arrangement from container
                Width(HorizontalArrangement.LENGTH_FILL_PARENT)
                (view.parent as ViewGroup).removeView(view)
            }
            return ViewHolder(root)
        }
    }

}
