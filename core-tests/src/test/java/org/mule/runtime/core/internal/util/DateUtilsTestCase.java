/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Date;

import org.junit.Test;

@SmallTest
public class DateUtilsTestCase extends AbstractMuleTestCase {

  private final String TEST_DATE_FORMAT = "dd/MM/yyyy hh:mm:ss";
  private final String TEST_DATE_FORMAT_2 = "dd-MM-yy, hh:mm";

  @Test
  public void testDateUtils() throws Exception {
    String date = "12/11/2002 12:06:47";

    Date result = DateUtils.getDateFromString(date, TEST_DATE_FORMAT);
    assertTrue(result.before(new Date(System.currentTimeMillis())));

    String newDate = DateUtils.getStringFromDate(result, TEST_DATE_FORMAT);
    assertEquals(date, newDate);

    String timestamp = DateUtils.formatTimeStamp(result, TEST_DATE_FORMAT_2);
    assertEquals("12-11-02, 12:06", timestamp);

    String newTimestamp = DateUtils.getTimeStamp(TEST_DATE_FORMAT_2);
    assertEquals(DateUtils.getStringFromDate(new Date(), TEST_DATE_FORMAT_2), newTimestamp);
  }

}
