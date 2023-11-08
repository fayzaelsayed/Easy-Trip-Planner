package com.example.tripplanner.ui.upcoming

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tripplanner.R
import com.example.tripplanner.database.TripEntity

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingWidgetView: View
    private lateinit var closeBubbleImage: ImageView
    private lateinit var note: TextView
    private lateinit var logo: ImageView
    private var height = 0
    private var width = 0
    private lateinit var notesCheckListAdapter: NotesCheckListAdapter
    private lateinit var popupWindow: PopupWindow
    private lateinit var popupView: View
    private lateinit var checkList: RecyclerView

    private var LAYOUT_FLAG = 0


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility", "InvalidWakeLockTag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE

        }
        popupWindow = PopupWindow(this)
        popupView =
            LayoutInflater.from(this).inflate(R.layout.dialog_check_list, null)
        checkList =
            popupView.findViewById<RecyclerView>(R.id.rv_check_list_dialog)
        notesCheckListAdapter = NotesCheckListAdapter()
        checkList.layoutManager = LinearLayoutManager(checkList.context,LinearLayoutManager.VERTICAL, false)
        checkList.adapter = notesCheckListAdapter


        //inflate widget layout
        floatingWidgetView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )


        //initial position
        layoutParams.gravity = Gravity.TOP or Gravity.RIGHT
        layoutParams.x = 100
        layoutParams.y = 0


//        logo = floatingWidgetView.findViewById(R.id.bubble_logo)
//        logo.setImageResource(R.drawable.logo)

        note = floatingWidgetView.findViewById(R.id.bubble_text)
        val value = intent?.getStringExtra("notes")

        // note.text = value
        //note.text = "Click \n To See Your Notes"

        //layout params for close button
        val imageParams = WindowManager.LayoutParams(
            140,
            140,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        imageParams.gravity = Gravity.BOTTOM or Gravity.CENTER
        imageParams.y = 100

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        closeBubbleImage = ImageView(this)
        closeBubbleImage.setImageResource(R.drawable.close_bubble_icon_white)
        closeBubbleImage.visibility = View.INVISIBLE


        windowManager.addView(closeBubbleImage, imageParams)
        windowManager.addView(floatingWidgetView, layoutParams)
        floatingWidgetView.visibility = View.VISIBLE

        height = windowManager.defaultDisplay.height
        width = windowManager.defaultDisplay.width


        //drag movement for widget
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val vc: ViewConfiguration = ViewConfiguration.get(this)
        val mSlop: Int = vc.scaledTouchSlop
        var draggingBubble = false
        floatingWidgetView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    closeBubbleImage.visibility = View.VISIBLE
                    initialX = layoutParams.x
                    initialY = layoutParams.y

                    //touch position
                    initialTouchX = motionEvent.rawX
                    initialTouchY = motionEvent.rawY
                    draggingBubble = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    //calculate x,y coordinates of view
                    layoutParams.x = initialX + ((initialTouchX - motionEvent.rawX)).toInt()
                    layoutParams.y = initialY + ((motionEvent.rawY - initialTouchY)).toInt()

                    if (Math.abs((motionEvent.rawX - initialTouchX)) > mSlop || Math.abs((motionEvent.rawY - initialTouchY)) > mSlop) {
                        draggingBubble = true
                    }
                    //update layout with new coordinates
                    windowManager.updateViewLayout(floatingWidgetView, layoutParams)
                    if (layoutParams.y > (height * 0.6)) {
                        closeBubbleImage.setImageResource(R.drawable.close_bubble_image)
                    } else {
                        closeBubbleImage.setImageResource(R.drawable.close_bubble_icon_white)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    closeBubbleImage.visibility = View.GONE
                    layoutParams.x = initialX + ((initialTouchX - motionEvent.rawX)).toInt()
                    layoutParams.y = initialY + ((motionEvent.rawY - initialTouchY)).toInt()

                    if (layoutParams.y > (height * 0.6)) {
                        stopSelf()
                    }
                    if (!draggingBubble) {
                        val entity = intent?.getParcelableExtra<TripEntity>("tripEntity")
                        val list = entity?.note?.split(",*herewecansplitit")


                        fun showMultiChoiceDialog() {
                            val dialog = LayoutInflater.from(
                                ContextThemeWrapper(
                                    this,
                                    R.style.Theme_TripPlanner
                                )
                            ).inflate(R.layout.dialog_check_list, null)
                            val okButton = dialog.findViewById<Button>(R.id.btn_ok)
                            val checkList =
                                dialog.findViewById<RecyclerView>(R.id.rv_check_list_dialog)
                            val dialogBuilder = AlertDialog.Builder(
                                ContextThemeWrapper(
                                    this,
                                    R.style.Theme_TripPlanner
                                )
                            ).setView(dialog).create()
                            notesCheckListAdapter = NotesCheckListAdapter()
                            notesCheckListAdapter.submitList(list)
                            checkList.adapter = notesCheckListAdapter
                            val layoutParams = WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                LAYOUT_FLAG,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                PixelFormat.TRANSLUCENT
                            )

                            layoutParams.gravity = Gravity.CENTER
                            layoutParams.x = 100
                            layoutParams.y = 1

                            okButton.setOnClickListener {
                                dialogBuilder.dismiss()
                            }

                            windowManager.addView(dialog, layoutParams)

                        }
                        //showMultiChoiceDialog()



                        showPopupWindow(intent!!, floatingWidgetView)

                    }
                    draggingBubble = false
                    true
                }
                else ->
                    false

            }
        }
        return START_STICKY


    }

    override fun onCreate() {
        super.onCreate()
    }

    fun showPopupWindow(intent: Intent, float:View) {
        popupWindow.contentView = popupView
        val okButton = popupView.findViewById<Button>(R.id.btn_ok)
        val entity = intent?.getParcelableExtra<TripEntity>("tripEntity")
        val list = entity?.note?.split(",*herewecansplitit")
        notesCheckListAdapter.submitList(list)
        okButton.setOnClickListener {
            popupWindow.dismiss()
//                                windowManager.removeView(view)
//                                stopSelf()
        }
//                            val layoutParams = WindowManager.LayoutParams(
//                                WindowManager.LayoutParams.MATCH_PARENT,
//                                WindowManager.LayoutParams.WRAP_CONTENT,
//                                LAYOUT_FLAG,
//                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                                PixelFormat.TRANSLUCENT
//                            )
        popupWindow.width = WindowManager.LayoutParams.MATCH_PARENT
        popupWindow.height = WindowManager.LayoutParams.WRAP_CONTENT
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//                            layoutParams.x = 100
//                            layoutParams.y = 0
        popupWindow.showAsDropDown(float)
        //windowManager.addView(view, layoutParams)


    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onDestroy() {
        super.onDestroy()
        if (floatingWidgetView != null) {
            windowManager.removeView(floatingWidgetView)
        }
        if (closeBubbleImage != null) {
            windowManager.removeView(closeBubbleImage)
        }
    }
}

