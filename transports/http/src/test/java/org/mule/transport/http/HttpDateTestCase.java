/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class HttpDateTestCase extends AbstractMuleTestCase
{
    private DateTimeZone savedDateTimeZone;

    @Before
    public void initDefaultDateTimeZone()
    {
        savedDateTimeZone = DateTimeZone.getDefault();

        DateTimeZone pst = DateTimeZone.forID("America/Los_Angeles");
        DateTimeZone.setDefault(pst);
    }

    @After
    public void restoreDefaultDateTimeZone()
    {
        DateTimeZone.setDefault(savedDateTimeZone);
    }

    @Test
    public void testParsingBackwardsCompatibility()
    {
        String originalFormat = "EEE, dd MMM yyyy hh:mm:ss zzz";
        SimpleDateFormat originalFormatter = new SimpleDateFormat(originalFormat, Locale.US);

        String[] dates = new String[] {
            "Tue, 15 Nov 1994 08:12:31 GMT",
            "Tue, 15 Nov 1994 08:12:31 -0300",
            "Tue, 15 Nov 1994 08:12:31 +0400",
            "Tue, 15 Nov 1994 08:12:31 +0230",
            "Tue, 15 Nov 1994 08:12:31 PST",
            "Tue, 15 Jun 1994 08:12:31 PDT",
            "Tue, 15 Nov 2013 00:00:00 EST",
            "Tue, 15 Nov 2013 23:59:59 EST",
            "Thu, 28 Mar 2013 13:15:40 -0700"
        };
        //, "Tue, 15 Nov 2013 23:12:31 UT" >> UT fails to parse in original parser even though it is valid in rfc822

        for (String date : dates)
        {
            long originalMillis = 0;
            try
            {
                originalMillis = originalFormatter.parse(date).getTime();
            }
            catch (ParseException e)
            {
                fail("Parsing failed with original parser: " + date);
            }

            String newDateStr = MuleMessageToHttpResponse.formatDate(originalMillis);
            try
            {
                // validate the original parser can still parse the new date format & the dates match.
                long newMillis = originalFormatter.parse(newDateStr).getTime();
                assertEquals("Dates don't match for date: " + date, originalMillis, newMillis);
            }
            catch (ParseException e)
            {
                fail("Old formatter failed to parse output of new formatter so it isn't backwards compatible. original: " + date + " new: " + newDateStr);
            }
        }
    }
}
