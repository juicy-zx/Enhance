package com.juicy.main

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.SparseArray
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.juicy.main.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mainModel = ViewModelProviders.of(this).get(MainModel::class.java)
        binding.recyclerView.adapter = Adapter(mainModel.articleList)
        mainModel.resultLiveData.observe(this, Observer<ReportBo> { reportBo ->
            if (reportBo.success) {
                binding.recyclerView.adapter?.notifyDataSetChanged()
            } else {
                Toast.makeText(this, reportBo.errorMsg, Toast.LENGTH_SHORT).show()
            }
        })
        mainModel.getArticleData()
    }

    private inner class Adapter(private val list: List<WxArticle>) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = TextView(this@MainActivity)
            view.layoutParams = RecyclerView.LayoutParams(-1, 150)
            view.id = R.id.textView
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val view = holder.getView<TextView>(R.id.textView)
            view.gravity = Gravity.CENTER_VERTICAL
            view.setPadding(36, 0, 0, 0)
            view.setTextColor(Color.BLACK)
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            view.text = list[position].name
        }
    }

    private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewArray = SparseArray<View>()
        @Suppress("UNCHECKED_CAST")
        fun <T : View> getView(id: Int): T {
            var view = viewArray.get(id)
            if (view != null) {
                return view as T
            }
            view = itemView.findViewById<T>(id)
            viewArray.put(id, view)
            return view
        }
    }
}
