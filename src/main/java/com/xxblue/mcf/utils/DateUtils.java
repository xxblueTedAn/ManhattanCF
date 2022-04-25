package com.xxblue.mcf.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
public class DateUtils {

    public static final String DF_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    public static LocalDateTime datetimeISO8601(String dateString) {

        if (dateString == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601);
        dateformater.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        Date date = null;
        try {
            date = dateformater.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        return localDateTime;
    }
    
}