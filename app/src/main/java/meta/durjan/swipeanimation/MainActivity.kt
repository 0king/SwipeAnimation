package meta.durjan.swipeanimation

import android.animation.ValueAnimator
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import meta.durjan.swipeanimation.databinding.ActivityMainBinding
import meta.durjan.swipeanimation.databinding.ItemBinding

class MainActivity : AppCompatActivity() {

    private val viewBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewBinding.listView.layoutManager = LinearLayoutManager(this)
        viewBinding.listView.adapter = Adapter()
        attachSwipe()
        viewBinding.listView.post {
            showHintAnimation()

            //todo why animation not working
            //viewBinding.listView.getChildAt(0).translationX = -300F

            val v = viewBinding.listView.findViewHolderForAdapterPosition(0)?.itemView
            //v?.translationX = -300F
            //v?.x = -300F
            //v?.animate()?.translationX(-300F)

        }
    }

    fun translateItem() {
        SwipeHelper.swipeRecyclerViewItem(
            viewBinding.listView,
            0,
            600,
            ItemTouchHelper.START,
            1000,
            null
        )

//        val view = viewBinding.listView.getChildAt(0)
//        ObjectAnimator.ofFloat(view, "translationX", 100f).apply {
//            duration = 2000
//            start()
//        }
    }

    fun startShowHintAnimation() {
        //repeat animation 3 times
    }

    fun showHintAnimation() {
        val dist = 600
        SwipeHelper.swipeRecyclerViewItem(
            viewBinding.listView,
            0,
            dist,
            ItemTouchHelper.START,
            1500,
            object : SwipeHelper.AnimationUpdateListener {
                override fun onSwipeAnimationEnd() {
                    SwipeHelper.swipeRecyclerViewItem(
                        viewBinding.listView,
                        0,
                        dist,
                        ItemTouchHelper.END,
                        100,
                        object : SwipeHelper.AnimationUpdateListener {
                            override fun onSwipeAnimationEnd() {
                                showHintAnimation()
                            }
                        }
                    )
                }
            }
        )
    }

    private fun attachSwipe() {
        val helper = ItemTouchHelper(object : SwipeHelper(viewBinding.listView) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder,
                underlayButtons: ArrayList<UnderlayButton>
            ) {
                val col = Color.parseColor("#FAB957")
                underlayButtons.add(
                    //UnderlayButton("SKIP", 0, col) { pos: Int ->  } todo error why?
                    UnderlayButton("SKIP", 0, col, object: UnderlayButtonClickListener {
                        override fun onClick(pos: Int) {
                            Log.d("durga", "onClick: pos = $pos")
                        }
                    })
                )
            }
        })
        helper.attachToRecyclerView(viewBinding.listView)
    }

    class Adapter: RecyclerView.Adapter<Adapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.b.tv.text = "Item $position"
        }

        override fun getItemCount(): Int = 20

        class Holder(val b: ItemBinding): RecyclerView.ViewHolder(b.root)

    }

}