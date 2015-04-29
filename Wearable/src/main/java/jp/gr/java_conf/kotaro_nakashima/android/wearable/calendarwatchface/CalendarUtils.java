/**
 * Copyright (C) 2015 Kotaro Nakashima
 */
package jp.gr.java_conf.kotaro_nakashima.android.wearable.calendarwatchface;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * カレンダー用のユーティリティークラス
 *
 * @author Kotaro Nakashima
 * @since 1.0.0.0
 */
public final class CalendarUtils {

    /**
     * インスタンス化できないようにするためにデフォルトコンストラクターを抑制する
     */
    private CalendarUtils() {
        throw new AssertionError();
    }

    /**
     * ミリ秒からカレンダー情報を取得する(時、分、ミリ秒は0にリセットする)。
     *
     * @param milliseconds ミリ秒
     */
    public static Calendar getHourCalendarFromMillis(final long milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    /**
     * iCalendar(RFC5545)形式の時刻をミリ秒形式の時刻に変換する。
     *
     * @param iCalendar iCalendar(RFC5545)形式の時刻
     *                  http://tools.ietf.org/html/rfc5545#section-3.8.2.5
     * @return ミリ秒形式の時刻
     */
    public static long convertICalendarToMillis(final String iCalendar) {
        // TODO [2015-04-29 09:23]
        // RFC5545の仕様通りに変換アルゴリズムを実装する。
        // 現状、GoogleカレンダーはP7200Sのように、
        // 秒の設定しか入らないようだが、問題が起こらないように実装する必要がある。
        // (正規表現でのチェック等も必要。)

        String iCal = iCalendar.replace("P", "").replace("S", "");
        return TimeUnit.SECONDS.toMillis(Integer.parseInt(iCal));
    }
}
