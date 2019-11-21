/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import com.ning.http.client.multipart.PartBase;

public class HttpRequesterPartsTestCase extends FunctionalTestCase
{

    private static final String HEADERS_CHARSET = "HEADERS_CHARSET";

    private static final String MIME_UTF_PROPERTY = "mail.mime.allowutf8";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Rule
    public SystemProperty encodingListener = new SystemProperty(MIME_UTF_PROPERTY, "true");

    @Before
    public void before() throws Exception
    {
        Field headersCharset = PartBase.class.getDeclaredField(HEADERS_CHARSET);
        headersCharset.setAccessible(true);
        headersCharset.set(null, UTF_8);
    }

    @Override
    protected String getConfigFile()
    {
        return "http-requester-parts-config.xml";
    }

    @Test
    public void utf8InHeaders() throws Exception
    {

        try (CloseableHttpClient httpClient = HttpClients.createDefault())
        {
            HttpGet httpGet = new HttpGet("http://localhost:" + listenPort.getValue() + "/utf");
            try (CloseableHttpResponse response = httpClient.execute(httpGet))
            {
                assertThat(IOUtils.toString(response.getEntity().getContent()), is("Hello"));
            }
        }
    }

}
