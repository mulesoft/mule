/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.junit.Assert.assertEquals;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class PetStoreISO8601DateParsingTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Override
  protected String getConfigFile() {
    return "petstore-iso8601-config.xml";
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


  public Date getDate(String flowName) throws Exception {
    PetStoreClient client = flowRunner(flowName).run().getMessage().getPayload();

    return client.getOpeningDate(); // 2008-09-15T15:53:23+05:00
  }

  private void assertDate(Calendar openingDate) {
    assertEquals(openingDate.get(Calendar.YEAR), 2008);
    assertEquals(openingDate.get(Calendar.MONTH) + 1, 9);
    assertEquals(openingDate.get(Calendar.DAY_OF_MONTH), 15);
  }

  private void assertTime(Calendar openingDate) {
    assertEquals(openingDate.get(Calendar.HOUR_OF_DAY), 15);
    assertEquals(openingDate.get(Calendar.MINUTE), 53);
    assertEquals(openingDate.get(Calendar.SECOND), 23);
  }

  private void assertTimeZoneDate(ZonedDateTime openingDate) {
    assertEquals(openingDate.getYear(), 2008);
    assertEquals(openingDate.getMonthValue(), 9);
    assertEquals(openingDate.getDayOfMonth(), 15);
  }

  private void assertTimeZoneDateTime(ZonedDateTime openingDate) {
    assertEquals(openingDate.getHour(), 15);
    assertEquals(openingDate.getMinute(), 53);
    assertEquals(openingDate.getSecond(), 23);
  }

  private void assertTimeZoneWithoutSeconds(ZonedDateTime openingDate) {
    assertEquals(openingDate.getHour(), 15);
    assertEquals(openingDate.getMinute(), 53);
  }

  private void assertTimeZoneHour(ZonedDateTime openingDate) {
    assertEquals(openingDate.getHour(), 15);
  }



}
