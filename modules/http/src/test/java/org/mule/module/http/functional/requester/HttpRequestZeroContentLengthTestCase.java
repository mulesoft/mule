/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.module.http.internal.request.grizzly.GrizzlyHttpClient.AVOID_ZERO_CONTENT_LENGTH;

import org.junit.runners.Parameterized.Parameters;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.NullPayload;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpRequestZeroContentLengthTestCase extends FunctionalTestCase
{

    @Rule
    public SystemProperty avoidZeroContentLengthProperty;

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    private final String expectedContentLength;

    public HttpRequestZeroContentLengthTestCase(String avoidZeroContentLength, String expectedContentLength)
    {
        avoidZeroContentLengthProperty = new SystemProperty(AVOID_ZERO_CONTENT_LENGTH, avoidZeroContentLength);
        this.expectedContentLength = expectedContentLength;
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        return asList(
                new Object[][]
                        {
                                {"true", ""},
                                {"false", "0"}
                        });
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-content-length-config.xml";
    }

    @Test
    public void emptyPayload() throws Exception
    {
        MuleEvent result = runFlow("testContentLengthClient", getTestEvent(NullPayload.getInstance()));
        assertThat(result.getMessage().getPayloadAsString(), is(expectedContentLength));
    }

}
