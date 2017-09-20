/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.datetime;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Models a DateTime and simplifies the parsing/formatting and very basic manipulation of dates via Mule expression language.
 */
public class DateTime extends AbstractInstant implements Date, Time, Serializable {

  private static final long serialVersionUID = -5006713884149136787L;

  public DateTime(Calendar calendar, Locale locale) {
    super(calendar, locale);
  }

  public DateTime() {
    super();
  }

  public DateTime(Calendar calendar) {
    this(calendar, Locale.getDefault());
  }

  public DateTime(java.util.Date date) {
    this(Calendar.getInstance(Calendar.getInstance().getTimeZone(), Locale.getDefault()), Locale.getDefault());
    this.calendar.setTime(date);
  }

  public DateTime(XMLGregorianCalendar xmlCalendar) {
    this(xmlCalendar.toGregorianCalendar(), Locale.getDefault());
  }

  public DateTime(String iso8601String) {
    this(datatypeFactory.newXMLGregorianCalendar(iso8601String).toGregorianCalendar());
  }

  public DateTime(String dateString, String format) throws ParseException {
    this(Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault()), Locale.getDefault());
    SimpleDateFormat dateFormat = new SimpleDateFormat(format);
    dateFormat.setCalendar(this.calendar);
    dateFormat.parse(dateString);
  }

  @Override
  public String toString() {
    return DatatypeConverter.printDateTime(calendar);
  }

  public int getDayOfWeek() {
    return calendar.get(Calendar.DAY_OF_WEEK);
  }

  public int getDayOfMonth() {
    return calendar.get(Calendar.DAY_OF_MONTH);
  }

  public int getDayOfYear() {
    return calendar.get(Calendar.DAY_OF_YEAR);
  }

  public int getWeekOfMonth() {
    return calendar.get(Calendar.WEEK_OF_MONTH);
  }

  public int getWeekOfYear() {
    return calendar.get(Calendar.WEEK_OF_YEAR);
  }

  public int getMonth() {
    return calendar.get(Calendar.MONTH) + 1;
  }

  public int getYear() {
    return calendar.get(Calendar.YEAR);
  }

  public DateTime plusDays(int add) {
    calendar.add(Calendar.DAY_OF_YEAR, add);
    return this;
  }

  public DateTime plusWeeks(int add) {
    calendar.add(Calendar.WEEK_OF_YEAR, add);
    return this;
  }

  public DateTime plusMonths(int add) {
    calendar.add(Calendar.MONTH, add);
    return this;
  }

  public DateTime plusYears(int add) {
    calendar.add(Calendar.YEAR, add);
    return this;
  }

  public DateTime plusMilliSeconds(int add) {
    calendar.add(Calendar.MILLISECOND, add);
    return this;
  }

  public DateTime plusSeconds(int add) {
    calendar.add(Calendar.SECOND, add);
    return this;
  }

  public DateTime plusMinutes(int add) {
    calendar.add(Calendar.MINUTE, add);
    return this;
  }

  public DateTime plusHours(int add) {
    calendar.add(Calendar.HOUR_OF_DAY, add);
    return this;
  }

  public long getMilliSeconds() {
    return calendar.get(Calendar.MILLISECOND);
  }

  public int getSeconds() {
    return calendar.get(Calendar.SECOND);
  }

  public int getMinutes() {
    return calendar.get(Calendar.MINUTE);
  }

  public int getHours() {
    return calendar.get(Calendar.HOUR_OF_DAY);
  }

  @Override
  public DateTime withLocale(String locale) {
    super.withLocale(locale);
    return this;
  }

  @Override
  public DateTime withTimeZone(String newTimezone) {
    super.withTimeZone(newTimezone);
    return this;
  };

  @Override
  public DateTime changeTimeZone(String newTimezone) {
    super.changeTimeZone(newTimezone);
    return this;
  }

  public Date getDate() {
    return new InternalDate((Calendar) calendar.clone(), locale);
  }

  public Time getTime() {
    return new InternalTime((Calendar) calendar.clone(), locale);
  }

  class InternalDate extends AbstractInstant implements Date {

    public InternalDate(Calendar calendar, Locale locale) {
      super(calendar, locale);
      resetTime();
    }

    protected void resetTime() {
      this.calendar.get(Calendar.ERA);
      this.calendar.set(Calendar.HOUR_OF_DAY, 0);
      this.calendar.set(Calendar.MINUTE, 0);
      this.calendar.set(Calendar.SECOND, 0);
      this.calendar.set(Calendar.MILLISECOND, 0);
    }

    public int getDayOfWeek() {
      return calendar.get(Calendar.DAY_OF_WEEK);
    }

    public int getDayOfMonth() {
      return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public int getDayOfYear() {
      return calendar.get(Calendar.DAY_OF_YEAR);
    }

    public int getWeekOfMonth() {
      return calendar.get(Calendar.WEEK_OF_MONTH);
    }

    public int getWeekOfYear() {
      return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    public int getMonth() {
      return calendar.get(Calendar.MONTH) + 1;
    }

    public int getYear() {
      return calendar.get(Calendar.YEAR);
    }

    public Date plusDays(int add) {
      calendar.add(Calendar.DAY_OF_YEAR, add);
      return this;
    }

    public Date plusWeeks(int add) {
      calendar.add(Calendar.WEEK_OF_YEAR, add);
      return this;
    }

    public Date plusMonths(int add) {
      calendar.add(Calendar.MONTH, add);
      return this;
    }

    public Date plusYears(int add) {
      calendar.add(Calendar.YEAR, add);
      return this;
    }

    @Override
    public Date withLocale(String locale) {
      super.withLocale(locale);
      return this;
    }

    @Override
    public Date withTimeZone(String newTimezone) {
      super.withTimeZone(newTimezone);
      return this;
    };

    @Override
    public Date changeTimeZone(String newTimezone) {
      super.changeTimeZone(newTimezone);
      resetTime();
      return this;
    }

    @Override
    public String toString() {
      return DatatypeConverter.printDate(calendar);
    }
  }

  class InternalTime extends AbstractInstant implements Time {

    public InternalTime(Calendar calendar, Locale locale) {
      super(calendar, locale);
      resetDate();
    }

    public Time plusMilliSeconds(int add) {
      calendar.add(Calendar.MILLISECOND, add);
      resetDate();
      return this;
    }

    public Time plusSeconds(int add) {
      calendar.add(Calendar.SECOND, add);
      resetDate();
      return this;
    }

    public Time plusMinutes(int add) {
      calendar.add(Calendar.MINUTE, add);
      resetDate();
      return this;
    }

    public Time plusHours(int add) {
      calendar.add(Calendar.HOUR_OF_DAY, add);
      resetDate();
      return this;
    }

    public long getMilliSeconds() {
      return calendar.get(Calendar.MILLISECOND);
    }

    public int getSeconds() {
      return calendar.get(Calendar.SECOND);
    }

    public int getMinutes() {
      return calendar.get(Calendar.MINUTE);
    }

    public int getHours() {
      return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public Time changeTimeZone(String newTimezone) {
      super.changeTimeZone(newTimezone);
      resetDate();
      return this;
    }

    @Override
    public Time withLocale(String locale) {
      super.withLocale(locale);
      return this;
    }

    @Override
    public Time withTimeZone(String newTimezone) {
      super.withTimeZone(newTimezone);
      return this;
    };

    @Override
    public String toString() {
      return DatatypeConverter.printTime(calendar);
    }

    private void resetDate() {
      // Workaround for issues with java.util.Calendar. Ensure Calendar is in right state before
      // updating timeZone
      calendar.get(Calendar.ERA);
      calendar.set(Calendar.YEAR, 1970);
      calendar.set(Calendar.DAY_OF_YEAR, 1);
    }

  }

  private void writeObject(ObjectOutputStream out) throws Exception {
    out.defaultWriteObject();
    out.writeObject(this.calendar);
    out.writeObject(this.locale);
  }

  private void readObject(ObjectInputStream in) throws Exception {
    in.defaultReadObject();
    this.calendar = (Calendar) in.readObject();
    this.locale = (Locale) in.readObject();
  }

}
