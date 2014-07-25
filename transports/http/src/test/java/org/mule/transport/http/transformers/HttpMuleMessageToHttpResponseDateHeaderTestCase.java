/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.transport.http.AbstractDateHeaderTestCase;
import org.mule.transport.http.HttpResponse;

public class HttpMuleMessageToHttpResponseDateHeaderTestCase extends AbstractDateHeaderTestCase
{

    private static final String EXPECTED_DATE_HEADER = "Mon, 05 Sep 2005 21:30:00 +0000";
    private MuleMessageToHttpResponse transformer;

    @Override
    protected void initialise() throws Exception
    {
        transformer = new MuleMessageToHttpResponse();
        transformer.initialise();
    }

    @Override
    protected void setDateHeader(HttpResponse response, long millis)
    {
        transformer.setDateHeader(response, millis);
    }

    @Override
    protected String getExpectedHeaderValue()
    {
        return EXPECTED_DATE_HEADER;
    }

}
