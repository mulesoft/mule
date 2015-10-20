/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.filter;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class RegexFilterTestCase extends FunctionalTestCase
{

    Map<String, String> payloadMap;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/filter/regex-test.xml";
    }

    @Before
    public void setUp()
    {
        payloadMap = new HashMap<>();
        payloadMap.put("goodValue", "code with the mules");
        payloadMap.put("wrongValue", "code with the zebras");
    }

    @Test
    public void matchesUsingDefaultPayload() throws Exception
    {
        MuleEvent event = runFlow("default-payload-value", "run with the mules");
        assertThat(event, is(notNullValue()));
    }

    @Test
    public void notMatchesUsingDefaultPayload() throws Exception
    {
        MuleEvent event = runFlow("default-payload-value", "run with the zebras");
        assertThat(event, is(nullValue()));
    }

    @Test
    public void matchesConfiguringValue() throws Exception
    {
        MuleEvent event = runFlow("matches-configuring-value", payloadMap);
        assertThat(event, is(notNullValue()));
    }

    @Test
    public void notMatchesConfiguringValue() throws Exception
    {
        MuleEvent event = runFlow("not-matches-configuring-value", payloadMap);
        assertThat(event, is(nullValue()));
    }

    @Test
    public void notMatchesConfiguringNonStringValue() throws Exception
    {
        payloadMap.remove("goodValue");
        MuleEvent event = runFlow("not-matches-configuring-non-string-value", payloadMap);
        assertThat(event, is(nullValue()));
    }

    @Test
    public void matchesConfiguringPlainTextValue() throws Exception
    {
        MuleEvent event = runFlow("matches-configuring-plain-text-value");
        assertThat(event, is(notNullValue()));
    }

}
