package com.specfive.meshtactoe.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.specfive.meshtactoe.R
import com.specfive.meshtactoe.databinding.AdapterTickToeBinding
import com.specfive.meshtactoe.fragments.GamePlayFragment
import java.util.UUID


enum class TickTacToeEnum {
    None,
    Green,
    Blue
}

data class TickTackToeOptionState(
    var selectedState: TickTacToeEnum = TickTacToeEnum.None,
    val uid: UUID = UUID.randomUUID()
)

class GamePlayAdapter(
    private val onClick: (Int, TickTacToeEnum) -> Unit
) : RecyclerView.Adapter<GamePlayAdapter.ViewHolder>() {

    var isGreenPlayer = true
    private var list: List<TickTackToeOptionState> = listOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            AdapterTickToeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun submitList(list: List<TickTackToeOptionState>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun setMove(index: Int, type: TickTacToeEnum) {
        list[index].selectedState = type
        notifyItemChanged(index)
    }

    fun getList(): List<TickTackToeOptionState> {
        return list
    }


    var lastKnownAngle = 0f

    inner class ViewHolder(
        private val binding: AdapterTickToeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(model: TickTackToeOptionState) {

            val indx = list.indexOf(model)
            binding.imageview.setOnClickListener {
                if (GamePlayFragment.gameActive &&
                    model.selectedState == TickTacToeEnum.None
                ) {
                    onClick.invoke(
                        indx,
                        if (isGreenPlayer) TickTacToeEnum.Blue else TickTacToeEnum.Green
                    )
                }
            }

            binding.imageview.setImageResource(
                when (model.selectedState) {
                    TickTacToeEnum.None -> R.drawable.ic_tick_none
                    TickTacToeEnum.Green -> R.drawable.ic_tick_o
                    TickTacToeEnum.Blue -> R.drawable.ic_tick_x
                }

            )
        }

    }
}