/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_RELATIVE_PATH;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY;
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
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerResponseBuilderTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty emptyResponseBuilderPath = new SystemProperty("emptyResponseBuilderPath","emptyResponseBuilderPath");
    @Rule
    public SystemProperty statusResponseBuilderPath = new SystemProperty("statusResponseBuilderPath","statusResponseBuilderPath");
    @Rule
    public SystemProperty statusResponseBuilderOverridePath = new SystemProperty("statusResponseBuilderOverridePath", "statusResponseBuilderOverridePath");
    @Rule
    public SystemProperty reasonPhraseResponseBuilderOverridePath = new SystemProperty("reasonPhraseResponseBuilderOverridePath", "reasonPhraseResponseBuilderOverridePath");
    @Rule
    public SystemProperty statusReasonPhraseResponseBuilderWontOverridePath = new SystemProperty("statusReasonPhraseResponseBuilderWontOverridePath", "statusReasonPhraseResponseBuilderWontOverridePath");
    @Rule
    public SystemProperty headerResponseBuilderPath = new SystemProperty("headerResponseBuilderPath","headerResponseBuilderPath");
    @Rule
    public SystemProperty headersResponseBuilderPath = new SystemProperty("headersResponseBuilderPath","headersResponseBuilderPath");
    @Rule
    public SystemProperty headerDuplicatesResponseBuilderPath = new SystemProperty("headerDuplicatesResponseBuilderPath","headerDuplicatesResponseBuilderPath");
    @Rule
    public SystemProperty errorEmptyResponseBuilderPath = new SystemProperty("emptyResponseBuilderPath","emptyResponseBuilderPath");
    @Rule
    public SystemProperty errorStatusResponseBuilderPath = new SystemProperty("statusResponseBuilderPath","statusResponseBuilderPath");
    @Rule
    public SystemProperty errorHeaderResponseBuilderPath = new SystemProperty("headerResponseBuilderPath","headerResponseBuilderPath");
    @Rule
    public SystemProperty errorHeadersResponseBuilderPath = new SystemProperty("headersResponseBuilderPath","headersResponseBuilderPath");
    @Rule
    public SystemProperty errorHeaderDuplicatesResponseBuilderPath = new SystemProperty("headerDuplicatesResponseBuilderPath","headerDuplicatesResponseBuilderPath");
    @Rule
    public SystemProperty responseBuilderAndErrorResponseBuilderNotTheSamePath = new SystemProperty("responseBuilderAndErrorResponseBuilderNotTheSamePath","responseBuilderAndErrorResponseBuilderNotTheSamePath");
    @Rule
    public SystemProperty httpHeadersResponseBuilderPath = new SystemProperty("httpHeadersResponseBuilderPath","httpHeadersResponseBuilderPath");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-response-builder-config.xml";
    }

    @Test
    public void emptyResponseBuilder() throws Exception
    {
        final String url = getUrl(emptyResponseBuilderPath);
        emptyResponseBuilderTest(url);
    }

    @Test
    public void statusLineResponseBuilder() throws Exception
    {
        final String url = getUrl(statusResponseBuilderPath);
        statusLineResponseBuilderTest(url, 201);
    }

    @Test
    public void statusLineWithCodeOverrideResponseBuilder() throws Exception
    {
        final String url = getUrl(statusResponseBuilderOverridePath);
        statusLineResponseBuilderTest(url, 202);
    }

    @Test
    public void reasonPhraseResponseBuilderOverrideResponseBuilder() throws Exception
    {
        final String url = getUrl(reasonPhraseResponseBuilderOverridePath);
        statusLineResponseBuilderTest(url, 200, "response success!");
    }

    @Test
    public void statusReasonPhraseResponseBuilderWontOverrideResponseBuilder() throws Exception
    {
        final String url = getUrl(statusReasonPhraseResponseBuilderWontOverridePath);
        statusLineResponseBuilderTest(url, 200, "OK");
    }

    @Test
    public void headerResponseBuilder() throws Exception
    {
        final String url = getUrl(headerResponseBuilderPath);
        simpleHeaderTest(url);
    }

    @Test
    public void headersResponseBuilder() throws Exception
    {
        final String url = getUrl(headersResponseBuilderPath);
        simpleHeaderTest(url);
    }

    @Test
    public void headerWithDuplicatesResponseBuilder() throws Exception
    {
        final String url = getUrl(headerDuplicatesResponseBuilderPath);
        headersWithDuplicatesResponseBuilderTest(url);
    }

    @Test
    public void errorEmptyResponseBuilder() throws Exception
    {
        final String url = getUrl(errorEmptyResponseBuilderPath);
        emptyResponseBuilderTest(url);
    }

    @Test
    public void errorStatusLineResponseBuilder() throws Exception
    {
        final String url = getUrl(errorStatusResponseBuilderPath);
        statusLineResponseBuilderTest(url, 201);
    }

    @Test
    public void errorHeaderResponseBuilder() throws Exception
    {
        final String url = getUrl(errorHeaderResponseBuilderPath);
        simpleHeaderTest(url);
    }

    @Test
    public void errorHeadersResponseBuilder() throws Exception
    {
        final String url = getUrl(errorHeadersResponseBuilderPath);
        simpleHeaderTest(url);
    }

    @Test
    public void errorHeaderWithDuplicatesResponseBuilder() throws Exception
    {
        final String url = getUrl(errorHeaderDuplicatesResponseBuilderPath);
        headersWithDuplicatesResponseBuilderTest(url);
    }

    @Test
    public void responseBuilderIsDifferentFromErrorResponseBuilder() throws Exception
    {
        final String url = getUrl(responseBuilderAndErrorResponseBuilderNotTheSamePath);
        final Response successfulResponse = Request.Get(url).connectTimeout(100000).socketTimeout(10000000).execute();
        assertThat(successfulResponse.returnResponse().getStatusLine().getStatusCode(), is(202));
        final Response failureResponse = Request.Get(url).addHeader("FAIL", "true").connectTimeout(100000).socketTimeout(100000).execute();
        assertThat(failureResponse.returnResponse().getStatusLine().getStatusCode(), is(505));
    }

    @Test
    public void httpHeadersResponseBuilder() throws Exception
    {
        final String url = getUrl(httpHeadersResponseBuilderPath);
        final Response response = Request.Get(url).connectTimeout(1000).execute();
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getFirstHeader(HTTP_RELATIVE_PATH).getValue(), is("relativePath"));
        Header[] requestPathHeaders = httpResponse.getHeaders(HTTP_REQUEST_PATH_PROPERTY);
        assertThat(requestPathHeaders, is(arrayWithSize(2)));
        assertThat(requestPathHeaders[0].getValue(), isOneOf("requestPath1", "requestPath2"));
        assertThat(requestPathHeaders[1].getValue(), isOneOf("requestPath1", "requestPath2"));
    }

    private String getUrl(SystemProperty pathSystemProperty)
    {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), pathSystemProperty.getValue());
    }

    private void statusLineResponseBuilderTest(String url, int expectedStatus, String expectedReasonPhrase) throws IOException
    {
        final Response response = Request.Get(url).connectTimeout(1000).execute();
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getAllHeaders().length, is(2));
        assertThat(httpResponse.getFirstHeader(HttpHeaders.Names.CONTENT_LENGTH).getValue(), is("0"));
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(expectedStatus));
        assertThat(httpResponse.getStatusLine().getReasonPhrase(), is(expectedReasonPhrase));
    }

    private void statusLineResponseBuilderTest(String url, int expectedStatus) throws IOException
    {
        statusLineResponseBuilderTest(url, expectedStatus, "everything works!");
    }

    private void emptyResponseBuilderTest(String url) throws IOException
    {
        final Response response = Request.Get(url).connectTimeout(1000).execute();
        assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(200));
    }

    private void headersWithDuplicatesResponseBuilderTest(String url) throws IOException
    {
        final Response response = Request.Get(url).connectTimeout(1000).execute();
        final HttpResponse httpResponse = response.returnResponse();
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
        assertThat(userAgentHeaders.length, is(5));
        assertThat(headerValues, Matchers.containsInAnyOrder(Arrays.asList("Mule 3.5.0", "Mule 3.6.0", "Mule 3.7.0", "Mule 3.8.0", "Mule 3.9.0").toArray(new String[4])));
    }

    private void simpleHeaderTest(String url) throws IOException
    {
        final Response response = Request.Get(url).connectTimeout(1000).execute();
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getFirstHeader(HttpHeaders.Names.USER_AGENT).getValue(), is("Mule 3.6.0"));
        assertThat(isDateValid(httpResponse.getFirstHeader(HttpHeaders.Names.DATE).getValue()), is(true));
    }

    public boolean isDateValid(String dateToValidate){
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
