/**
 * Copyright (C) 2015 Kotaro Nakashima
 */
package jp.gr.java_conf.kotaro_nakashima.android.wearable.calendarwatchface;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Sample analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in
 * ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p/>
 *
 * @author Kotaro Nakashima
 * @since 1.0.0.0
 */
public class CalendarWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "WatchFaceService";

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;

        /** 短針の描画スタイル */
        private Paint mHourPaint;

        /** 長針の描画スタイル */
        private Paint mMinutePaint;

        /** 秒針の描画スタイル */
        private Paint mSecondPaint;

        /** 目盛り(小)の描画スタイル */
        private Paint mTickPaintSmall;

        /** 目盛り(大)の描画スタイル */
        private Paint mTickPaintBig;

        private boolean mMute;

        /** 現在時刻 */
        private Time mTime;

        /** ウォッチフェイスの幅 */
        private int mWatchFaceWidth = 0;

        /** ウォッチフェイスの高さ */
        private int mWatchFaceHeight = 0;

        /** ウォッチフェイスの中心(X軸の座標) */
        private float mCenterX = 0;

        /** ウォッチフェイスの中心(Y軸の座標) */
        private float mCenterY = 0;

        /** バッテリーの残量 */
        private int mBatteryLevel = 0;// バッテリー残量

        /** バッテリーの最大値 */
        private int mBatteryScale = 0;// バッテリー最大値

        /** カレンダーの予定 */
        private List<CalendarEvent> mCalendarEvents = new ArrayList<>();

        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        final private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mBatteryLevel = intent.getIntExtra("level", 0); // バッテリーの残量
                mBatteryScale = intent.getIntExtra("scale", 0); // バッテリーの最大値
            }
        };

        boolean mRegisteredBatteryReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        /**
         * ここで、使用する画像を読み込んでおく
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            int gravity = Gravity.TOP | Gravity.CENTER;
            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(CalendarWatchFaceService.this)
                            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                            .setBackgroundVisibility(
                                    WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                            .setShowSystemUiTime(false)
                            .setStatusBarGravity(gravity) // ステータスバーの制御/ ステータスアイコンの位置
                            .setHotwordIndicatorGravity(gravity) // Ok Googleの位置
                            .setViewProtection(WatchFaceStyle.PROTECT_STATUS_BAR
                                    | WatchFaceStyle.PROTECT_HOTWORD_INDICATOR) // 背景の設定
                            .build());

            // 短針の描画スタイルを設定する
            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 0, 0, 0);
            mHourPaint.setStrokeWidth(12.f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            // 長針の描画スタイルを設定する
            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 0, 0, 0);
            mMinutePaint.setStrokeWidth(6.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            // 秒針の描画スタイルを設定する
            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 255, 0, 0);
            mSecondPaint.setStrokeWidth(3.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            // 目盛り(小)の描画スタイルを設定する
            mTickPaintSmall = new Paint();
            mTickPaintSmall.setARGB(100, 0, 0, 0);
            mTickPaintSmall.setStrokeWidth(2.f);
            mTickPaintSmall.setAntiAlias(true);

            // 目盛り(大)の描画スタイルを設定する
            mTickPaintBig = new Paint();
            mTickPaintBig.setARGB(150, 0, 0, 0);
            mTickPaintBig.setStrokeWidth(8.f);
            mTickPaintBig.setAntiAlias(true);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        /**
         * AmbientModeがONの時に、1分ごとに1回呼ばれる。
         * AmbientModeでないときは、呼ばれない。
         */
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        /**
         * 時計にしばらく触れていないとAmbientModeに移行し、省電力モードに入る。
         * AmbientModeのときは、ウォッチフェイスを白黒表示にするのが好ましい。
         */
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mTickPaintSmall.setAntiAlias(antiAlias);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        /**
         * ここでウォッチフェイスの描画を行う
         *
         * @param canvas 描画に利用するキャンバス
         * @param bounds 描画領域
         */
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // 変数を設定する。
            mTime.setToNow();
            mWatchFaceWidth = bounds.width();
            mWatchFaceHeight = bounds.height();
            mCenterX = mWatchFaceWidth / 2f;
            mCenterY = mWatchFaceHeight / 2f;
            if (isInDebugMode) {
                setDummyDataForDebugMode();
            }

            // カレンダーの情報を取得する。
            setCalendarEvents();

            // ウォッチフェイスの描画をする
            // (文字盤の外側から順番に描画していく)
            drawWatchFaceBase(canvas); // 時計の文字盤
            if (!isInAmbientMode()) {
                drawBatteryLevel(canvas); // バッテリー残量(外周)
                drawCalendarEvents(canvas); // カレンダーの予定(外周)
                drawNextCalendarEvent(canvas); // カレンダーの次の予定(中央・上側)
                drawWeather(canvas); // 天気(左側)
                drawCalendar(canvas); // 日付・曜日(右側)
            }
            drawWatchHands(canvas); // 短針・長針・秒針
        }

        /**
         * ウォッチフェイスの表示/非表示に合わせて描画処理を開始/停止する
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                CalendarWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
            }

            // バッテリー残量表示用の処理
            if (!mRegisteredBatteryReceiver) {
                mRegisteredBatteryReceiver = true;
                IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                CalendarWatchFaceService.this.registerReceiver(mBatteryReceiver, batteryFilter);
            }
        }

        private void unregisterReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = false;
                CalendarWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
            }

            // バッテリー残量表示用の処理
            if (mRegisteredBatteryReceiver) {
                mRegisteredBatteryReceiver = false;
                CalendarWatchFaceService.this.unregisterReceiver(mBatteryReceiver);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        //------------------------------------------------------------------------------------------

        /**
         * カレンダーの予定を設定する。
         */
        private void setCalendarEvents() {
            // デバッグモードの場合、テスト用ダミーデータを設定して終了する
            if (isInDebugMode) {
                setDummyCalendarEventsForDebugMode();
                return;
            }

            // カレンダープロバイダーを設定する
            final long startTime = System.currentTimeMillis();
            Uri.Builder builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startTime);
            ContentUris.appendId(builder, startTime + TimeUnit.HOURS.toMillis(12));
            Cursor cursor = getContentResolver().query(builder.build(), null, null, null, null);

            // カレンダープロバイダーからカレンダーの予定を取得する
            mCalendarEvents.clear();
            while (cursor.moveToNext()) {
                mCalendarEvents.add(getCalendarEvent(cursor, startTime));
            }
            Collections.sort(mCalendarEvents, new CalendarEventComparator());
        }

        /**
         * カレンダーの予定をカーソルから取得する。
         *
         * @param cursor            カーソル
         * @param currentTimeMillis 現在時刻
         * @return カレンダーの予定
         */
        private CalendarEvent getCalendarEvent(final Cursor cursor, final long currentTimeMillis) {
            // カレンダーの予定をカーソルから取得する
            CalendarEvent event = new CalendarEvent();
            event.setTitle(cursor.getString(
                    cursor.getColumnIndex(CalendarContract.Events.TITLE))); // 予定のタイトル
            event.setDtStart(
                    cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART))); // 開始時刻
            event.setDtEnd(
                    cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTEND))); // 終了時刻
            event.setAllDay(
                    cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)) == 1
                            ? true : false); // 終日
            event.setTimeZone(cursor.getString(
                    cursor.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE))); // タイムゾーン
            event.setEventLocation(cursor.getString(
                    cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION))); // 場所
            event.setCalendarId(cursor.getInt(
                    cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID))); // カレンダー
            event.setCalendarColor(cursor.getInt(
                    cursor.getColumnIndex(CalendarContract.Events.CALENDAR_COLOR))); // カレンダーの色
            event.setDescription(cursor.getString(
                    cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION))); // 説明
            event.setRrule(cursor.getString(
                    cursor.getColumnIndex(CalendarContract.Events.RRULE))); // 繰り返し(RRULE)
            event.setRdate(cursor.getString(
                    cursor.getColumnIndex(CalendarContract.Events.RDATE))); // 繰り返し(RDATE)
            event.setDuration(cursor.getString(
                    cursor.getColumnIndex(CalendarContract.Events.DURATION))); // 繰り返し(DURATION)

            // カレンダーの予定を調整する
            Calendar currentHour = CalendarUtils.getHourCalendarFromMillis(currentTimeMillis);
            setRepetitiveEvent(event, currentHour);
            setOvertimeEvent(event, currentHour);

            return event;
        }

        /**
         * 繰り返しの予定の開始時刻、終了時刻を設定する。
         *
         * 繰り返しの予定の場合、DTSTARTには繰り返しの最初の開始時刻、DTENDには0が設定されている。
         * 開始時刻、終了時刻の情報を取得するには、DTSTART、DTENDの値ではなく、
         * RRULE、RDATE、DURATIONに設定されている情報を基に計算する必要がある。
         *
         * @param event       カレンダーの予定
         * @param currentTime 現在時刻
         */
        private void setRepetitiveEvent(final CalendarEvent event, final Calendar currentTime) {
            if ((event.getRrule() != null) && !event.getRrule().isEmpty()) {
                // 開始時刻を設定する
                Calendar startTime = Calendar.getInstance();
                startTime.setTimeInMillis(event.getDtStart());
                startTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
                startTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
                startTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));
                event.setDtStart(startTime.getTimeInMillis());

                // 終了時刻を設定する
                Calendar endTime = Calendar.getInstance();
                endTime.setTimeInMillis(event.getDtStart());
                endTime.add(Calendar.MILLISECOND,
                        (int) CalendarUtils.convertICalendarToMillis(event.getDuration()));
                event.setDtEnd(endTime.getTimeInMillis());
            }
        }

        /**
         * 開始時間が現在時刻(時)より前の場合、開始時間を短縮する。
         * また、終了時間が現在時刻の12時間以降になる場合、終了時間を短縮する。
         *
         * @param event       カレンダーの予定
         * @param currentHour 現在時刻(時)
         */
        private void setOvertimeEvent(final CalendarEvent event, final Calendar currentHour) {
            // 開始時刻を設定する。
            final Calendar startHour = CalendarUtils.getHourCalendarFromMillis(event.getDtStart());
            final long startTimeDiff = startHour.getTimeInMillis() - currentHour.getTimeInMillis();
            if (TimeUnit.MILLISECONDS.toHours(startTimeDiff) <= -1) {
                startHour.set(Calendar.YEAR, currentHour.get(Calendar.YEAR));
                startHour.set(Calendar.MONTH, currentHour.get(Calendar.MONTH));
                startHour.set(Calendar.DAY_OF_MONTH, currentHour.get(Calendar.DAY_OF_MONTH));
                startHour.set(Calendar.HOUR_OF_DAY, currentHour.get(Calendar.HOUR_OF_DAY));
                event.setDtStart(startHour.getTimeInMillis());
            }

            // 終了時刻を設定する。
            final Calendar endHour = CalendarUtils.getHourCalendarFromMillis(event.getDtEnd());
            final long endTimeDiff = endHour.getTimeInMillis() - currentHour.getTimeInMillis();
            if (TimeUnit.MILLISECONDS.toHours(endTimeDiff) >= 12) {
                endHour.set(Calendar.YEAR, currentHour.get(Calendar.YEAR));
                endHour.set(Calendar.MONTH, currentHour.get(Calendar.MONTH));
                endHour.set(Calendar.DAY_OF_MONTH, currentHour.get(Calendar.DAY_OF_MONTH));
                endHour.set(Calendar.HOUR_OF_DAY, currentHour.get(Calendar.HOUR_OF_DAY) + 12);
                event.setDtEnd(endHour.getTimeInMillis());
            }
        }

        //------------------------------------------------------------------------------------------

        /**
         * ウォッチフェイスの背景、目盛りを描画する。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawWatchFaceBase(final Canvas canvas) {
            // 背景を描画する
            canvas.drawColor(!isInAmbientMode() ? Color.WHITE : Color.BLACK);

            // 目盛りの色を設定する
            final int tickColor = !isInAmbientMode() ? Color.BLACK : Color.GRAY;
            mTickPaintSmall.setColor(tickColor);
            mTickPaintBig.setColor(tickColor);

            // 目盛りを描画する
            for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                float innerTickRadius = mCenterX - 20;
                float outerTickRadius = mCenterX - 5;
                Paint paint = mTickPaintSmall;
                if ((tickIndex % 5) == 0) {
                    innerTickRadius = mCenterX - 35;
                    outerTickRadius = mCenterX - 5;
                    paint = mTickPaintBig;
                }

                final float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                canvas.drawLine(
                        mCenterX + (float) Math.sin(tickRot) * innerTickRadius,
                        mCenterY + (float) -Math.cos(tickRot) * innerTickRadius,
                        mCenterX + (float) Math.sin(tickRot) * outerTickRadius,
                        mCenterY + (float) -Math.cos(tickRot) * outerTickRadius,
                        paint);
            }

            // Moto 360用の描画をする
            if (isInMoto360Mode) {
                drawMoto360ForDebugMode(canvas);
            }
        }

        /**
         * バッテリーを描画する。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawBatteryLevel(final Canvas canvas) {
            // 描画スタイルを設定する
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLUE);
            paint.setAlpha(100);
            paint.setStrokeWidth(5.f);
            paint.setStrokeCap(Paint.Cap.ROUND);

            // 線を描画する
            final float width = mWatchFaceWidth - 35;
            final float height = mWatchFaceHeight - 35;
            final float left = (mWatchFaceWidth - width) / 2;
            final float top = (mWatchFaceHeight - height) / 2;
            final RectF oval = new RectF(left, top, left + width, top + height);
            final float startAngle = -90;
            final float sweepAngle = ((float) mBatteryLevel / mBatteryScale) * 360;
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(oval, startAngle, sweepAngle, false, paint);

            // 先端を描画する
            float rot = (float) ((mBatteryLevel / (mBatteryScale / 2f)) * Math.PI);
            float cx = mCenterX + ((float) Math.sin(rot) * (width / 2f));
            float cy = mCenterY + ((float) -Math.cos(rot) * (width / 2f));
            float radius = 6;
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawCircle(cx, cy, radius, paint);
        }

        /**
         * カレンダーの予定を描画する。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawCalendarEvents(final Canvas canvas) {
            for (CalendarEvent event : mCalendarEvents) {
                // 終日の予定はウォッチフェイスに描画しない
                if (event.isAllDay()) {
                    continue;
                }

                drawCalendarEvent(canvas, event);
            }
            drawCalendarEventsFrame(canvas);
        }

        /**
         * カレンダーの予定を描画する。
         *
         * @param canvas 描画に利用するキャンバス
         * @param event  カレンダーの予定
         */
        private void drawCalendarEvent(final Canvas canvas, final CalendarEvent event) {
            // カレンダーの予定の枠(円弧)の角度を設定する
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(event.getDtStart());
            final int startDay = calendar.get(Calendar.DAY_OF_MONTH);
            final int startHour = calendar.get(Calendar.HOUR_OF_DAY);
            final int startMinute = calendar.get(Calendar.MINUTE);
            calendar.setTimeInMillis(event.getDtEnd());
            final int endDay = calendar.get(Calendar.DAY_OF_MONTH);
            final int endHour = (startDay == endDay) ? calendar.get(Calendar.HOUR_OF_DAY)
                    : calendar.get(Calendar.HOUR_OF_DAY) + (24 * (endDay - startDay));
            final int endMinutes = calendar.get(Calendar.MINUTE);
            final int topAngle = -90;
            final int startAngle = topAngle + (int) ((startHour + (startMinute / 60f)) / 12f * 360);
            final int endAngle = topAngle + (int) ((endHour + (endMinutes / 60f)) / 12f * 360);
            final int sweepAngle = endAngle - startAngle;

            // カレンダーの予定の枠(上側)を描画する
            Paint upperSidePaint = new Paint();
            upperSidePaint.setAntiAlias(true);
            upperSidePaint.setColor(event.getCalendarColor());
            upperSidePaint.setStyle(Paint.Style.FILL);
            final float upperSideWidth = mWatchFaceWidth - 70;
            final float upperSideLeftTop = (mWatchFaceWidth - upperSideWidth) / 2;
            final float upperSideRightBottom = upperSideLeftTop + upperSideWidth;
            RectF upperSideOval = new RectF(upperSideLeftTop, upperSideLeftTop,
                    upperSideRightBottom, upperSideRightBottom);
            canvas.drawArc(upperSideOval, startAngle, sweepAngle, true, upperSidePaint);

            // カレンダーの予定の枠(下側)を描画する
            Paint lowerSidePaint = new Paint();
            lowerSidePaint.setAntiAlias(true);
            lowerSidePaint.setColor(Color.WHITE);
            lowerSidePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            lowerSidePaint.setStrokeWidth(1.f);
            final float lowerSideWidth = mWatchFaceWidth - 100;
            final float lowerSideLeftTop = (mWatchFaceWidth - lowerSideWidth) / 2;
            final float lowerSideRightBottom = lowerSideLeftTop + lowerSideWidth;
            RectF lowerSideOval = new RectF(lowerSideLeftTop, lowerSideLeftTop,
                    lowerSideRightBottom, lowerSideRightBottom);
            canvas.drawArc(lowerSideOval, startAngle, sweepAngle, true, lowerSidePaint);

            // カレンダーの予定の枠(左側・右側)を描画する
            Paint sidePaint = new Paint();
            sidePaint.setAntiAlias(true);
            sidePaint.setColor(Color.LTGRAY);
            sidePaint.setStyle(Paint.Style.STROKE);
            sidePaint.setStrokeWidth(2.f);
            float startTimeRot = (float) ((startHour + (startMinute / 60f)) / 6f * Math.PI);
            final float innerRadius = lowerSideWidth / 2;
            final float outerRadius = upperSideWidth / 2;
            canvas.drawLine(
                    mCenterX + (float) Math.sin(startTimeRot) * innerRadius,
                    mCenterY + (float) -Math.cos(startTimeRot) * innerRadius,
                    mCenterX + (float) Math.sin(startTimeRot) * outerRadius,
                    mCenterY + (float) -Math.cos(startTimeRot) * outerRadius,
                    sidePaint);
            final float endTimeRot = (float) ((endHour + (endMinutes / 60f)) / 6f * Math.PI);
            canvas.drawLine(
                    mCenterX + (float) Math.sin(endTimeRot) * innerRadius,
                    mCenterY + (float) -Math.cos(endTimeRot) * innerRadius,
                    mCenterX + (float) Math.sin(endTimeRot) * outerRadius,
                    mCenterY + (float) -Math.cos(endTimeRot) * outerRadius,
                    sidePaint);

            // カレンダーの予定のタイトルを描画する
            Paint titlePaint = new Paint();
            titlePaint.setAntiAlias(true);
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(11);
            titlePaint.setTextAlign(Paint.Align.CENTER);
            Path titlePath = new Path();
            titlePath.addArc(upperSideOval, startAngle, sweepAngle);
            canvas.drawTextOnPath(event.getTitle(), titlePath, 0, 12, titlePaint);
        }

        /**
         * カレンダーの予定の枠を描画する。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawCalendarEventsFrame(final Canvas canvas) {
            // 描画スタイルを設定する
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.LTGRAY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.f);

            // カレンダーの予定の枠(円)を描画する
            canvas.drawCircle(mCenterX, mCenterY, mCenterX - 35, paint);
            canvas.drawCircle(mCenterX, mCenterY, mCenterX - 50, paint);
        }

        /**
         * カレンダーの次の予定を描画する。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawNextCalendarEvent(final Canvas canvas) {
            // カレンダーの次の予定を取得する
            CalendarEvent event = null;
            for (CalendarEvent e : mCalendarEvents) {
                // 終日の予定はウォッチフェイスに描画しない
                if (e.isAllDay()) {
                    continue;
                }

                event = e;
            }

            // カレンダーの次の予定を描画する。
            if (event != null) {
                // 描画スタイルを設定する
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(Color.GRAY);
                paint.setStyle(Paint.Style.FILL);

                Calendar startTime = Calendar.getInstance();
                Calendar endTime = Calendar.getInstance();
                Calendar currentTime = Calendar.getInstance();
                startTime.setTimeInMillis(event.getDtStart());
                endTime.setTimeInMillis(event.getDtEnd());
                if (isInDebugMode) {
                    currentTime.set(mTime.year, mTime.month, mTime.monthDay, mTime.hour,
                            mTime.minute, mTime.second);
                }

                if (Locale.getDefault().equals(Locale.JAPAN)) {
                    // 日本の場合(日本語)
                    drawNextEventMessage(canvas, event, startTime, currentTime, paint, true);
                    drawNextEventTime(canvas, startTime, endTime, currentTime, paint, false);
                } else {
                    // 日本以外の場合(英語)
                    drawNextEventTime(canvas, startTime, endTime, currentTime, paint, true);
                    drawNextEventMessage(canvas, event, startTime, currentTime, paint, false);
                }
            }
        }

        /**
         * カレンダーの次の予定のメッセージを描画する。
         *
         * @param canvas      描画に利用するキャンバス
         * @param event       カレンダーの次の予定
         * @param startTime   カレンダーの次の予定の開始時刻
         * @param currentTime 現在時刻
         * @param paint       描画スタイル
         * @param isUpperSide 上段に描画するかどうか
         */
        private void drawNextEventMessage(final Canvas canvas, final CalendarEvent event,
                final Calendar startTime, final Calendar currentTime, final Paint paint,
                final boolean isUpperSide) {
            // メッセージを設定する
            String text;
            if (currentTime.before(startTime)) {
                text = getString(R.string.next_schedule_title_start, event.getTitle());
            } else {
                text = getString(R.string.next_schedule_title_end, event.getTitle());
            }

            // メッセージの座標を設定する
            final int height = (int) (14 * 1.5f);
            int x = (int) mCenterX;
            int y = isUpperSide ? (int) mCenterY - 70 : (int) mCenterY - 70 + height;

            // メッセージの描画スタイルを設定する
            paint.setTextSize(12);
            paint.setTextAlign(Paint.Align.CENTER);

            // メッセージを描画する
            canvas.drawText(text, x, y, paint);
        }

        /**
         * カレンダーの次の予定の残り時間を描画する。
         *
         * @param canvas      描画に利用するキャンバス
         * @param startTime   カレンダーの次の予定の開始時刻
         * @param endTime     カレンダーの次の予定の終了時刻
         * @param currentTime 現在時刻
         * @param paint       描画スタイル
         * @param isUpperSide 上段に描画するかどうか
         */
        private void drawNextEventTime(final Canvas canvas, final Calendar startTime,
                final Calendar endTime, final Calendar currentTime, final Paint paint,
                final boolean isUpperSide) {
            // 残り時間を設定する
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis((currentTime.before(startTime) ? startTime.getTimeInMillis()
                    : endTime.getTimeInMillis()) - currentTime.getTimeInMillis());

            // 残り時間の描画スタイルを設定する
            paint.setTextSize(18);
            paint.setTextAlign(Paint.Align.CENTER);

            // 残り時間を描画する
            String text = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(time.getTimeInMillis()),
                    time.get(Calendar.MINUTE) == 59 ? 0 : time.get(Calendar.MINUTE) + 1,
                    time.get(Calendar.SECOND) == 59 ? 0 : time.get(Calendar.SECOND) + 1);
            final float height = (int) (14 * 1.5f);
            final int x = (int) mCenterX;
            final int y = isUpperSide ? (int) mCenterX - 70 : (int) (mCenterX - 70 + height);
            canvas.drawText(text, x, y, paint);
        }

        /**
         * 天気を描画する。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawWeather(final Canvas canvas) {
            // TODO: [2015-04-29 0:46] 天気を描画する処理を実装する。
        }

        /**
         * 日付・曜日を描画する。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawCalendar(final Canvas canvas) {
            // 日付・曜日の枠を描画する
            Paint paint = new Paint();
            paint.setAntiAlias(true); // グラフィックの描画を滑らかにする
            paint.setStyle(Paint.Style.STROKE); // 線のスタイル
            paint.setStrokeWidth(1.f); // 線の太さ
            paint.setColor(Color.LTGRAY); // 色の設定
            final int dowWidth = 40; // 曜日の枠の幅
            final int dateWidth = 30; // 日付の枠の幅
            final int height = 20; // 日付・曜日の枠の高さ
            final int dowX = mWatchFaceWidth - dowWidth - 85; // 曜日のX軸の座標
            final int dateX = dowX + dowWidth; // 日付のX軸の座標
            final int y = (int) (mCenterY - (height / 2f)); // 日付・曜日のY軸の座標
            canvas.drawRect(dowX, y, dowX + dowWidth, y + height, paint);
            canvas.drawRect(dateX, y, dateX + dateWidth, y + height, paint);
            paint.setColor(Color.DKGRAY);
            paint.setStrokeWidth(1.0f);
            canvas.drawRect(dowX, y, dowX + dowWidth + dateWidth, y + height, paint); // 外枠

            // 日付・曜日の文字列を描画する
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("E");
            final String dow = sdf.format(calendar.getTime());
            sdf = new SimpleDateFormat("dd");
            final String date = sdf.format(calendar.getTime());
            final int textSize = 16;
            paint.setTextSize(textSize);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(1.0f);
            paint.setTypeface(Typeface.SERIF);
            canvas.drawText(date, dateX + (dateWidth / 2), y + textSize, paint); // 日付の描画
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                paint.setColor(Color.BLUE);
            } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                paint.setColor(Color.RED);
            }
            canvas.drawText(dow, dowX + (dowWidth / 2), y + textSize, paint); // 曜日の描画
        }

        /**
         * ウォッチフェイスの短針、長針、秒針を描画する
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawWatchHands(final Canvas canvas) {
            // 針の色を設定する
            final int handsColor = !isInAmbientMode() ? Color.BLACK : Color.GRAY;
            mHourPaint.setColor(handsColor);
            mMinutePaint.setColor(handsColor);

            final int minutes = mTime.minute;

            // 短針を描画する
            final float hrRot = (mTime.hour + (minutes / 60f)) / 6f * (float) Math.PI;
            final float hrLength = mCenterX - 58;
            final float hrX = (float) Math.sin(hrRot) * hrLength;
            final float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(mCenterX, mCenterY, mCenterX + hrX, mCenterY + hrY, mHourPaint);

            // 長針を描画する
            final float minRot = minutes / 30f * (float) Math.PI;
            final float minLength = mCenterX - 5;
            final float minX = (float) Math.sin(minRot) * minLength;
            final float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(mCenterX, mCenterY, mCenterX + minX, mCenterY + minY, mMinutePaint);

            // 秒針を描画する
            final float secRot = mTime.second / 30f * (float) Math.PI;
            final float secLength = mCenterX - 42;
            if (!isInAmbientMode()) {
                // 秒針を描画する
                final float secX = (float) Math.sin(secRot) * secLength;
                final float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(mCenterX, mCenterY, mCenterX + secX, mCenterY + secY, mSecondPaint);

                // 秒針の先端部分を描画する
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(Color.RED);
                paint.setStrokeWidth(1.f);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(mCenterX + secX, mCenterY + secY, 8, paint);
            }
        }

        //------------------------------------------------------------------------------------------

        /** デバッグモードかどうか */
        private boolean isInDebugMode = false; //

        /** Moto 360モードかどうか */
        private boolean isInMoto360Mode = false;

        /**
         * デバッグモード用のダミーデータを設定する。
         */
        private void setDummyDataForDebugMode() {
            mTime.set(36, 8, 10, 27, 4, 2015);
            mBatteryLevel = 29;
        }

        /**
         * Moto 360用の下側にある環境光センサーの黒い帯をエミュレートする。
         *
         * @param canvas 描画に利用するキャンバス
         */
        private void drawMoto360ForDebugMode(final Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            canvas.drawRect(0, 290, 320, 320, paint);
        }

        /**
         * デバッグ用のダミーのカレンダーの予定を取得する。
         */
        private void setDummyCalendarEventsForDebugMode() {
            mCalendarEvents.clear();

            Calendar calendar = Calendar.getInstance();

            // 1番目の予定
            CalendarEvent event1 = new CalendarEvent();
            event1.setTitle("打ち合わせ");
            //event1.setTitle("Meeting");
            calendar.set(2015, 4, 27, 11, 0, 0);
            event1.setDtStart(calendar.getTimeInMillis());
            calendar.set(2015, 4, 27, 12, 0, 0);
            event1.setDtEnd(calendar.getTimeInMillis());
            event1.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo").getID());
            event1.setCalendarColor(Color.argb(0xff, 0x42, 0xd6, 0x92)); // 仕事
            mCalendarEvents.add(event1);

            // 2番目の予定
            CalendarEvent event2 = new CalendarEvent();
            event2.setTitle("A社へ訪問");
            //event2.setTitle("Visit A");
            calendar.set(2015, 4, 27, 13, 0, 0);
            event2.setDtStart(calendar.getTimeInMillis());
            calendar.set(2015, 4, 27, 16, 0, 0);
            event2.setDtEnd(calendar.getTimeInMillis());
            event2.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo").getID());
            event2.setCalendarColor(Color.argb(0xff, 0x42, 0xd6, 0x92)); // 仕事
            mCalendarEvents.add(event2);

            // 3番目の予定
            CalendarEvent event3 = new CalendarEvent();
            event3.setTitle("Bさんと食事");
            //event3.setTitle("Dinner w/ B");
            calendar.set(2015, 4, 27, 19, 0, 0);
            event3.setDtStart(calendar.getTimeInMillis());
            calendar.set(2015, 4, 27, 21, 0, 0);
            event3.setDtEnd(calendar.getTimeInMillis());
            event3.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo").getID());
            event3.setCalendarColor(Color.argb(0xff, 0x9f, 0xc6, 0xe7)); // プライベート
            mCalendarEvents.add(event3);
        }

        //------------------------------------------------------------------------------------------
    }
}
