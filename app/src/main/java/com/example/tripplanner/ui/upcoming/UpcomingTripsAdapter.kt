package com.example.tripplanner.ui.upcoming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity
import com.example.tripplanner.databinding.TripItemBinding

class UpcomingTripsAdapter(private val isUpcoming: Boolean = true) :
    ListAdapter<TripEntity, UpcomingTripsAdapter.TripViewHolder>(DiffCallback()) {

    private lateinit var buttonClickListener: OnButtonClickListener
    fun setOnButtonClickListener(listener: OnButtonClickListener) {
        buttonClickListener = listener
    }

    interface OnButtonClickListener {
        fun onButtonClick(
            tripEntity: TripEntity,
            action: String,
            position: Int,
            rvView: View,
            text: String
        )
    }

    inner class TripViewHolder(private val binding: TripItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(tripEntity: TripEntity) {

            binding.apply {
                tvTripNameDescription.text = tripEntity.tripName
                tvStartPointDescription.text = tripEntity.startPoint
                tvEndPointDescription.text = tripEntity.endPoint
//                tvNotesDescription.text = tripEntity.note.split(",*herewecansplitit").toString()
                tvDateDescription.text = tripEntity.date
                 tvTimeDescription.text = tripEntity.time
                tvTripDistanceItem.text =
                    tripEntity.tripDistance.takeIf { it.isNotEmpty() } ?: "Not Available"
                tvTripDurationItem.text =
                    tripEntity.tripDuration.takeIf { it.isNotEmpty() } ?: "Not Available"

                if (!isUpcoming) {
                    btnBeginTrip.isEnabled = false
                    btnBeginTrip.text = tripEntity.tripStatus
                    btnBeginTrip.setTextColor(btnBeginTrip.context.resources.getColor(R.color.black))
                    btnBeginTrip.icon =
                        btnBeginTrip.context.getDrawable(R.drawable.check_circle_icon)

                } else {
                    tvTripDistanceItem.visibility = View.GONE
                    tvTripDurationItem.visibility = View.GONE
                }
                btnBeginTrip.setOnClickListener {
                    buttonClickListener.onButtonClick(
                        tripEntity,
                        "start",
                        adapterPosition,
                        it,
                        tripEntity.note.split(",*herewecansplitit").joinToString("\n")
                    )
                }
                moreMenu.setOnClickListener {
                    buttonClickListener.onButtonClick(
                        tripEntity,
                        "more",
                        adapterPosition,
                        it,
                        tripEntity.note.split(",*herewecansplitit").joinToString("\n")
                    )
                }
                tvNotesDescription.setOnClickListener {
                    buttonClickListener.onButtonClick(
                        tripEntity,
                        "dialog",
                        adapterPosition,
                        it,
                        tripEntity.note.split(",*herewecansplitit").joinToString("\n")
                    )
                }
                cvTripItem.setOnClickListener {
                    buttonClickListener.onButtonClick(
                        tripEntity,
                        "route",
                        adapterPosition,
                        it,
                        tripEntity.note.split(",*herewecansplitit").joinToString("\n")
                    )
                }
            }

        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TripEntity>() {
        override fun areItemsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
            return oldItem.tripId == newItem.tripId
        }

        override fun areContentsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = TripItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }


    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val currentItem = getItem(position)
        currentItem?.let {
            holder.bind(currentItem)
        }
    }


//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
//        binding = TripItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return TripViewHolder(binding)
//    }
//
//    override fun getItemCount(): Int {
//        return tripList.size
//    }
//
//    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
//        val currentItem = tripList[position]
//        holder.itemView.apply {
//            binding.apply {
//                tvTripNameDescription.text = currentItem.tripName
//                tvStartPointDescription.text = currentItem.startPoint
//                tvEndPointDescription.text = currentItem.endPoint
//                tvNotesDescription.text = currentItem.note
//                tvDateDescription.text = currentItem.date
//                tvTimeDescription.text = currentItem.time
//
//                btnBeginTrip.setOnClickListener {
//                    buttonClickListener?.onButtonClick(currentItem,"start",position)
//                }
////                fun showPopupMenu(view: View) {
////                    val popupMenu = PopupMenu(view.context, view)
////                    val menuInflater = popupMenu.menuInflater
////                    menuInflater.inflate(R.menu.item_menu, popupMenu.menu)
////                    try {
////                        val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
////                        fieldPopup.isAccessible = true
////                        val mPopup = fieldPopup.get(popupMenu)
////                        mPopup.javaClass
////                            .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
////                            .invoke(mPopup, true)
////                    }catch (e: Exception){
////                        Log.e("Adapter","error icons", e)
////                    }
////                    popupMenu.setOnMenuItemClickListener { menuItem ->
////                        buttonClickListener?.onMenuItemClick(currentItem, menuItem.itemId, position)
////                        true
////                    }
////                    popupMenu.show()
////                }
////                moreMenu.setOnClickListener {
////                    showPopupMenu(moreMenu)
////                }
//                delete.setOnClickListener {
//                    buttonClickListener?.onButtonClick(currentItem, "delete", position)
//                }
//
//                edit.setOnClickListener {
//                    buttonClickListener?.onButtonClick(currentItem, "edit", position)
//                }
//
//            }
//        }
//    }
//    @SuppressLint("NotifyDataSetChanged")
//    fun setData(trip: List<TripEntity>) {
//        this.tripList = trip
//        notifyDataSetChanged()
//    }
////    fun deleteItem(position: Int){
////        tripList.removeAt(position)
////        notifyItemRemoved(position)
////    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    fun notifyUpdates(){
//       notifyDataSetChanged()
//    }
//    fun removed(position: Int){
//        notifyItemRemoved(position)
//    }
//    interface OnButtonClickListener {
//        fun onButtonClick(tripEntity: TripEntity,action: String, position: Int)
//      //  fun onMenuItemClick(tripEntity: TripEntity, menuItemId: Int, position: Int)
//    }
}
