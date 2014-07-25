/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.CustomTimeZone;

import org.apache.commons.httpclient.Header;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractDateHeaderTestCase extends AbstractMuleTestCase
{
    @Rule
    public final CustomTimeZone timeZone = new CustomTimeZone("EST");

    @Test
    public void testDateHeaderFormat() throws Exception
    {
        initialise();
        HttpResponse response = new HttpResponse();

        DateTime dateTime = new DateTime(2005, 9, 5, 16, 30, 0, 0, DateTimeZone.forID("EST"));

        setDateHeader(response, dateTime.getMillis());

        boolean headerFound = false;
        for (Header header : response.getHeaders())
        {
            if (HttpConstants.HEADER_DATE.equals(header.getName()))
            {
                headerFound = true;
                assertThat(header.getValue(), equalTo(getExpectedHeaderValue()));
            }
        }
        assertThat("Date header missing.", headerFound, equalTo(true));
    }

    //Set up the object to be tested
    protected abstract void initialise() throws Exception;
    //Perform the specific date header setting
    protected abstract void setDateHeader(HttpResponse response, long millis);
    //Set up the expected header outcome
    protected abstract String getExpectedHeaderValue();
}
