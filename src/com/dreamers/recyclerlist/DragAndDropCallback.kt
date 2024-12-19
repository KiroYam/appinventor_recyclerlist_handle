package com.dreamers.recyclerlist

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.runtime.EventDispatcher

class DragAndDropCallback(private val recyclerList: RecyclerList,
                          private val adapter: RecyclerList.RecyclerAdapter,
                          private val transparentView: Boolean,
                          private val enableSwipe: Boolean
                          ) : ItemTouchHelper.Callback() {

    private var currentViewHolder: RecyclerView.ViewHolder? = null
    private var startPosition: Int? = 0

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        var swipeFlags = 0
        if (enableSwipe)
            swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Можно использовать для свайпов, если нужно
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
        //adapter.notifyItemMoved(fromPosition, toPosition)
        adapter.moveMyItem(fromPosition, toPosition)
        //EventDispatcher.dispatchEvent(recyclerList, "OnMoveTriggered", fromPosition, toPosition)
        // Обмен позиций элементов в адаптере
        //adapter.notifyItemMoved(fromPosition, toPosition)

        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                // Элемент выбран для перетаскивания

                if (transparentView == true) {
                    viewHolder?.itemView?.alpha = 0.5f // Например, изменить прозрачность элемента
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
                // Элемент больше не перетаскивается или не свайпается
                if (transparentView == true) {
                    currentViewHolder?.itemView?.alpha = 1.0f // Восстановить исходную прозрачность элемента
                }
                EventDispatcher.dispatchEvent(recyclerList, "OnMoveEnd", startPosition, currentViewHolder?.adapterPosition, currentViewHolder)
                //currentViewHolder = null
                startPosition = 0
            }
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
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
        adapter.notifyItemRemoved(position)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return enableSwipe
    }

}
