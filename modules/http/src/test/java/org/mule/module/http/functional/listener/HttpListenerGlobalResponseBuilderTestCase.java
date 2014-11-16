/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.module.http.api.HttpHeaders;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerGlobalResponseBuilderTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty globalResponseBuilderPath = new SystemProperty("globalResponseBuilderPath","globalResponseBuilderPath");
    @Rule
    public SystemProperty globalResponseBuilderCustomizedPath = new SystemProperty("globalResponseBuilderCustomizedPath","globalResponseBuilderCustomizedPath");
    @Rule
    public SystemProperty globalCompositeResponseBuilderPath = new SystemProperty("globalCompositeResponseBuilderPath","globalCompositeResponseBuilderPath");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-response-builder-global-config.xml";
    }

    @Test
    public void globalResponseBuilder() throws Exception
    {
        final String url = String.format("http://localhost:%s/%s", listenPort.getNumber(), globalResponseBuilderPath.getValue());
        testResponseHeaders(url, Arrays.asList("Mule 3.6.0"));
    }

    @Test
    public void globalResponseBuilderCustomized() throws Exception
    {
        final String url = String.format("http://localhost:%s/%s", listenPort.getNumber(), globalResponseBuilderCustomizedPath.getValue());
        testResponseHeaders(url, Arrays.asList("Mule 3.6.0", "Mule 3.7.0"));
    }

    @Test
    public void globalCompositeResponseBuilder() throws Exception
    {
        final String url = String.format("http://localhost:%s/%s", listenPort.getNumber(), globalCompositeResponseBuilderPath.getValue());
        testResponseHeaders(url, Arrays.asList("Mule 3.6.0", "Mule 3.7.0", "Mule 3.8.0"));
    }

    private void testResponseHeaders(String url, Collection<String> userAgentHeaderValues) throws IOException
    {
        final Response response = Request.Get(url).connectTimeout(1000).execute();
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(isDateValid(httpResponse.getFirstHeader(HttpHeaders.Names.DATE).getValue()), Is.is(true));
        final Header[] userAgentHeaders = httpResponse.getHeaders(HttpHeaders.Names.USER_AGENT);
        final Collection<String> headerValues = CollectionUtils.collect(Arrays.asList(userAgentHeaders), new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                Header header = (Header) input;
                return header.getValue();
            }
        });
        assertThat(userAgentHeaders.length, is(userAgentHeaderValues.size()));
        assertThat(headerValues, Matchers.containsInAnyOrder(userAgentHeaderValues.toArray(new String[userAgentHeaderValues.size()])));
    }


    public boolean isDateValid(String dateToValidate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            sdf.parse(dateToValidate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
