/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.listener;

import static com.google.common.base.Charsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.module.http.functional.matcher.ParamMapMatcher.isEqual;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.functional.AbstractHttpTestCase;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.ParameterMap;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.net.URLDecoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerUrlEncodedTestCase extends AbstractHttpTestCase
{

    public static final String PARAM_1_NAME = "param1";
    public static final String PARAM_2_NAME = "param2";
    public static final String PARAM_1_VALUE = "param1Value";
    public static final String PARAM_2_VALUE = "param2Value";
    public static final String PARAM_2_VALUE_1 = "param2Value1";
    public static final String PARAM_2_VALUE_2 = "param2Value2";
    public static final String OUT_QUEUE_URL = "test://out";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty path = new SystemProperty("path", "path");


    @Override
    protected String getConfigFile()
    {
        return "http-listener-url-encoded-config.xml";
    }

    @Test
    public void urlEncodedParamsGenerateAMapPayload() throws Exception
    {
        final Response response = Request.Post(getListenerUrl())
                .bodyForm(new BasicNameValuePair(PARAM_1_NAME, PARAM_1_VALUE),
                          new BasicNameValuePair(PARAM_2_NAME, PARAM_2_VALUE)).execute();
        final MuleMessage receivedMessage = muleContext.getClient().request(OUT_QUEUE_URL, 1000);
        assertThat(receivedMessage.getPayload(), instanceOf(ParameterMap.class));
        ParameterMap payloadAsMap = (ParameterMap) receivedMessage.getPayload();
        assertThat(payloadAsMap.size(), is(2));
        assertThat(payloadAsMap.get(PARAM_1_NAME), is(PARAM_1_VALUE));
        assertThat(payloadAsMap.get(PARAM_2_NAME), is(PARAM_2_VALUE));

        compareParameterMaps(response, payloadAsMap);
    }

    @Test
    public void invalidUrlEncodedParamsReturnInvalidRequestStatusCode() throws Exception
    {
        final Response response = Request.Post(getListenerUrl())
                .body(new StringEntity("Invalid url encoded content"))
                .addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
                .execute();

        final HttpResponse httpResponse = response.returnResponse();

        assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));

        assertThat(URLDecoder.decode(IOUtils.toString(httpResponse.getEntity().getContent()), UTF_8.name()), is("Invalid url encoded content"));
    }

    @Test
    public void urlEncodedMultiValueParamsHasOldValues() throws Exception
    {
        final Response response = Request.Post(getListenerUrl())
                .bodyForm(new BasicNameValuePair(PARAM_1_NAME, PARAM_1_VALUE),
                          new BasicNameValuePair(PARAM_2_NAME, PARAM_2_VALUE_1),
                          new BasicNameValuePair(PARAM_2_NAME, PARAM_2_VALUE_2)).execute();
        final MuleMessage receivedMessage = muleContext.getClient().request(OUT_QUEUE_URL, 1000);
        assertThat(receivedMessage.getPayload(), instanceOf(ParameterMap.class));
        ParameterMap payloadAsMap = (ParameterMap) receivedMessage.getPayload();
        assertThat(payloadAsMap.size(), is(2));
        assertThat(payloadAsMap.get(PARAM_1_NAME), is(PARAM_1_VALUE));
        assertThat(payloadAsMap.getAll(PARAM_2_NAME).size(), is(2));
        assertThat(payloadAsMap.getAll(PARAM_2_NAME).get(0), is(PARAM_2_VALUE_1));
        assertThat(payloadAsMap.getAll(PARAM_2_NAME).get(1), is(PARAM_2_VALUE_2));

        compareParameterMaps(response, payloadAsMap);
    }

    @Test
    public void urlEncodedEmptyParamsGenerateANullPayload() throws Exception
    {
        final Response response = Request.Post(getListenerUrl()).execute();
        assertNullPayloadAndEmptyResponse(response);
    }

    @Test
    public void urlEncodedEmptyParamsUrlEncodedContentTypeGenerateANullPayload() throws Exception
    {
        final Response response = Request.Post(getListenerUrl())
                .addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
                .execute();

        assertNullPayloadAndEmptyResponse(response);
    }

    private void assertNullPayloadAndEmptyResponse(Response response) throws Exception
    {
        final MuleMessage receivedMessage = muleContext.getClient().request(OUT_QUEUE_URL, 1000);
        assertThat(receivedMessage.getPayload(), instanceOf(NullPayload.class));

        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getFirstHeader(CONTENT_LENGTH).getValue(), Is.is("0"));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(StringUtils.EMPTY));
    }

    private void compareParameterMaps(Response response, ParameterMap payloadAsMap) throws IOException
    {
        final HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getFirstHeader(CONTENT_TYPE).getValue(), startsWith(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString()));
        final String responseContent = IOUtils.toString(httpResponse.getEntity().getContent());
        assertThat(payloadAsMap, isEqual(HttpParser.decodeUrlEncodedBody(responseContent, UTF_8).toListValuesMap()));
    }

    private String getListenerUrl()
    {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), path.getValue());
    }

}