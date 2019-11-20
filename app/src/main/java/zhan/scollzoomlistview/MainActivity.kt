package zhan.scollzoomlistview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.widget.BaseAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        //设置全屏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)
        initData()
    }

    private fun initData() {
        val mData = ArrayList<Int>()
        mData.add(R.mipmap.comic1)
        mData.add(R.mipmap.comic2)
        mData.add(R.mipmap.comic3)
        mData.add(R.mipmap.comic4)
        mData.add(R.mipmap.comic5)
        mData.add(R.mipmap.comic6)
        mData.add(R.mipmap.comic7)
        mData.add(R.mipmap.comic8)
        mData.add(R.mipmap.comic9)
        mData.add(R.mipmap.comic10)

        val adapter = MyAdapter()
        scrollZoomListView.adapter = adapter
        adapter.setData(mData)
    }

    private inner class MyAdapter : BaseAdapter() {

        private var mData = ArrayList<Int>()

        fun setData(data: List<Int>) {
            mData.clear()
            mData.addAll(data)
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return mData.size
        }

        override fun getItem(position: Int): Any {
            return mData[position]
        }

        override fun getItemId(position: Int): Long {
            return (position + 1000).toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val holder: MyHolder
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item, parent, false)
                holder = MyHolder()
                holder.picIv = convertView!!.findViewById<View>(R.id.pic_iv) as ImageView
                convertView.tag = holder
            } else {
                holder = convertView.tag as MyHolder
            }
            Glide.with(parent.context)
                    .load(mData[position])
                    .dontAnimate()
                    .into(holder.picIv!!)

            return convertView
        }
    }

    internal class MyHolder {
        var picIv: ImageView? = null
    }
}