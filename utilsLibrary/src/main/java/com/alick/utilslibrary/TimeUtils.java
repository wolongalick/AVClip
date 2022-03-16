package com.alick.utilslibrary;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间工具类
 */
@SuppressLint("SimpleDateFormat")
public class TimeUtils {
    public static final String format1  = "yyyy-MM-dd HH:mm:ss";
    public static final String format2  = "yyyy-MM-dd";
    public static final String format3  = "yy-MM-dd";
    public static final String format4  = "yyyy-MM-dd HH:mm";
    public static final String format5  = "yyyy-MM";
    public static final String format6  = "yyyy/MM/dd";
    public static final String format7  = "yyyy-MM-dd HH:mm:ss:ms";
    public static final String format8  = "hh:mm";
    public static final String format9  = "yyyy年MM月dd日 HH:mm:ss";
    public static final String format10 = "h:mm";
    public static final String format11 = "yyyy.MM.dd HH:mm";
    public static final String format12 = "HH:mm";
    public static final String format13 = "H:mm:ss";
    public static final String format14 = "yyyy-MM-dd HH：mm：ss";


    private static final long ONE_WEEK   = 7 * 24 * 60 * 60 * 1000;
    private static final long ONE_DAY    = 24 * 60 * 60 * 1000;
    private static final long ONE_HOUR   = 60 * 60 * 1000;
    private static final long ONE_MINUTE = 60 * 1000;

    /**
     * 根据时间戳转换字符串时间格式
     *
     * @param time
     * @return
     */
    public static String parseLongToString(long time, String formater) {
        SimpleDateFormat format = new SimpleDateFormat(formater);
        return format.format(new Date(time));
    }

    /**
     * 根据时间戳转换字符串时间格式
     *
     * @param time
     * @return
     */
    public static String parseLongToString(long time) {
        SimpleDateFormat format = new SimpleDateFormat(format1);
        return format.format(new Date(time));
    }


    /**
     * 根据字符串时间格式转换系统时间戳
     *
     * @param strTime
     * @return modify by zhang
     */
    public static long parseStringToMillis(String strTime, String formatStr) {

        SimpleDateFormat format = new SimpleDateFormat(formatStr);
        try {
            if (strTime != null) {
                Date d    = format.parse(strTime);
                return d.getTime();
            } else {
                return System.currentTimeMillis();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 根据字符串时间格式转换系统时间戳
     *
     * @param strTime
     * @return modify by zhang
     */
    public static long parseStringToMillis2(long strTime, String formatStr) {
        SimpleDateFormat format = new SimpleDateFormat(formatStr);
        try {
            if (strTime != 0) {
                String d    = format.format(strTime);
                Date   data = format.parse(d);
                long   time = data.getTime();
                return time;
            } else {
                return System.currentTimeMillis();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Date parseStringToDate(String strTime, String formatStr) {
        if (TextUtils.isEmpty(strTime)) {
            return new Date();
        }
        SimpleDateFormat format = new SimpleDateFormat(formatStr);
        try {
            return format.parse(strTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }


    /**
     * String格式的时间转换成另一种String格式的时间
     *
     * @param strTime
     * @param befoerFormat
     * @param afterFormat
     * @return
     */
    public static String parseStringToString(String strTime, String befoerFormat, String afterFormat) {
        if (TextUtils.isEmpty(strTime)) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(befoerFormat);
        try {
            return parseLongToString(format.parse(strTime).getTime(), afterFormat);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 获取聊天时间
     *
     * @param timesamp
     * @return
     */
    public static String getChatTime(long timesamp) {
        String           result   = "";
        SimpleDateFormat sdf      = new SimpleDateFormat("dd");
        Date             today    = new Date(System.currentTimeMillis());
        Date             otherDay = new Date(timesamp);
        int temp = Integer.parseInt(sdf.format(today))
                - Integer.parseInt(sdf.format(otherDay));

        switch (temp) {
            case 0:
                result = parseLongToString(timesamp, "HH:mm");
                break;
            case 1:
                result = "昨天 " + parseLongToString(timesamp, "HH:mm");
                break;
            case 2:
                result = "前天 " + parseLongToString(timesamp, "HH:mm");
                break;

            default:
                // result = temp + "天前 ";
                result = parseLongToString(timesamp, "yyyy-MM-dd HH:mm");
                break;
        }

        return result;
    }


    public static DateModel parseLongToCalendar(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return new DateModel(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static class DateModel {
        public int year;
        public int month;
        public int day;

        public DateModel() {
        }

        public DateModel(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public static String getCurrentTime() {
        return parseLongToString(System.currentTimeMillis(), format1);
    }

    /**
     * 获取当前时间
     *
     * @param format 时间格式
     * @return
     */
    public static String getCurrentTime(String format) {
        return parseLongToString(System.currentTimeMillis(), format);
    }


    public static boolean isOverRange(long second, String beginTime, String endTime) {
        return isOverRange(second, parseStringToMillis(beginTime, format1), parseStringToMillis(endTime, format1));
    }

    public static boolean isOverRange(long second, String beginTime, String endTime, String format) {
        return isOverRange(second, parseStringToMillis(beginTime, format), parseStringToMillis(endTime, format1));
    }

    public static boolean isOverRange(long second, long beginTime, long endTime) {
        return endTime - beginTime > second;
    }

    public static int getYearLength(String beginTime, String endTime, String format) {
        if (TextUtils.isEmpty(beginTime) || TextUtils.isEmpty(endTime)) {
            return -1;
        }

        long beginLongTime = parseStringToMillis(beginTime, format);
        long endLongTime   = parseStringToMillis(endTime, format);

        Calendar beginCalendar = Calendar.getInstance();
        beginCalendar.setTimeInMillis(beginLongTime);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(endLongTime);

        int length = endCalendar.get(Calendar.YEAR) - beginCalendar.get(Calendar.YEAR);

        return length > -1 ? length : -1;
    }

    public static String parseChatTime(String strTime) {
        return getRelativeTime2(parseStringToMillis(strTime, format1));
    }

    /**
     * 解析聊天时间<br/>
     * 1.不同年,或同年但不同月,或同年同月但日相差大于1天的,就显示具体时间,例如:2015年5月10日 上午 8:7<br/>
     * 2.同年同月作日,显示:昨天上午 8:7<br/>
     * 3.同年同月同日,显示:今天下午 6:7
     *
     * @param time
     * @return
     */
    public static String parseChatTime(long time) {
        Calendar otherCalendar = Calendar.getInstance();
        otherCalendar.setTimeInMillis(time);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        if (otherCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR)
                || (otherCalendar.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH))
                || (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) > 1)) {

            return otherCalendar.get(Calendar.YEAR) + "年" + (otherCalendar.get(Calendar.MONTH) + 1) + "月"
                    + otherCalendar.get(Calendar.DAY_OF_MONTH) + "日 "
                    + (otherCalendar.get(Calendar.AM_PM) == Calendar.AM ? "上午" : "下午") + " "
                    + parseLongToString(time, format8);
        }

        return (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) == 1 ? "昨天" : "")
                + (otherCalendar.get(Calendar.AM_PM) == Calendar.AM ? "上午" : "下午") + " "
                + parseLongToString(time, format8);
    }


    public static String parseLastMsgTime(long time) {
        Calendar otherCalendar = Calendar.getInstance();
        otherCalendar.setTimeInMillis(time);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        if (otherCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR)
                || (otherCalendar.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH))
                || (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) > 1)) {

            return String.valueOf(otherCalendar.get(Calendar.YEAR)).substring(2) + "/" + (otherCalendar.get(Calendar.MONTH) + 1) + "/"
                    + otherCalendar.get(Calendar.DAY_OF_MONTH);
        }

        if (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) == 1) {
            return "昨天";
        }

        return (otherCalendar.get(Calendar.AM_PM) == Calendar.AM ? "上午" : "下午") + " "
                + parseLongToString(time, format8)/*otherCalendar.get(Calendar.HOUR) + ":" + otherCalendar.get(Calendar.MINUTE)*/;
    }

    /**
     * 格式一：
     * 1、今天
     * 上午3:10，下午1:02
     * 2、昨天
     * 昨天
     * 3、其他
     * 2015/1/1
     * 使用场景：
     * （1）消息列表
     * （2）审批－我的审批和我的申请列表
     * （3）目标管理－列表，评论，子任务
     * （4）费用报销－我的单据，我的审批的列表
     *
     * @param time
     * @return
     */
    public static String getRelativeTime1(long time) {
        if (time == 0) {
            return "";
        }
        Calendar otherCalendar = Calendar.getInstance();
        otherCalendar.setTimeInMillis(time);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        if (otherCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR)
                || (otherCalendar.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH))
                || (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) > 1)) {

            return String.valueOf(otherCalendar.get(Calendar.YEAR))/*.substring(2)*/ + "/" + (otherCalendar.get(Calendar.MONTH) + 1) + "/"
                    + otherCalendar.get(Calendar.DAY_OF_MONTH);
        }

        if (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) == 1) {
            return "昨天";
        }

        return addAM_PM(time, otherCalendar);
    }

    /**
     * 格式二：
     * 1、今天
     * 上午3:10，下午1:02
     * 2、昨天
     * 昨天上午3:10，昨天下午1:02
     * 3、其他
     * 2015/1/1上午3:10，2015/1/1下午1:02
     * 使用场景：
     * （1）聊天界面
     * （2）审批－审批流程的时间
     * （3）目标管理－汇报结果的时间，改动记录的时间
     *
     * @param time
     * @return
     */
    public static String getRelativeTime2(long time) {
        Calendar otherCalendar = Calendar.getInstance();
        otherCalendar.setTimeInMillis(time);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        //昨天以前的格式:2015/1/1下午1:02
        if (otherCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR)
                || (otherCalendar.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH))
                || (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) > 1)) {

            return String.valueOf(otherCalendar.get(Calendar.YEAR))/*.substring(2)*/ + "/" + (otherCalendar.get(Calendar.MONTH) + 1) + "/"
                    + otherCalendar.get(Calendar.DAY_OF_MONTH)
                    + addAM_PM(time, otherCalendar);
        }
        //昨天的格式:昨天下午1:02
        if (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) == 1) {
            return "昨天" + addAM_PM(time, otherCalendar);
        }

        //今天的格式:下午1:02
        return addAM_PM(time, otherCalendar);
    }

    public static String getRelativeTime3(long time) {
        Calendar otherCalendar = Calendar.getInstance();
        otherCalendar.setTimeInMillis(time);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        if (otherCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR)
                || (otherCalendar.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH))
                || (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) > 1)) {

            return String.valueOf(otherCalendar.get(Calendar.YEAR))/*.substring(2)*/ + "/" + (otherCalendar.get(Calendar.MONTH) + 1) + "/"
                    + otherCalendar.get(Calendar.DAY_OF_MONTH);
        }

        if (currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH) == 1) {
            return "昨天";
        }

        return "今天";
    }


    public static String getRelativeTime4(long time) {
        Calendar otherCalendar = Calendar.getInstance();
        otherCalendar.setTimeInMillis(time);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        if (otherCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR)) {
            //不是今年
            return parseLongToString(time, format9);
        }

        int differMonth = currentCalendar.get(Calendar.MONTH) - otherCalendar.get(Calendar.MONTH);
        if (differMonth > 0) {
            return differMonth + "个月以前";
        }
        if (differMonth < 0) {
            return parseLongToString(time, format9);
        }

//        执行到此处,说明是同年同月

        int differDay = currentCalendar.get(Calendar.DAY_OF_MONTH) - otherCalendar.get(Calendar.DAY_OF_MONTH);

        if ((differDay > 1)) {
            //是今年当月,但不是昨天,可能是前天或者更早以前
            return differDay + "天前";
        }
        if (differDay == 1) {
            //是今年当月,而且是昨天的
            return "昨天" + parseLongToString(time, format12);
        }
        if (differDay == 0) {
            //是今年当月,而且是今天的
            return "今天" + parseLongToString(time, format12);
        }

        if (differDay == -1) {
            //是今年当月,而且是今天的
            return "明天" + parseLongToString(time, format12);
        }
        if (differDay == -2) {
            //是今年当月,而且是今天的
            return "后天" + parseLongToString(time, format12);
        }

        //大后台或更以后:显示具体年月日时分
        return parseLongToString(time, format9);
    }


    /**
     * 添加上午下午的时间格式,格式形如:下午1:02
     *
     * @param time
     * @param otherCalendar
     * @return
     */
    private static String addAM_PM(long time, Calendar otherCalendar) {
        return (otherCalendar.get(Calendar.AM_PM) == Calendar.AM ? "上午" : "下午")
                + parseLongToString(time, format8);
    }

    /**
     * 获取相对时间:
     * 7天前
     * 1天前
     * 1小时前
     * 1分钟前
     * 刚刚
     *
     * @param time
     * @return
     */
    public static String getRelativeTime5(long time) {
        long currentTime = System.currentTimeMillis();
//        long currentTime = parseStringToMillis("2018-01-10 12:00:00",format1);

        long diffDuration = currentTime - time;

        if (diffDuration > ONE_WEEK) {
            return parseLongToString(time, format4);
        } else if (diffDuration >= ONE_DAY) {
            return (diffDuration / ONE_DAY) + "天前";
        } else if (diffDuration >= ONE_HOUR) {
            return (diffDuration / ONE_HOUR) + "小时前";
        } else if (diffDuration >= ONE_MINUTE) {
            return (diffDuration / ONE_MINUTE) + "分钟前";
        }
        return "刚刚";
    }

    /**
     * 注意secondL单位为秒,而不是毫秒
     *
     * @param secondL
     * @return
     */
    public static String secToTime(long secondL) {
        return secToTime(secondL, false);
    }

    /**
     * 注意secondL单位为秒,而不是毫秒
     *
     * @param secondL
     * @param isRemovePrefix
     * @return
     */
    public static String secToTime(long secondL, boolean isRemovePrefix) {
        int    time    = (int) secondL;
        String timeStr = null;
        int    hour    = 0;
        int    minute  = 0;
        int    second  = 0;
        if (time <= 0)
            return "00:00:00";
        else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                String hourStr = unitFormat(hour);
                if (isRemovePrefix && "00".equals(hourStr)) {
                    hourStr = "";
                } else {
                    hourStr = hourStr + ":";
                }

                timeStr = hourStr + unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;

                String hourStr = unitFormat(hour);
                if (isRemovePrefix && "00".equals(hourStr)) {
                    hourStr = "";
                } else {
                    hourStr = hourStr + ":";
                }
                timeStr = hourStr + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    public static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }

    public static String formatTimeDuration(long timeDuration) {
        long second = timeDuration / 1000;
        if (second < 60) {
            return "";
        } else if (second < 3600) {
            return second / 60 + "分钟";
        } else {
            return second / 3600 + "小时" + ((second % 3600) / 60) + "分钟";
        }
    }


    public static String formatTimeDurationForBookRack(long timeDuration) {
        long second = timeDuration / 1000;
        if (second < 60) {
            return "0";
        } else if (second < 3600) {
            return String.valueOf(second / 60);
        } else {
            return String.valueOf(second / 3600 * 60 + ((second % 3600) / 60));
        }
    }

    //判断闰年
    public static boolean isLeap(int year) {
        if (((year % 100 == 0) && year % 400 == 0) || ((year % 100 != 0) && year % 4 == 0))
            return true;
        else
            return false;
    }

    //返回当月天数
    public static int getDays(int year, int month) {
        int days;
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                days = 31;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                days = 30;
                break;
            case 2:
                int febDay = 28;
                if (isLeap(year)) {
                    febDay = 29;
                }
                days = febDay;
                break;
            default:
                days = 0;
                break;
        }
        return days;
    }

    public static String parseWeek(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "星期日";
            case Calendar.MONDAY:
                return "星期一";
            case Calendar.TUESDAY:
                return "星期二";
            case Calendar.WEDNESDAY:
                return "星期三";
            case Calendar.THURSDAY:
                return "星期四";
            case Calendar.FRIDAY:
                return "星期五";
            case Calendar.SATURDAY:
                return "星期六";
        }
        return "星期日";

    }
}
