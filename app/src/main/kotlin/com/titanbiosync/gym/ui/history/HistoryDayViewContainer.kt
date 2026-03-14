package com.titanbiosync.gym.ui.history

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendar.view.ViewContainer
import com.titanbiosync.R

class HistoryDayViewContainer(view: View) : ViewContainer(view) {
    val root: View = view
    val dayText: TextView = view.findViewById(R.id.dayText)
    val dot: View = view.findViewById(R.id.dayDot)
}