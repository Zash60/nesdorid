package com.example.nesdorid

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RomAdapter(private val roms: List<String>, private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<RomAdapter.RomViewHolder>() {

    class RomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val romIcon: ImageView = itemView.findViewById(R.id.romIcon)
        val romName: TextView = itemView.findViewById(R.id.romName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rom, parent, false)
        return RomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RomViewHolder, position: Int) {
        val rom = roms[position]
        val uri = Uri.parse(rom)
        val displayName = uri.lastPathSegment ?: "Unknown ROM"
        holder.romName.text = displayName
        holder.itemView.setOnClickListener { onItemClick(rom) }
    }

    override fun getItemCount(): Int = roms.size
}