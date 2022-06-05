package meta.durjan.swipeanimation

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View.OnTouchListener
import kotlin.jvm.Synchronized
import android.view.View
import androidx.core.animation.doOnEnd
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@SuppressLint("ClickableViewAccessibility")
abstract class SwipeHelper (
    private val recyclerView: RecyclerView
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private var swipedPos = -1
    private var swipeThreshold = 0.1f
    private var buttons = ArrayList<UnderlayButton>()
    private val buttonsBuffer = HashMap<Int, ArrayList<UnderlayButton>>()
    private val recoverQueue: Queue<Int>
    private val gestureDetector: GestureDetector

    private val gestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            for (button in buttons) {
                if (button.onClick(e.x, e.y)) {
                    break
                }
            }
            return true
        }
    }

    init {
        gestureDetector = GestureDetector(recyclerView.context, gestureListener)
        recoverQueue = object : LinkedList<Int>() {
            override fun add(element: Int): Boolean {
                return if (contains(element)) false else super.add(element)
            }
        }
    }

    private val onTouchListener = OnTouchListener { view, e ->
        if (swipedPos < 0)
            return@OnTouchListener false
        val point = Point(e.rawX.toInt(), e.rawY.toInt())
        val swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPos)
                ?: return@OnTouchListener false
        val swipedItem = swipedViewHolder.itemView
        val rect = Rect()
        swipedItem.getGlobalVisibleRect(rect)
        if (e.action == MotionEvent.ACTION_DOWN ||
            e.action == MotionEvent.ACTION_UP ||
            e.action == MotionEvent.ACTION_MOVE) {
            if (rect.top < point.y && rect.bottom > point.y) //clicked on the viewholder
                gestureDetector.onTouchEvent(e)
            else {
                recoverQueue.add(swipedPos)
                swipedPos = -1
                recoverSwipedItem()
            }
        }
        false
    }

    init {
        recyclerView.setOnTouchListener(onTouchListener)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition
        if (swipedPos != pos)
            recoverQueue.add(swipedPos)
        swipedPos = pos

        if (buttonsBuffer.containsKey(swipedPos))
            buttons = buttonsBuffer[swipedPos]!!
        else
            buttons.clear()

        buttonsBuffer.clear()
        swipeThreshold = 0.1f * buttons.size * BUTTON_WIDTH
        recoverSwipedItem()
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.06f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 2.0f * defaultValue
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.adapterPosition
        var translationX = dX
        val itemView = viewHolder.itemView
        if (pos < 0) {
            swipedPos = pos
            return
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                var buffer = ArrayList<UnderlayButton>()
                if (!buttonsBuffer.containsKey(pos)) {
                    instantiateUnderlayButton(viewHolder, buffer)
                    buttonsBuffer[pos] = buffer
                } else {
                    buffer = buttonsBuffer[pos]!!
                }
                translationX = dX * buffer.size * BUTTON_WIDTH / itemView.width
                drawButtons(c, itemView, buffer, pos, translationX)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive)
    }

    @Synchronized
    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val pos = recoverQueue.poll()
            if (pos != null) {
                if (pos > -1) {
                    recyclerView.adapter?.notifyItemChanged(pos)
                }
            }
        }
    }

    private fun drawButtons(
        c: Canvas,
        itemView: View,
        buffer: List<UnderlayButton>,
        pos: Int,
        dX: Float
    ) {
        var right = itemView.right.toFloat()
        val dButtonWidth = -1 * dX / buffer.size
        for (button in buffer) {
            val left = right - dButtonWidth
            button.onDraw(
                c,
                RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()),
                pos
            )
            right = left
        }
    }

    fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    abstract fun instantiateUnderlayButton (
        viewHolder: RecyclerView.ViewHolder,
        underlayButtons: ArrayList<UnderlayButton>
    )

    class UnderlayButton(
        private val text: String,
        private val imageResId: Int,
        private val color: Int,
        private val clickListener: UnderlayButtonClickListener
    ) {
        private var pos = 0
        private var clickRegion: RectF? = null

        fun onClick(x: Float, y: Float): Boolean {
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                clickListener.onClick(pos)
                return true
            }
            return false
        }

        fun onDraw(c: Canvas, rect: RectF, pos: Int) {
            val p = Paint()

            // Draw background
            p.color = color
            c.drawRect(rect, p)

            // Draw Text
            p.color = Color.WHITE
            p.textSize = 40f
            val r = Rect()
            val cHeight = rect.height()
            val cWidth = rect.width()
            p.textAlign = Paint.Align.LEFT
            p.getTextBounds(text, 0, text.length, r)
            val x = cWidth / 2f - r.width() / 2f - r.left
            val y = cHeight / 2f + r.height() / 2f - r.bottom
            c.drawText(text, rect.left + x, rect.top + y, p)
            clickRegion = rect
            this.pos = pos
        }
    }

    interface UnderlayButtonClickListener {
        fun onClick(pos: Int)
    }

    interface AnimationUpdateListener {
        fun onSwipeAnimationEnd()
    }

    companion object {
        const val BUTTON_WIDTH = 220

        /**
         * Programmatically swipe RecyclerView item
         * @param recyclerView RecyclerView which item will be swiped
         * @param index Position of item
         * @param distance Swipe distance
         * @param direction Swipe direction, can be [ItemTouchHelper.START] or [ItemTouchHelper.END]
         * @param time Animation time in milliseconds
         */
        fun swipeRecyclerViewItem(
            recyclerView: RecyclerView,
            index: Int,
            distance: Int,
            direction: Int,
            time: Long,
            listener: AnimationUpdateListener?
        ) {
            val childView = recyclerView.getChildAt(index) ?: return
            val x = childView.width / 2F
            val viewLocation = IntArray(2)
            childView.getLocationInWindow(viewLocation)
            val y = (viewLocation[1] + childView.height) / 2F
            val downTime = SystemClock.uptimeMillis()
            recyclerView.dispatchTouchEvent(
                MotionEvent.obtain(
                    downTime,
                    downTime,
                    MotionEvent.ACTION_DOWN,
                    x,
                    y,
                    0
                )
            )
            val anim = ValueAnimator.ofInt(0, distance).apply {
                duration = time
                addUpdateListener {
                    val dX = it.animatedValue as Int
                    val mX = when (direction) {
                        ItemTouchHelper.END -> x + dX
                        ItemTouchHelper.START -> x - dX
                        else -> 0F
                    }
                    recyclerView.dispatchTouchEvent(
                        MotionEvent.obtain(
                            downTime,
                            SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_MOVE,
                            mX,
                            y,
                            0
                        )
                    )
                }

            }
            anim.doOnEnd {
                listener?.onSwipeAnimationEnd()
            }
            anim.start()
        }

    }

}