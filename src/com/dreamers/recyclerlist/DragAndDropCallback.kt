package com.dreamers.recyclerlist

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.errors.YailRuntimeError

class DragAndDropCallback(private val recyclerList: RecyclerList,
                          private val adapter: RecyclerList.RecyclerAdapter,
                          private val transparentView: Boolean,
                          //private val enableSwipeOld: Boolean,
                          private val useHandle: Boolean
                          ) : ItemTouchHelper.Callback() {

    private var currentViewHolder: RecyclerView.ViewHolder? = null
    private var startPosition: Int? = 0

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        var swipeFlags = 0
        //if (enableSwipe)
        if (adapter.enableSwipe)
            swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Can be used for swipes if needed
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        currentViewHolder = viewHolder
        adapter.moveMyItem(fromPosition, toPosition)
        EventDispatcher.dispatchEvent(recyclerList, "OnMoveTriggered", fromPosition, toPosition)

        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                // Element selected for dragging
                //if (transparentView == true) {
                if (adapter.enableTrnsp){
                    viewHolder?.itemView?.alpha = 0.5f // Change the transparency of an element
                }
                currentViewHolder = viewHolder
                startPosition = viewHolder?.adapterPosition
                EventDispatcher.dispatchEvent(recyclerList, "OnMoveStart", startPosition)
            }
            else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE ){
                currentViewHolder = viewHolder
                startPosition = viewHolder?.adapterPosition
            }
            else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                // The element is no longer draggable or swiped
                //if (transparentView == true) {
                if (adapter.enableTrnsp){
                    currentViewHolder?.itemView?.alpha = 1.0f // Restore the element's original transparency
                }
                EventDispatcher.dispatchEvent(recyclerList, "OnMoveEnd", startPosition, currentViewHolder?.adapterPosition)
                currentViewHolder = null
                startPosition = 0
            }
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        try {
            val position = viewHolder.adapterPosition
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    EventDispatcher.dispatchEvent(
                        recyclerList, "OnItemSwiped", position, "left"
                    )
                }

                ItemTouchHelper.RIGHT -> {
                    EventDispatcher.dispatchEvent(
                        recyclerList, "OnItemSwiped", position, "right"
                    )
                }
            }
            adapter.removeMyItem(position)
        } catch (e: Exception)
        {
            throw YailRuntimeError("Got an error inside the invoke", "DragAndDropCallback")

        }

    }

    override fun isLongPressDragEnabled(): Boolean {
        return !useHandle
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }

}
