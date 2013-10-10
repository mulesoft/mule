/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SmallTest
public class DateUtilsTestCase extends AbstractMuleTestCase
{
    private final String TEST_DATE_FORMAT = "dd/MM/yyyy hh:mm:ss";
    private final String TEST_DATE_FORMAT_2 = "dd-MM-yy, hh:mm";

    @Test
    public void testDateUtils() throws Exception
    {
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
