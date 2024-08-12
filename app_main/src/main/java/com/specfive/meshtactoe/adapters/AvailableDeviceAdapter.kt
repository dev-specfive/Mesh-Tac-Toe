package com.specfive.meshtactoe.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.specfive.app.R
import com.specfive.app.model.BTScanModel
import com.specfive.meshtactoe.databinding.AvailableDevicesBinding

class AvailableDeviceAdapter(
    private val context: Context,
    private val devices: ArrayList<BTScanModel.DeviceListEntry>,
    private var selectedAddress: String,
    isMeshConnected: Boolean,
    private val preSelectedIndex: Int
) : RecyclerView.Adapter<AvailableDeviceAdapter.ViewHolder>() {
    private var selectedPosition = -1
    private var listener: OnItemClickListener? = null
    init {
        selectedPosition = preSelectedIndex
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val deviceView = AvailableDevicesBinding.inflate(inflater, parent, false)
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int = devices.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.name

        if (selectedPosition == position){
            holder.itemView.background = ContextCompat.getDrawable(context, R.drawable.device_selected_background)
            holder.logo.setColorFilter(ContextCompat.getColor(context, R.color.green_color), android.graphics.PorterDuff.Mode.MULTIPLY)
            selectedPosition = holder.bindingAdapterPosition
            listener?.onItemClick(device)
        }
        else
        {
            holder.logo.background = null
            holder.itemView.background = ContextCompat.getDrawable(context, R.drawable.my_profile_background)
            holder.logo.setColorFilter(ContextCompat.getColor(context, R.color.colourGreyNavbar), android.graphics.PorterDuff.Mode.MULTIPLY)
            holder.logo.setImageResource(R.drawable.default_radio)
        }
        holder.itemView.setOnClickListener {
            if (selectedPosition >= 0 ){
                notifyItemChanged(selectedPosition)
            }
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(selectedPosition)
        }
    }
    fun setDisconnectedSelection(flag:String){
        selectedAddress = flag
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: AvailableDevicesBinding) : RecyclerView.ViewHolder(itemView.root) {
        val deviceName = itemView.tvDeviceName
        val logo = itemView.ivLogo
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    interface OnItemClickListener {
        fun onItemClick(selectedDevice: BTScanModel.DeviceListEntry)
    }
}