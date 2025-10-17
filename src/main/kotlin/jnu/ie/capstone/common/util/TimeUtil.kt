package jnu.ie.capstone.common.util

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtil {
    private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    private val SIMPLE_DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun zonedDateTimeNow(): ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)

    fun formatToSimpleString(dateTime: ZonedDateTime): String {
        return dateTime.format(SIMPLE_DATE_FORMATTER)
    }

    fun nowString() = formatToSimpleString(zonedDateTimeNow())
}