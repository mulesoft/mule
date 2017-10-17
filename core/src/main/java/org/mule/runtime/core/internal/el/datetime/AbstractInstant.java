/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.datetime;

import static java.util.Objects.hash;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.LocaleUtils;

public abstract class AbstractInstant implements Instant {

  protected Locale locale;
  protected Calendar calendar;

  protected static final DatatypeFactory datatypeFactory;

  static {
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new Error(e);
    }
  }

  public AbstractInstant(Calendar calendar, Locale locale) {
    this.calendar = calendar;
    this.locale = locale;
  }

  public AbstractInstant() {
    this.calendar = Calendar.getInstance(Locale.getDefault());
    this.locale = Locale.getDefault();
  }

  @Override
  public boolean isBefore(Instant date) {
    return calendar.before(date.toCalendar());
  }

  @Override
  public boolean isAfter(Instant date) {
    return calendar.after(date.toCalendar());
  }

  @Override
  public String format() {
    return toString();
  }

  @Override
  public String format(String pattern) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, locale);
    dateFormat.setTimeZone(calendar.getTimeZone());
    return dateFormat.format(calendar.getTime());
  }

  @Override
  public String getTimeZone() {
    return calendar.getTimeZone().getDisplayName(locale);
  }

  @Override
  public Instant withTimeZone(String newTimezone) {
    TimeZone timeZone = TimeZone.getTimeZone(newTimezone);
    calendar.add(Calendar.MILLISECOND, -timeZone.getOffset(calendar.getTimeInMillis()));
    calendar.setTimeZone(timeZone);
    return this;
  }

  @Override
  public Instant changeTimeZone(String newTimezone) {
    // Workaround for issues with java.util.Calendar. Ensure Calendar is in right state before updating
    // timeZone
    calendar.get(Calendar.ERA);
    calendar.setTimeZone(TimeZone.getTimeZone(newTimezone));
    return this;
  }

  @Override
  public Instant withLocale(String locale) {
    this.locale = LocaleUtils.toLocale(locale);
    Calendar newCalendar = Calendar.getInstance(calendar.getTimeZone(), this.locale);
    newCalendar.setTime(calendar.getTime());
    this.calendar = newCalendar;
    return this;
  }

  @Override
  public Calendar toCalendar() {
    return calendar;
  }

  @Override
  public java.util.Date toDate() {
    return calendar.getTime();
  }

  @Override
  public XMLGregorianCalendar toXMLCalendar() throws DatatypeConfigurationException {
    if (calendar instanceof GregorianCalendar) {
      return datatypeFactory.newXMLGregorianCalendar((GregorianCalendar) calendar);
    } else {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage("org.mule.runtime.core.internal.el.datetime.DateTime.toXMLCalendar() does not support non-gregorian calendars."));
    }
  }

  private int getTimeZoneOffset() {
    return calendar.get(Calendar.ZONE_OFFSET);
  }

  @Override
  public int hashCode() {
    return hash(calendar.getTimeInMillis(), calendar.getFirstDayOfWeek(), calendar.getMinimalDaysInFirstWeek(),
                getTimeZoneOffset());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AbstractInstant)) {
      return false;
    } else {
      AbstractInstant other = (AbstractInstant) obj;
      return calendar.getTimeInMillis() == other.calendar.getTimeInMillis()
          && calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek()
          && calendar.getMinimalDaysInFirstWeek() == other.calendar.getMinimalDaysInFirstWeek()
          && getTimeZoneOffset() == other.getTimeZoneOffset();
    }
  }
}
