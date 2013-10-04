/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations;

import org.mule.api.annotations.meta.Channel;
import org.mule.api.annotations.meta.ChannelType;
import org.mule.transport.quartz.config.ScheduleConfigBuilder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that allows you to call the method at the specified interval.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Channel(identifer = "quartz", type = ChannelType.Inbound)
public @interface Schedule
{
    /**
     * The cron expression to schedule events at specified dates/times. You must set either this attribute or the interval attribute. A cron expression is a string comprised by 6 or 7 fields separated by white space. Fields can contain any of the allowed values, along with various combinations of the allowed special characters for that field. The fields are as follows:
     * {html}
     * <table>
     * <th>Field Name </th><th>Mandatory</th><th>Allowed Values</th><th>Allowed Special Chars</th>
     * <tr><td>Seconds</td><td>YES</td><td>0-59</td><td>, - * /</td></tr>
     * <tr><td>Minutes</td><td>YES</td><td>0-59</td><td>, - * /</td></tr>
     * <tr><td>Hours</td><td>YES</td><td>0-23</td><td>, - * /</td></tr>
     * <tr><td>Day of Month</td><td>YES</td><td>1-31</td><td>, - * ? / L W C</td></tr>
     * <tr><td>Month</td><td>YES</td><td>1-12 or JAN-DEC</td><td>, - * /</td></tr>
     * <tr><td>Day of Week</td><td>YES</td><td>1-7 or SUN-SAT</td><td>, - * ? / L C #</td></tr>
     * <tr><td>Year</td><td>NO</td><td>empty, 1970-2099</td><td>, - * /</td></tr>
     * </table>
     * <p/>
     * Cron expressions can be as simple as this: <b>* * * * ? *</b><br></br>
     * or more complex, like this: <b>0 0/5 14,18,3-39,52 ? JAN,MAR,SEP MON-FRI 2002-2010</b>
     * <p/>
     * <B>Some examples:</b>
     * <ul>
     * <li>0 0 12 * * ?     Fire at 12pm (noon) every day</li>
     * <li>0 15 10 ? * *     Fire at 10:15am every day</li>
     * <li>0 15 10 * * ?     Fire at 10:15am every day</li>
     * <li>0 15 10 * * ? *     Fire at 10:15am every day</li>
     * <li>0 15 10 * * ?    2005 Fire at 10:15am every day during the year 2005</li>
     * <li>0 * 14 * * ?     Fire every minute starting at 2pm and ending at 2:59pm, every day</li>
     * <li>0 0/5 14 * * ?     Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day</li>
     * </ul>
     * <p/>
     * <p/>
     * Individual sub-expressions can contain ranges and/or lists. For example, the day of week field in the previous (which reads "WED") example could be replaces with "MON-FRI", "MON, WED, FRI", or even "MON-WED,SAT".
     * <p/>
     * Wild-cards (the '*' character) can be used to say "every" possible value of this field. Therefore the '*' character in the "Month" field of the previous example simply means "every month". A '*' in the Day-Of-Week field would obviously mean "every day of the week".
     * <p/>
     * All of the fields have a set of valid values that can be specified. These values should be fairly obvious - such as the numbers 0 to 59 for seconds and minutes, and the values 0 to 23 for hours. Day-of-Month can be any value 0-31, but you need to be careful about how many days are in a given month! Months can be specified as values between 0 and 11, or by using the strings JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV and DEC. Days-of-Week can be specified as vaules between 1 and 7 (1 = Sunday) or by using the strings SUN, MON, TUE, WED, THU, FRI and SAT.
     * <p/>
     * The '/' character can be used to specify increments to values. For example, if you put '0/15' in the Minutes field, it means 'every 15 minutes, starting at minute zero'. If you used '3/20' in the Minutes field, it would mean 'every 20 minutes during the hour, starting at minute three' - or in other words it is the same as specifying '3,23,43' in the Minutes field.
     * <p/>
     * The '?' character is allowed for the day-of-month and day-of-week fields. It is used to specify "no specific value". This is useful when you need to specify something in one of the two fields, but not the other. See the examples below (and CronTrigger JavaDoc) for clarification.
     * <p/>
     * The 'L' character is allowed for the day-of-month and day-of-week fields. This character is short-hand for "last", but it has different meaning in each of the two fields. For example, the value "L" in the day-of-month field means "the last day of the month" - day 31 for January, day 28 for February on non-leap years. If used in the day-of-week field by itself, it simply means "7" or "SAT". But if used in the day-of-week field after another value, it means "the last xxx day of the month" - for example "6L" or "FRIL" both mean "the last friday of the month". When using the 'L' option, it is important not to specify lists, or ranges of values, as you'll get confusing results.
     * <p/>
     * The 'W' is used to specify the weekday (Monday-Friday) nearest the given day. As an example, if you were to specify "15W" as the value for the day-of-month field, the meaning is: "the nearest weekday to the 15th of the month".
     * <p/>
     * The '#' is used to specify "the nth" XXX weekday of the month. For example, the value of "6#3" or "FRI#3" in the day-of-week field means "the third Friday of the month".
     * <p/>
     * Here are a few more examples of expressions and their meanings - you can find even more in the JavaDoc for CronTrigger
     * Example Cron Expressions
     * <p/>
     * CronTrigger Example 1 - an expression to create a trigger that simply fires every 5 minutes
     * <p/>
     * "0 0/5 * * * ?"
     * <p/>
     * CronTrigger Example 2 - an expression to create a trigger that fires every 5 minutes, at 10 seconds after the minute (i.e. 10:00:10 am, 10:05:10 am, etc.).
     * <p/>
     * "10 0/5 * * * ?"
     * <p/>
     * CronTrigger Example 3 - an expression to create a trigger that fires at 10:30, 11:30, 12:30, and 13:30, on every Wednesday and Friday.
     * <p/>
     * "0 30 10-13 ? * WED,FRI"
     * <p/>
     * CronTrigger Example 4 - an expression to create a trigger that fires every half hour between the hours of 8 am and 10 am on the 5th and 20th of every month. Note that the trigger will NOT fire at 10:00 am, just at 8:00, 8:30, 9:00 and 9:30
     * <p/>
     * "0 0/30 8-9 5,20 * ?"
     * {html}
     *
     * @return The cron expression to use for scheduling calls to this method or an empty string if not set
     */
    public abstract String cron() default "";

    /**
     * <p>
     * A reference to a {@link ScheduleConfigBuilder} object used to configure this
     * scheduler job.
     * </p>
     * The scheduler reference can be a reference to a connector in the local
     * registry or a reference to an object in the Mule service registry. Local
     * configBuilder references can use the "id" value passed into the
     * {@link ScheduleConfigBuilder}. </p>
     *
     * @return The scheduler name associated with the schedule action
     */
    public abstract String config() default "";

    /**
     * The number of milliseconds between two scheduled invocations of the method.
     * This attribute or cron is required.
     *
     * @return The number of milliseconds between two scheduled invocations of the method.
     */
    public abstract long interval() default -1;

    /**
     * The number of milliseconds that will elapse before the first event is fired. The default is -1, which means the
     * first event is fired as soon as the service is started.
     *
     * @return The number of milliseconds that will elapse before the first event is fired.
     */
    public abstract long startDelay() default -1;

}
