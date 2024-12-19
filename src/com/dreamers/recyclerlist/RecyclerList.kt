package com.dreamers.recyclerlist

import android.content.Context
import android.view.View
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import android.view.MotionEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dreamers.recyclerlist.utils.*
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.AndroidViewComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.util.YailList

@Suppress("FunctionName")
class RecyclerList(private val container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()){

    private val context: Context = container.`$context`()
    private val dynamicComponents: DynamicComponents = DynamicComponents()

    private var recyclerView: RecyclerView? = null
    private var customScrollBar: View? = null

    private var itemTouchHelper: ItemTouchHelper? = null
    private var animator: ItemAnimator = ItemAnimator.Default
    private var dataList: YailList = YailList.makeEmptyList()
    private var transparentView: Boolean = false
    private var enableSwipe: Boolean = false
    private var stringHandleId: String = ""
    private var uniqueId: String = ""
    private var mylog: String = ""

    private fun createAdapter(): RecyclerView.Adapter<ViewHolder> {
        return RecyclerAdapter()
    }

    private val Int.px: Int
        get() {
            val metrics = context.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), metrics).toInt()
        }

    @SimpleFunction(
        description = "Initialize recycler view inside a layout."
    )
    fun Initialize(
        `in`: AndroidViewComponent,
        layoutManager: String,
        snapHelper: String,
        orientation: Int,
        reverse: Boolean,
        spanCount: Int,
        dragByHandle: Boolean,
        swipe: Boolean,
        scrollBarColor: String,
        padRight: Int
    ) {
        recyclerView = RecyclerView(context).apply {
            this.layoutManager = ListManager.valueOf(layoutManager).getLayoutManager(
                context,
                orientation,
                reverse,
                spanCount
            )
            enableSwipe = swipe
            adapter = createAdapter()
            // Создаем itemTouchHelper после инициализации адаптера


            if (dragByHandle) {
                val touchHelper = initializeItemTouchHelper(this)
                (adapter as RecyclerAdapter).itemTouchHelper = touchHelper
            }

            ListSnapHelper.valueOf(snapHelper).getSnapHelper()?.attachToRecyclerView(this)
            itemAnimator = animator.getAnimator()

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    OnScrollStateChanged(newState)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    // Получение данных о прокрутке
                    val totalScrollRange = recyclerView.computeVerticalScrollRange()
                    val verticalOffset = recyclerView.computeVerticalScrollOffset()
                    val visibleHeight = recyclerView.computeVerticalScrollExtent()

                    // Рассчитываем высоту и позицию скроллбара (бегунка)
                    val scrollBarHeight = (visibleHeight.toFloat() / totalScrollRange * visibleHeight).toInt()
                    val scrollPercentage = verticalOffset.toFloat() / (totalScrollRange - visibleHeight)

                    // Обновляем параметры кастомного скроллбара
                    customScrollBar?.layoutParams?.height = scrollBarHeight
                    customScrollBar?.translationY = scrollPercentage * (visibleHeight - scrollBarHeight)

                    // Обновляем скроллбар
                    customScrollBar?.requestLayout()
                    OnScrolled(dx, dy)
                }
            })

            // Добавляем глобальный слушатель для установки размеров скроллбара при инициализации
            viewTreeObserver.addOnGlobalLayoutListener {
                val totalScrollRange = computeVerticalScrollRange()
                val visibleHeight = computeVerticalScrollExtent()

                // Рассчитываем высоту скроллбара
                val scrollBarHeight = (visibleHeight.toFloat() / totalScrollRange * visibleHeight).toInt()

                // Устанавливаем размер скроллбара
                customScrollBar?.layoutParams?.height = scrollBarHeight
                customScrollBar?.requestLayout()
            }
        }

        // Создаем GradientDrawable с закругленными углами
        val roundedBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val color: Int = Color.parseColor(scrollBarColor)
            setColor(color) // Цвет скроллбара
            cornerRadius = 10.px.toFloat() // Радиус закругления углов
        }

        // Создание кастомного скроллбара
        customScrollBar = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(4.px, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                // Устанавливаем выравнивание по правой стороне
                //(this as FrameLayout.LayoutParams).gravity = Gravity.END
                gravity = Gravity.END
                marginStart = 0 // Нулевой отступ слева
                marginEnd = 0 // Отступ справа
            }
            //setBackgroundColor(Color.GRAY) // Цвет скроллбара
            background = roundedBackground // Устанавливаем закругленный фон
            alpha = 0.5f // 70% непрозрачности
        }
        // Добавляем в контейнер
        (`in`.view as ViewGroup).apply {
            val frameLayout = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Устанавливаем отступы у самого контейнера
                setPadding(0, 0, padRight.px, 0) // Отступ в 5 пикселей справа
            }

            frameLayout.addView(recyclerView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            frameLayout.addView(customScrollBar, FrameLayout.LayoutParams(
                4.px,
                ViewGroup.LayoutParams.WRAP_CONTENT // Высота скроллбара будет изменяться динамически
            ).apply {
                gravity = Gravity.END
            })

            addView(frameLayout)
        }
    }

    // Адаптер как внутренний класс
    inner class RecyclerAdapter : RecyclerView.Adapter<ViewHolder>() {
        var itemTouchHelper: ItemTouchHelper? = null
        private var data: YailList = YailList.makeEmptyList()

        fun updateData(data: YailList) {
            if (this.data != data) {
                this.data = data
            }
        }


        fun moveMyItem(fromPosition: Int, toPosition: Int) {
            if (fromPosition < 0 || toPosition < 0 || fromPosition >= data.size || toPosition >= data.size) {
                return // Проверка на допустимые индексы
            }


            // Преобразуем YailList в изменяемый список
            val mutableData = data.toMutableList()

            // Перемещаем элемент
            val item = mutableData.removeAt(fromPosition) // Удаляем объект с позиции
            mutableData.add(toPosition, item)           // Вставляем объект в новую позицию

            // Преобразуем обратно в YailList
            data = YailList.makeList(mutableData)
            // Уведомляем адаптер об изменении позиций
            notifyItemMoved(fromPosition, toPosition)

        }


        fun getData(): YailList = data

        override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ViewHolder {
            val viewHolder = ViewHolder.create(container)
            OnCreateView(viewHolder.component)
            return viewHolder
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            OnBindView(viewHolder.component, position.inc(), data[position.inc()])

            val dragHandleId = stringHandleId//"DragHandleTag${position.inc()}"
            val dragHandle = dynamicComponents.getAndroidViewById(dragHandleId)

            // Удаляем старый listener перед установкой нового
            dragHandle?.view?.setOnTouchListener(null)

            if (dragHandle?.view != null) {
                    dragHandle.view.setOnTouchListener { _, event ->
                        if (event.action == 0) {
                            itemTouchHelper?.startDrag(viewHolder)
                        }
                        return@setOnTouchListener true
                    }
                }

        }

        override fun getItemCount(): Int = data.size
    }

    //@SimpleFunction(description = "Enable drag and drop functionality.")
    private fun initializeItemTouchHelper(recyclerView: RecyclerView) : ItemTouchHelper{
            val adapter = recyclerView.adapter as RecyclerAdapter//recyclerView.adapter as? RecyclerView.Adapter<RecyclerView.ViewHolder>
            val callback = DragAndDropCallback(this, adapter /*as RecyclerView.Adapter<RecyclerView.ViewHolder>*/, transparentView, enableSwipe)
            val itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper.attachToRecyclerView(recyclerView)

        return itemTouchHelper
    }

    @SimpleFunction("Notify any registered observers that the data set has changed.")
    fun NotifyDataSetChanged() {
        recyclerView?.adapter?.notifyDataSetChanged()
    }

    @SimpleFunction("Notify any registered observers that the item at `position` has changed.")
    fun NotifyItemChanged(position: Int) {
        recyclerView?.adapter?.notifyItemChanged(position.dec())
    }

    @SimpleFunction("Notify any registered observers that the item reflected at `position` has been newly inserted.")
    fun NotifyItemInserted(position: Int) {
        recyclerView?.adapter?.notifyItemInserted(position.dec())
    }

    @SimpleFunction("Notify any registered observers that the item previously located at 'position' has been removed from the data set.")
    fun NotifyItemRemoved(position: Int) {
        recyclerView?.adapter?.notifyItemRemoved(position.dec())
    }

    @SimpleFunction("Notify any registered observers that the item reflected at `from` has been moved to `to`.")
    fun NotifyItemMoved(from: Int, to: Int) {
        recyclerView?.adapter?.notifyItemMoved(from.dec(), to.dec())
    }

    @SimpleFunction("Notify any registered observers that the currently reflected `count` items starting at `startPosition` have been newly inserted.")
    fun NotifyItemRangeChanged(startPosition: Int, count: Int) {
        recyclerView?.adapter?.notifyItemRangeChanged(startPosition.dec(), count)
    }

    @SimpleFunction("Notify any registered observers that the currently reflected `count` items starting at `startPosition` have been newly inserted.")
    fun NotifyItemRangeInserted(startPosition: Int, count: Int) {
        recyclerView?.adapter?.notifyItemRangeInserted(startPosition.dec(), count)
    }

    @SimpleFunction("Notify any registered observers that the currently reflected `count` items starting at `startPosition` have been removed.")
    fun NotifyItemRangeRemoved(startPosition: Int, count: Int) {
        recyclerView?.adapter?.notifyItemRangeRemoved(startPosition.dec(), count)
    }

    @SimpleFunction(
        description = "Add consistent gap between list items."
    )
    fun AddGapDecorator(gap: Int) {
        recyclerView?.apply {
            val spanCount = when (val manager = layoutManager) {
                is GridLayoutManager -> manager.spanCount
                is StaggeredGridLayoutManager -> manager.spanCount
                else -> 1
            }

            val orientation = when (val manager = layoutManager) {
                is GridLayoutManager -> manager.orientation
                is StaggeredGridLayoutManager -> manager.orientation
                is LinearLayoutManager -> manager.orientation
                else -> RecyclerView.VERTICAL
            }

            val decoration = MarginItemDecoration(orientation, spanCount, gap.px)
            addItemDecoration(decoration)
        }
    }

    @SimpleFunction(
        description = "Create a new component."
    )
    fun CreateComponent(`in`: AndroidViewComponent, name: Any, tag: String, properties: Any) {
        dynamicComponents.createComponent(`in`, name, tag, properties)
    }

    @SimpleFunction(description = "Create a drag handle component.")
    fun CreateComponentForDrag(`in`: AndroidViewComponent, properties: Any) {
        val handleComponent = dynamicComponents.createComponent(`in`, name, "DragHandleTag", properties)

        // Установите дополнительные параметры для handleComponent, если необходимо
    }

    @SimpleFunction(
        description = "Create components using JSON template."
    )
    fun CreateTemplate(`in`: AndroidViewComponent, template: String, parameters: YailList) {
        dynamicComponents.createComponentsFromJson(`in`, template, parameters)
    }

    @SimpleFunction(
        description = "Set properties of a component. You can either use JSON string or dictionary to set properties."
    )
    fun SetProperties(view: AndroidViewComponent, properties: Any) {
        dynamicComponents.setProperties(view, properties)
    }

    @SimpleFunction(
        description = "Set unique id of a view"
    )
    fun SetUniqueId(view: AndroidViewComponent, id: String) {
        if (id.equals("DragHandleTag"))
        {
            uniqueId = System.currentTimeMillis().toString()
            dynamicComponents.setUniqueId(view, uniqueId)
            stringHandleId = uniqueId
        }
        else
            dynamicComponents.setUniqueId(view, id)
    }

    @SimpleFunction(
        description = "Get component with unique id."
    )
    fun GetUniqueId(view: AndroidViewComponent): String = dynamicComponents.getUniqueId(view)


    @SimpleFunction(
        description = "Get component using tag. Make sure to set RootParent before using."
    )
    fun GetComponent(root: AndroidViewComponent, tag: String): AndroidViewComponent? {
        val view = root.findViewByTag(tag)
        return dynamicComponents.getAndroidView(view)
    }



    @SimpleFunction(
        description = "Get root view using component."
    )
    fun GetRootView(view: AndroidViewComponent): AndroidViewComponent? {
        val viewHolder = recyclerView?.findContainingViewHolder(view.view)
        return if (viewHolder is ViewHolder) viewHolder.component else null
    }

    @SimpleFunction("Returns the Adapter position of the item represented by `root`.")
    fun GetPosition(root: AndroidViewComponent): Int {
        return recyclerView?.findContainingViewHolder(root.view)?.adapterPosition?.inc() ?: 0
    }

    @SimpleFunction(
        description = "Returns true if the given component is dynamic."
    )
    fun IsDynamic(view: AndroidViewComponent): Boolean {
        return dynamicComponents.getAndroidView(view.view) != null
    }

    @SimpleFunction(
        description = "Scroll to position."
    )
    fun ScrollToPosition(position: Int) {
        recyclerView?.scrollToPosition(position.dec())
    }

    @SimpleFunction(
        description = "Smooth scroll to position."
    )
    fun SmoothScrollToPosition(position: Int) {
        recyclerView?.smoothScrollToPosition(position.dec())
    }

    @SimpleEvent(description = "Triggered when onMove is called during drag and drop.")
    fun OnMoveTriggered(fromPosition: Int, toPosition: Int) {
        EventDispatcher.dispatchEvent(this, "OnMoveTriggered", fromPosition, toPosition)
    }

    @SimpleEvent(description = "Triggered when two adjacent items are swapped.")
    fun OnMoveStart(position: Int) {
        EventDispatcher.dispatchEvent(this, "OnMoveStart", position)
    }

    @SimpleEvent(description = "Triggered when two adjacent items are swapped.")
    fun OnMoveEnd(startPos: Int, endPos: Int, string: String ) {
        EventDispatcher.dispatchEvent(this, "OnMoveEnd", startPos, endPos, string)
    }

    @SimpleEvent(
        description = "Event raised to create UI. Don't bind any data to the UI."
    )
    fun OnCreateView(root: AndroidViewComponent) {
        EventDispatcher.dispatchEvent(this, "OnCreateView", root)
    }

    @SimpleEvent(
        description = "Event raised to bind data to UI."
    )
    fun OnBindView(root: AndroidViewComponent, position: Int, dataItem: Any?) {
        EventDispatcher.dispatchEvent(this, "OnBindView", root, position, dataItem)
    }

    @SimpleEvent(
        description = "Event raised when scroll state changes."
    )
    fun OnScrollStateChanged(scrollState: Int) {
        EventDispatcher.dispatchEvent(this, "OnScrollStateChanged", scrollState)
    }

    @SimpleEvent(
        description = "Event raised when scroll event occurs."
    )
    fun OnScrolled(dx: Int, dy: Int) {
        EventDispatcher.dispatchEvent(this, "OnScrolled", dx, dy)
    }

    @SimpleEvent(
        description = "Called when a view created by adapter has been attached to a window."
    )
    fun OnAttachToWindow(root: AndroidViewComponent) {
        EventDispatcher.dispatchEvent(this, "OnAttachToWindow", root)
    }

    @SimpleEvent(
        description = "Called when a view created by adapter has been detached from its window."
    )
    fun OnDetachFromWindow(root: AndroidViewComponent) {
        EventDispatcher.dispatchEvent(this, "OnDetachFromWindow", root)
    }

    @SimpleEvent(description = "Triggered when item is swiped.")
    fun OnItemSwiped(position: Int, direction: Int) {
        EventDispatcher.dispatchEvent(this, "OnItemSwiped", position, direction)
    }

    @SimpleProperty(
        description = "Update recycler view data. This causes recycler view to recreate views."
    )
    fun TransparentView(value: Boolean) {
        transparentView = value
    }

    @SimpleProperty(
        description = "Update recycler view data. This causes recycler view to recreate views."
    )
    fun Data(list: YailList) {
        (recyclerView?.adapter as? RecyclerAdapter)?.updateData(list)
    }

    @SimpleProperty(
        description = "Get recycler view data."
    )
    fun Data(): YailList {
        return (recyclerView?.adapter as? RecyclerAdapter)?.getData() ?: YailList.makeEmptyList()
    }

    @SimpleProperty()
    fun MyLog(): String {
        return mylog
    }

    @SimpleProperty(
        description = "Returns the adapter position of the first visible view."
    )
    fun FirstVisibleItem(): Int {
        return when (val manager = recyclerView?.layoutManager) {
            is GridLayoutManager -> manager.findFirstVisibleItemPosition().inc()
            is StaggeredGridLayoutManager -> manager.findFirstVisibleItemPositions(IntArray(manager.spanCount)).first()
                .inc()
            is LinearLayoutManager -> manager.findFirstVisibleItemPosition().inc()
            else -> -1
        }
    }

    @SimpleProperty(
        description = "Returns the adapter position of the first fully visible view."
    )
    fun FirstCompletelyVisibleItem(): Int {
        return when (val manager = recyclerView?.layoutManager) {
            is GridLayoutManager -> manager.findFirstCompletelyVisibleItemPosition().inc()
            is StaggeredGridLayoutManager -> manager.findFirstCompletelyVisibleItemPositions(IntArray(manager.spanCount))
                .first().inc()
            is LinearLayoutManager -> manager.findFirstCompletelyVisibleItemPosition().inc()
            else -> -1
        }
    }

    @SimpleProperty(
        description = "Returns the adapter position of the last visible view."
    )
    fun LastVisibleItem(): Int {
        return when (val manager = recyclerView?.layoutManager) {
            is GridLayoutManager -> manager.findLastVisibleItemPosition().inc()
            is StaggeredGridLayoutManager -> manager.findLastVisibleItemPositions(IntArray(manager.spanCount)).last()
                .inc()
            is LinearLayoutManager -> manager.findLastVisibleItemPosition().inc()
            else -> -1
        }
    }

    @SimpleProperty(
        description = "Returns the adapter position of the last fully visible view."
    )
    fun allDynTags(): String {
        val allTags = dynamicComponents.allTags() // Получаем список всех тегов
        return allTags.joinToString(", ") // Преобразуем список в строку, разделенную запятыми
    }

    @SimpleProperty(
        description = "Returns the adapter position of the last fully visible view."
    )
    fun LastCompletelyVisibleItem(): Int {
        return when (val manager = recyclerView?.layoutManager) {
            is GridLayoutManager -> manager.findLastCompletelyVisibleItemPosition().inc()
            is StaggeredGridLayoutManager -> manager.findLastCompletelyVisibleItemPositions(IntArray(manager.spanCount))
                .last()
                .inc()
            is LinearLayoutManager -> manager.findLastCompletelyVisibleItemPosition().inc()
            else -> -1
        }
    }

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        defaultValue = "Default",
        editorArgs = [
            "Default",
            "LandingAnimator",
            "ScaleInAnimator", "ScaleInTopAnimator", "ScaleInBottomAnimator", "ScaleInLeftAnimator", "ScaleInRightAnimator",
            "FadeInAnimator", "FadeInDownAnimator", "FadeInUpAnimator", "FadeInLeftAnimator", "FadeInRightAnimator",
            "FlipInTopXAnimator", "FlipInBottomXAnimator", "FlipInLeftYAnimator", "FlipInRightYAnimator",
            "SlideInLeftAnimator", "SlideInRightAnimator", "OvershootInLeftAnimator", "OvershootInRightAnimator",
            "SlideInUpAnimator", "SlideInDownAnimator"
        ]
    )
    @SimpleProperty("Set `Animations` that take place on items as changes are made to the adapter.")
    fun ItemAnimator(animator: String) {
        this.animator = ItemAnimator.valueOf(animator)
        recyclerView?.itemAnimator = this.animator.getAnimator()
    }

    @SimpleProperty
    fun ItemAnimator() = animator.name

    @SimpleProperty
    fun DefaultAnimator() = ItemAnimator.Default.name

    @SimpleProperty
    fun LandingAnimator() = ItemAnimator.LandingAnimator.name

    @SimpleProperty
    fun FadeInAnimator() = ItemAnimator.FadeInAnimator.name

    @SimpleProperty
    fun FadeInDownAnimator() = ItemAnimator.FadeInDownAnimator.name

    @SimpleProperty
    fun FadeInUpAnimator() = ItemAnimator.FadeInUpAnimator.name

    @SimpleProperty
    fun FadeInLeftAnimator() = ItemAnimator.FadeInLeftAnimator.name

    @SimpleProperty
    fun FadeInRightAnimator() = ItemAnimator.FadeInRightAnimator.name

    @SimpleProperty
    fun ScaleInAnimator() = ItemAnimator.ScaleInAnimator.name

    @SimpleProperty
    fun ScaleInTopAnimator() = ItemAnimator.ScaleInTopAnimator.name

    @SimpleProperty
    fun ScaleInBottomAnimator() = ItemAnimator.ScaleInBottomAnimator.name

    @SimpleProperty
    fun ScaleInLeftAnimator() = ItemAnimator.ScaleInLeftAnimator.name

    @SimpleProperty
    fun ScaleInRightAnimator() = ItemAnimator.ScaleInRightAnimator.name

    @SimpleProperty
    fun FlipInTopXAnimator() = ItemAnimator.FlipInTopXAnimator.name

    @SimpleProperty
    fun FlipInBottomXAnimator() = ItemAnimator.FlipInBottomXAnimator.name

    @SimpleProperty
    fun FlipInLeftYAnimator() = ItemAnimator.FlipInLeftYAnimator.name

    @SimpleProperty
    fun FlipInRightYAnimator() = ItemAnimator.FlipInRightYAnimator.name

    @SimpleProperty
    fun SlideInLeftAnimator() = ItemAnimator.SlideInLeftAnimator.name

    @SimpleProperty
    fun SlideInRightAnimator() = ItemAnimator.SlideInRightAnimator.name

    @SimpleProperty
    fun OvershootInLeftAnimator() = ItemAnimator.OvershootInLeftAnimator.name

    @SimpleProperty
    fun OvershootInRightAnimator() = ItemAnimator.OvershootInRightAnimator.name

    @SimpleProperty
    fun SlideInUpAnimator() = ItemAnimator.SlideInUpAnimator.name

    @SimpleProperty
    fun SlideInDownAnimator() = ItemAnimator.SlideInDownAnimator.name


    @SimpleProperty
    fun LinearLayoutManager() = ListManager.Linear.name

    @SimpleProperty
    fun GridLayoutManager() = ListManager.Grid.name

    @SimpleProperty
    fun StaggeredLayoutManager() = ListManager.Staggered.name

    @SimpleProperty
    fun Vertical() = RecyclerView.VERTICAL

    @SimpleProperty
    fun Horizontal() = RecyclerView.HORIZONTAL

    @SimpleProperty
    fun LinearSnapHelper() = ListSnapHelper.Linear.name

    @SimpleProperty
    fun PagerSnapHelper() = ListSnapHelper.Pager.name

    @SimpleProperty
    fun NoSnapHelper() = ListSnapHelper.None.name

    @SimpleProperty
    fun ScrollStateIdle() = RecyclerView.SCROLL_STATE_IDLE

    @SimpleProperty
    fun ScrollStateDragging() = RecyclerView.SCROLL_STATE_DRAGGING

    @SimpleProperty
    fun ScrollStateSettling() = RecyclerView.SCROLL_STATE_SETTLING
}
