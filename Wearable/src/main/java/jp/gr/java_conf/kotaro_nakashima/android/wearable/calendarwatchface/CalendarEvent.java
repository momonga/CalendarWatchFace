/**
 * Copyright (C) 2015 Kotaro Nakashima
 */
package jp.gr.java_conf.kotaro_nakashima.android.wearable.calendarwatchface;

import java.lang.String;

/**
 * カレンダーの予定を管理するクラス。
 *
 * @author Kotaro Nakashima
 * @since 1.0.0.0
 */
public final class CalendarEvent {

    /** 予定のタイトル */
    private String title;

    /**
     * 予定のタイトルを取得する。
     *
     * @return 予定のタイトル
     */
    public String getTitle() {
        return title;
    }

    /**
     * 予定のタイトルを設定する。
     *
     * @param title 予定のタイトル
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    //----------------------------------------------------------------------------------------------

    /** 予定の開始時刻 */
    private long dtStart;

    /**
     * 予定の開始時刻を取得する。
     *
     * @return 予定の開始時刻
     */
    public long getDtStart() {
        return dtStart;
    }

    /**
     * 予定の開始時刻を設定する
     *
     * @param dtStart 予定の開始時刻
     */
    public void setDtStart(final long dtStart) {
        this.dtStart = dtStart;
    }

    //----------------------------------------------------------------------------------------------

    /** 予定の終了時刻 */
    private long dtEnd;

    /**
     * 予定の終了時刻を取得する。
     *
     * @return 予定の終了時刻
     */
    public long getDtEnd() {
        return dtEnd;
    }

    /**
     * 予定の終了時刻を設定する。
     *
     * @param dtEnd 予定の終了時刻
     */
    public void setDtEnd(final long dtEnd) {
        this.dtEnd = dtEnd;
    }

    //----------------------------------------------------------------------------------------------

    /** 終日 */
    private boolean allDay;

    /**
     * 終日を取得する。
     *
     * @return 終日
     */
    public boolean isAllDay() {
        return allDay;
    }

    /**
     * 終日を設定する。
     *
     * @param allDay 終日
     */
    public void setAllDay(final boolean allDay) {
        this.allDay = allDay;
    }

    //----------------------------------------------------------------------------------------------

    /** タイムゾーン */
    private String timeZone;

    /**
     * タイムゾーンを取得する
     *
     * @return タイムゾーン
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * タイムゾーンを設定する。
     *
     * @param timeZone タイムゾーン
     */
    public void setTimeZone(final String timeZone) {
        this.timeZone = timeZone;
    }

    //----------------------------------------------------------------------------------------------

    /** 場所 */
    private String eventLocation;

    /**
     * 場所を取得する。
     *
     * @return 場所
     */
    public String getEventLocation() {
        return eventLocation;
    }

    /**
     * 場所を設定する。
     *
     * @param eventLocation 場所
     */
    public void setEventLocation(final String eventLocation) {
        this.eventLocation = eventLocation;
    }

    //----------------------------------------------------------------------------------------------

    /** カレンダー */
    private int calendarId;

    /**
     * カレンダーを取得する。
     *
     * @return カレンダー
     */
    public int getCalendarId() {
        return calendarId;
    }

    /**
     * カレンダーを設定する。
     *
     * @param calendarId カレンダー
     */
    public void setCalendarId(final int calendarId) {
        this.calendarId = calendarId;
    }

    //----------------------------------------------------------------------------------------------

    /** カレンダーの色 */
    private int calendarColor;

    /**
     * カレンダーの色を取得する。
     *
     * @return カレンダーの色
     */
    public int getCalendarColor() {
        return calendarColor;
    }

    /**
     * カレンダーの色を設定する。
     *
     * @param calendarColor カレンダーの色
     */
    public void setCalendarColor(final int calendarColor) {
        this.calendarColor = calendarColor;
    }

    //----------------------------------------------------------------------------------------------

    /** 説明 */
    private String description;

    /**
     * 説明を取得する。
     *
     * @return 説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 説明を設定する。
     *
     * @param description 説明
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    //----------------------------------------------------------------------------------------------

    /** 繰り返し(RRULE) */
    private String rrule;

    /**
     * 繰り返し(RRULE)を取得する。
     *
     * @return 繰り返し(RRULE)
     */
    public String getRrule() {
        return rrule;
    }

    /**
     * 繰り返し(RRULE)を設定する。
     *
     * @param rrule 繰り返し(RRULE)
     */
    public void setRrule(final String rrule) {
        this.rrule = rrule;
    }

    //----------------------------------------------------------------------------------------------

    /** 繰り返し(RDATE) */
    private String rdate;

    /**
     * 繰り返し(RDATE)を取得する。
     *
     * @return 繰り返し(RDATE)
     */
    public String getRdate() {
        return rdate;
    }

    /**
     * 繰り返し(RDATE)を設定する。
     *
     * @param rdate 繰り返し(RDATE)
     */
    public void setRdate(final String rdate) {
        this.rdate = rdate;
    }

    //----------------------------------------------------------------------------------------------

    /** 繰り返し(DURATION) */
    private String duration;

    /**
     * 繰り返し(DURATION)を取得する。
     *
     * @return 繰り返し(DURATION)
     */
    public String getDuration() {
        return duration;
    }

    /**
     * 繰り返し(DURATION)を設定する。
     *
     * @param duration 繰り返し(DURATION)
     */
    public void setDuration(String duration) {
        this.duration = duration;
    }
}
