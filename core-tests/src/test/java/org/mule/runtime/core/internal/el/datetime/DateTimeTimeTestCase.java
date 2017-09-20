/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.datetime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DateTimeTimeTestCase extends AbstractMuleTestCase {

  private static final int TWO_DAYS_IN_SECONDS = 172800;
  private static final int TWO_DAYS_IN_MINUTES = 2880;
  private static final int TWO_DAYS_IN_HOURS = 48;
  private volatile Calendar calendarNow;
  private volatile DateTime now;
  private volatile DateTime before;
  private volatile DateTime after;

  @Before
  public void setup() {
    calendarNow = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

    GregorianCalendar calendarBefore = ((GregorianCalendar) calendarNow.clone());
    calendarBefore.add(Calendar.MILLISECOND, -10);

    GregorianCalendar calendarAfter = ((GregorianCalendar) calendarNow.clone());
    calendarAfter.add(Calendar.MILLISECOND, +10);

    before = new DateTime(calendarBefore).withTimeZone("UTC");
    now = new DateTime((GregorianCalendar) calendarNow.clone()).withTimeZone("UTC");
    after = new DateTime(calendarAfter).withTimeZone("UTC");
  }

  @Test
  public void milliSeconds() {
    assertThat(now.getMilliSeconds(), is((long) calendarNow.get(Calendar.MILLISECOND)));
  }

  @Test
  public void isBefore() {
    assertThat(before.isBefore(now), is(true));
    assertThat(before.isBefore(after), is(true));
    assertThat(now.isBefore(after), is(true));
  }

  @Test
  public void isAfter() {
    assertThat(after.isAfter(before), is(true));
    assertThat(after.isAfter(now), is(true));
    assertThat(now.isAfter(before), is(true));
  }

  @Test
  public void format() {
    SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertThat(now.format("hh:mm:ss"), is(df.format(calendarNow.getTime())));
  }

  @Test
  public void timeZone() {
    assertThat(now.getTimeZone(), is(TimeZone.getTimeZone("UTC").getDisplayName()));
  }

  @Test
  public void plusSeconds() {
    assertThat(now.plusSeconds(1).getSeconds(), is((calendarNow.get(Calendar.SECOND) + 1) % 60));
  }

  @Test
  public void plusSecondsShouldKeepSameDayInRollover() {
    assertSameDay(now.getTime(), now.getTime().plusSeconds(TWO_DAYS_IN_SECONDS));
  }

  @Test
  public void plusMinutes() {
    assertThat(now.getTime().plusMinutes(1).getMinutes(), is((calendarNow.get(Calendar.MINUTE) + 1) % 60));
  }

  @Test
  public void plusMinutesShouldKeepSameDayInRollover() {
    assertSameDay(now.getTime(), now.getTime().plusMinutes(TWO_DAYS_IN_MINUTES));
  }

  @Test
  public void plusHours() {
    assertThat(now.getTime().plusHours(1).getHours(), is((calendarNow.get(Calendar.HOUR_OF_DAY) + 1) % 24));
  }

  @Test
  public void plusHoursShouldKeepSameDayInRollover() {
    assertSameDay(now.getTime(), now.getTime().plusHours(TWO_DAYS_IN_HOURS));
  }

  private void assertSameDay(Time a, Time b) {
    assertThat(a.toCalendar().get(Calendar.DAY_OF_YEAR), is(b.toCalendar().get(Calendar.DAY_OF_YEAR)));
  }

  @Test
  public void withTimeZone() {
    int hour = now.getHours();
    assertThat(now.withTimeZone("GMT-03:00").getHours(), is(hour));
  }

  @Test
  public void changeTimeZone() {
    int hour = now.getHours();
    assertThat(now.changeTimeZone("GMT-03:00").getHours(), is((hour + 24 - 3) % 24));
  }

  @Test
  public void changeTimeZoneRollover() {
    assertThat(now.getTime().plusHours(48).toCalendar().get(Calendar.DAY_OF_YEAR), is(1));
  }

  @Test
  public void seconds() {
    assertThat(now.getSeconds(), is(calendarNow.get(Calendar.SECOND)));
  }

  @Test
  public void minutes() {
    assertThat(now.getMinutes(), is(calendarNow.get(Calendar.MINUTE)));
  }

  @Test
  public void hourOfDay() {
    assertThat(now.getHours(), is(calendarNow.get(Calendar.HOUR_OF_DAY)));
  }

  @Test
  public void testToString() {
    assertThat(now.getTime().withTimeZone("UTC").toString().substring(0, 8),
               is(DatatypeConverter.printTime(Calendar.getInstance(TimeZone.getTimeZone("UTC"))).substring(0, 8)));
  }

  @Test
  public void toDate() {
    assertThat(now.toDate(), is(instanceOf(Date.class)));
  }

  @Test
  public void toCalendar() {
    assertThat(now.toCalendar(), is(instanceOf(GregorianCalendar.class)));
  }

  @Test
  public void toXMLCalendar() throws DatatypeConfigurationException {
    assertThat(now.toXMLCalendar(), is(instanceOf(XMLGregorianCalendar.class)));
  }

  @Test
  public void fromDate() {
    Date date = new Date();
    date.setYear(0);
    date.setMonth(0);
    date.setDate(1);
    assertEquals(1900, new DateTime(date).getYear());
    assertEquals(1, new DateTime(date).getMonth());
    assertEquals(1, new DateTime(date).getDayOfMonth());
  }

  @Test
  public void fromCalendar() {
    Calendar cal = new GregorianCalendar();
    cal.set(Calendar.YEAR, 1900);
    cal.set(Calendar.MONTH, 0);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    assertEquals(1900, new DateTime(cal).getYear());
    assertEquals(1, new DateTime(cal).getMonth());
    assertEquals(1, new DateTime(cal).getDayOfMonth());
  }

  @Test
  public void fromXMLCalendar() throws DatatypeConfigurationException {
    XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
    xmlCal.setYear(1900);
    xmlCal.setMonth(1);
    xmlCal.setDay(1);
    assertEquals(1900, new DateTime(xmlCal).getYear());
    assertEquals(1, new DateTime(xmlCal).getMonth());
    assertEquals(1, new DateTime(xmlCal).getDayOfMonth());
  }

}
