/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import java.util.TimeZone;

import org.apache.commons.httpclient.Header;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class HttpMuleMessageToHttpResponseDateHeaderTestCase extends AbstractMuleTestCase
{
    private TimeZone savedTimeZone;

    @Before
    public void setUp()
    {
        savedTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("EST"));
    }

    @After
    public void tearDown()
    {
        TimeZone.setDefault(savedTimeZone);
    }

    @Test
    public void testDateHeaderFormat() throws Exception
    {
        MuleMessageToHttpResponse transformer = new MuleMessageToHttpResponse();
        HttpResponse response = new HttpResponse();
        transformer.initialise();

        DateTime dateTime = new DateTime(2005, 9, 5, 16, 30, 0, 0, DateTimeZone.forID("EST"));

        transformer.setDateHeader(response, dateTime.getMillis());

        for (Header header : response.getHeaders())
        {
            if (HttpConstants.HEADER_DATE.equals(header.getName()))
            {
                assertThat(header.getValue(), equalTo(getExpectedHeaderValue()));
            }
        }
    }

    protected String getExpectedHeaderValue()
    {
        return "Mon, 05 Sep 2005 21:30:00 +0000";
    }

}
