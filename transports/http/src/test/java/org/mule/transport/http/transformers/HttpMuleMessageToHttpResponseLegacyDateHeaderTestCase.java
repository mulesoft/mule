/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.tck.junit4.rule.BackwardsCompatibilityProperty;
import org.mule.transport.http.HttpConstants;

import org.junit.Before;
import org.junit.Rule;

public class HttpMuleMessageToHttpResponseLegacyDateHeaderTestCase extends HttpMuleMessageToHttpResponseDateHeaderTestCase
{

    private static final String EXPECTED_DATE_HEADER = "Mon, 05 Sep 2005 16:30:00 -0500";

    @Rule
    public BackwardsCompatibilityProperty property = new BackwardsCompatibilityProperty(HttpConstants.SERVER_TIME_ZONE_PROPERTY);

    @Before
    public void setUp()
    {
        property.switchOn();
    }

    @Override
    protected String getExpectedHeaderValue()
    {
        return EXPECTED_DATE_HEADER;
    }

}
