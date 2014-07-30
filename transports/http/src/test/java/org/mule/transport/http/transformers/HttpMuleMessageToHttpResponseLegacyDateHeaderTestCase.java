/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.http.HttpConstants;

import org.junit.Rule;

/**
 *
 */
public class HttpMuleMessageToHttpResponseLegacyDateHeaderTestCase extends HttpMuleMessageToHttpResponseDateHeaderTestCase
{
    @Rule
    public SystemProperty systemProperty = new SystemProperty(HttpConstants.SERVER_TIME_ZONE_PROPERTY.getPropertyName(), "true");

    @Override
    protected String getExpectedHeaderValue()
    {
        return "Mon, 05 Sep 2005 16:30:00 -0500";
    }
}
