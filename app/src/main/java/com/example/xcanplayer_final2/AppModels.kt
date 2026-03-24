package com.example.xcanplayer_final2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FondantDefaults {
    const val QT_URL = "https://www.fondant.kr"
    const val BIBLE_URL = "https://www.fondant.kr/series/00090228-5db3-dc44-3c29-52bcaf0002ce?category=episode"
    const val MAIN_SERIES_URL = "https://www.fondant.kr"
}

data class ScheduleItem(val hour: Int, val minute: Int, val title: String, val url: String)

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("SchedulerPrefs_v6", Context.MODE_PRIVATE)

    fun loadSchedule(): MutableList<ScheduleItem> {
        val list = mutableListOf<ScheduleItem>()
        list.add(ScheduleItem(hour = 7, minute = 0, title = "성경통독", url = FondantDefaults.BIBLE_URL))
        saveSchedule(list)
        return list
    }

    fun saveSchedule(list: List<ScheduleItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("h", item.hour); put("m", item.minute); put("title", item.title); put("url", item.url)
            }
            array.put(obj)
        }
        prefs.edit().putString("schedule_json", array.toString()).apply()
    }
}