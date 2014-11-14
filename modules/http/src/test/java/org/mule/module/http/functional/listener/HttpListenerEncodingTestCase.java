/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpListenerEncodingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port = new DynamicPort("port");

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
        return "http-listener-encoding-config.xml";
    }

    @Test
    public void testEncoding() throws Exception
    {
        final String url = String.format("http://localhost:%s/test", port.getNumber());
        Request request = Request.Post(url).bodyString(testMessage, ContentType.create("text/plain", Charset.forName(encoding)));
        request.execute();
        MuleMessage result = muleContext.getClient().request("vm://out", 2000);
        assertThat(result.getPayloadAsString(), is(testMessage));
    }

}
