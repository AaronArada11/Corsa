package com.corsa.app.ui.vehicles

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.corsa.app.R
import com.google.android.material.card.MaterialCardView

class VehicleListAdapter(
    private val onVehicleClick: (Long) -> Unit
) : ListAdapter<VehicleListItemUiModel, VehicleListAdapter.VehicleViewHolder>(VehicleDiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view, onVehicleClick)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VehicleViewHolder(
        itemView: View,
        private val onVehicleClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardVehicle: MaterialCardView = itemView.findViewById(R.id.cardVehicle)
        private val tvVehicleName: TextView = itemView.findViewById(R.id.tvVehicleName)
        private val tvPlateNumber: TextView = itemView.findViewById(R.id.tvPlateNumber)
        private val tvMileage: TextView = itemView.findViewById(R.id.tvMileage)

        fun bind(item: VehicleListItemUiModel) {
            val title = "${item.year} ${item.brand} ${item.model}"
            tvVehicleName.text = title
            tvMileage.text = item.mileageDisplay

            if (item.plateNumber.isNullOrBlank()) {
                tvPlateNumber.visibility = View.GONE
                tvPlateNumber.text = ""
            } else {
                tvPlateNumber.visibility = View.VISIBLE
                tvPlateNumber.text = item.plateNumber
            }

            cardVehicle.contentDescription = if (!item.plateNumber.isNullOrBlank()) {
                itemView.context.getString(R.string.vehicle_row_with_plate, title, item.plateNumber, item.mileageDisplay)
            } else {
                itemView.context.getString(R.string.vehicle_row_without_plate, title, item.mileageDisplay)
            }

            cardVehicle.setOnClickListener {
                onVehicleClick(item.id)
            }
        }
    }

    private object VehicleDiffCallback : DiffUtil.ItemCallback<VehicleListItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: VehicleListItemUiModel,
            newItem: VehicleListItemUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: VehicleListItemUiModel,
            newItem: VehicleListItemUiModel
        ): Boolean = oldItem == newItem
    }
}
