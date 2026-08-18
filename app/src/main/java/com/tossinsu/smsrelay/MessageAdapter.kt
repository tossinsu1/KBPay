package com.tossinsu.smsrelay

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tossinsu.smsrelay.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.VH>() {

    private val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    private var data: List<RelayMessage> = emptyList()

    fun submit(list: List<RelayMessage>) {
        data = list
        notifyDataSetChanged()
    }

    inner class VH(val v: ItemMessageBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val bind = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(bind)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = data[position]
        holder.v.from.text = m.from
        holder.v.body.text = m.body
        holder.v.time.text = fmt.format(Date(m.ts))
    }

    override fun getItemCount() = data.size
}
