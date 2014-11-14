/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.api.HttpHeaders;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpRequestEncodingTestCase extends AbstractHttpRequestTestCase
{
    private static final String JAPANESE_MESSAGE = "\u3042";
    private static final String ARABIC_MESSAGE = "\u0634";
    private static final String CYRILLIC_MESSAGE = "\u0416";
    private static final String SIMPLE_MESSAGE = "A";

    @Parameterized.Parameter(0)
    public String encoding;

    @Parameterized.Parameter(1)
    public String testMessage;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"EUC-JP", JAPANESE_MESSAGE},
                {"Windows-31J", JAPANESE_MESSAGE},
                {"ISO-2022-JP", JAPANESE_MESSAGE},
                {"UTF-8", JAPANESE_MESSAGE},
                {"Arabic", ARABIC_MESSAGE},
                {"Windows-1256", ARABIC_MESSAGE},
                {"Windows-1251", CYRILLIC_MESSAGE},
                {"Cyrillic", CYRILLIC_MESSAGE},
                {"US-ASCII", SIMPLE_MESSAGE}});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-encoding-config.xml";
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, String.format("text/plain; charset=%s", encoding));
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(testMessage);
    }

    @Test
    public void testEncoding() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("encodingTest");
        MuleEvent result = flow.process(getTestEvent(TEST_MESSAGE));
        assertThat(result.getMessage().getPayloadAsString(), is(testMessage));
    }

}