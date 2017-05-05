/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.junit.Assert.assertEquals;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

public class PetStoreISO8601DateParsingTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int YEAR = 2008;
  public static final int MONTH = 9;
  public static final int DAY = 15;
  public static final int HOUR = 15;
  public static final int MINUTE = 53;
  public static final int SECOND = 23;

  @Override
  protected String getConfigFile() {
    return "petstore-iso8601-config.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void testDateTimeWithTimeZone() throws Exception {
    Date date = getDate("getWithTimeZone");
    ZonedDateTime openingDate = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("+05:00"));
    assertTimeZoneDate(openingDate);
    assertTimeZoneDateTime(openingDate);
  }

  @Test
  public void testDateTimeWithTimeZoneNoSeconds() throws Exception {
    Date date = getDate("getWithTimeZoneNoSeconds");
    ZonedDateTime openingDate = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("+05:00"));
    assertTimeZoneDate(openingDate);
    assertTimeZoneWithoutSeconds(openingDate);
  }

  @Test
  public void testDateTimeWithTimeZoneNoMinutes() throws Exception {
    Date date = getDate("getWithTimeZoneNoMinutes");
    ZonedDateTime openingDate = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("+05:00"));
    assertTimeZoneDate(openingDate);
    assertTimeZoneHour(openingDate);
  }

  @Test
  public void testDateTime() throws Exception {
    Date date = getDate("getWithDateTime");
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    assertDate(calendar);
    assertTime(calendar);
  }

  @Test
  public void testDate() throws Exception {
    Date date = getDate("getWithDate");
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    assertDate(calendar);
  }

  @Test
  public void testDateList() throws Exception {
    Calendar calendar = Calendar.getInstance();
    PetStoreClient client = getClient("getDateList");
    List<Date> dates = client.getClosedForHolidays();
    calendar.setTime(dates.get(0));
    assertEquals(dates.size(), 1);
    assertDate(calendar);
  }

  @Test
  public void testDateTimeList() throws Exception {
    PetStoreClient client = getClient("getDateTimeList");
    List<LocalDateTime> dates = client.getDiscountDates();
    assertEquals(dates.size(), 1);
    assertDateTime(dates.get(0));
  }

  public Date getDate(String flowName) throws Exception {
    PetStoreClient client = getClient(flowName);
    return client.getOpeningDate(); // 2008-09-15T15:53:23+05:00
  }

  public PetStoreClient getClient(String flowName) throws Exception {
    return ((PetStoreClient) flowRunner(flowName).run().getMessage().getPayload().getValue());
  }

  private void assertDate(Calendar openingDate) {
    assertEquals(openingDate.get(Calendar.YEAR), YEAR);
    assertEquals(openingDate.get(Calendar.MONTH) + 1, MONTH);
    assertEquals(openingDate.get(Calendar.DAY_OF_MONTH), DAY);
  }

  private void assertDateTime(LocalDateTime localDateTime) {
    assertEquals(localDateTime.getYear(), YEAR);
    assertEquals(localDateTime.getMonthValue(), MONTH);
    assertEquals(localDateTime.getDayOfMonth(), DAY);
    assertEquals(localDateTime.getHour(), HOUR);
    assertEquals(localDateTime.getMinute(), MINUTE);
    assertEquals(localDateTime.getSecond(), SECOND);
  }

  private void assertTime(Calendar openingDate) {
    assertEquals(openingDate.get(Calendar.HOUR_OF_DAY), HOUR);
    assertEquals(openingDate.get(Calendar.MINUTE), MINUTE);
    assertEquals(openingDate.get(Calendar.SECOND), SECOND);
  }

  private void assertTimeZoneDate(ZonedDateTime openingDate) {
    assertEquals(openingDate.getYear(), YEAR);
    assertEquals(openingDate.getMonthValue(), MONTH);
    assertEquals(openingDate.getDayOfMonth(), DAY);
  }

  private void assertTimeZoneDateTime(ZonedDateTime openingDate) {
    assertEquals(openingDate.getHour(), HOUR);
    assertEquals(openingDate.getMinute(), MINUTE);
    assertEquals(openingDate.getSecond(), SECOND);
  }

  private void assertTimeZoneWithoutSeconds(ZonedDateTime openingDate) {
    assertEquals(openingDate.getHour(), HOUR);
    assertEquals(openingDate.getMinute(), MINUTE);
  }

  private void assertTimeZoneHour(ZonedDateTime openingDate) {
    assertEquals(openingDate.getHour(), 15);
  }



}
