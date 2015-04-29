/**
 * Copyright (C) 2015 Kotaro Nakashima
 */
package jp.gr.java_conf.kotaro_nakashima.android.wearable.calendarwatchface;

import java.util.Comparator;

/**
 * CalendarEventクラスのソート用クラス
 *
 * @author Kotaro Nakashima
 * @since 1.0.0.0
 */
public class CalendarEventComparator implements Comparator<CalendarEvent> {

    @Override
    public int compare(CalendarEvent obj1, CalendarEvent obj2) {
        Long a = obj1.getDtStart();
        Long b = obj2.getDtStart();
        if ((a == null) || (b == null)) {
            return 0;
        }
        return -a.compareTo(b);
    }
}

