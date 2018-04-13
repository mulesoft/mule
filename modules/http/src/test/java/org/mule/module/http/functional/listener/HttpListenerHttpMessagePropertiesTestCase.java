/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.apache.http.client.fluent.Request.Post;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_REMOTE_ADDRESS;
import static org.mule.module.http.api.HttpHeaders.Names.X_CORRELATION_ID;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.HttpProtocol;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHttpMessagePropertiesTestCase extends FunctionalTestCase
{

    public static final String QUERY_PARAM_NAME = "queryParam";
    public static final String QUERY_PARAM_VALUE = "paramValue";
    public static final String QUERY_PARAM_VALUE_WITH_SPACES = "param Value";
    public static final String QUERY_PARAM_SECOND_VALUE = "paramAnotherValue";
    public static final String SECOND_QUERY_PARAM_NAME = "queryParam2";
    public static final String SECOND_QUERY_PARAM_VALUE = "paramValue2";
    public static final String CONTEXT_PATH = "/context/path";
    public static final String API_CONTEXT_PATH = "/api" + CONTEXT_PATH;
    public static final String BASE_PATH = "/";

    private static final String FIRST_URI_PARAM_NAME = "uri-param1";
    private static final String SECOND_URI_PARAM_NAME = "uri-param2";
    private static final String THIRD_URI_PARAM_NAME = "uri-param3";
    public static final String FIRST_URI_PARAM = "uri-param-value-1";
    public static final String SECOND_URI_PARAM_VALUE = "uri-param-value-2";
    public static final String THIRD_URI_PARAM_VALUE = "uri-param-value-3";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port1");

    @Rule
    public DynamicPort listenBasePort = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-message-properties-config.xml";
    }

    @Test
    public void get() throws Exception
    {
        final String url = String.format("http://localhost:%s", listenPort.getNumber());
        Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_URI), is(BASE_PATH));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY), is(BASE_PATH));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_RELATIVE_PATH), is(BASE_PATH));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_QUERY_STRING), is(""));
        assertThat(message.getInboundProperty(HttpConstants.RequestProperties.HTTP_URI_PARAMS), notNullValue());
        assertThat(message.<Map>getInboundProperty(HttpConstants.RequestProperties.HTTP_URI_PARAMS).isEmpty(), is(true));
        final Map queryParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS);
        assertThat(queryParams, IsNull.notNullValue());
        assertThat(queryParams.size(), is(0));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY), is("GET"));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_VERSION_PROPERTY), is(HttpProtocol.HTTP_1_1.asString()));
        assertThat(message.<String>getInboundProperty(HTTP_REMOTE_ADDRESS), is(CoreMatchers.notNullValue()));
    }

    @Test
    public void getWithQueryParams() throws Exception
    {
        final ImmutableMap<String, Object> queryParams = ImmutableMap.<String, Object>builder()
                .put(QUERY_PARAM_NAME, QUERY_PARAM_VALUE)
                .put(SECOND_QUERY_PARAM_NAME, SECOND_QUERY_PARAM_VALUE)
                .build();
        final String uri = "/?" + buildQueryString(queryParams);
        final String url = String.format("http://localhost:%s" + uri, listenPort.getNumber());
        Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_URI), is(uri));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY), is(BASE_PATH));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_RELATIVE_PATH), is(BASE_PATH));
        Map<String, String> retrivedQueryParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS);
        assertThat(retrivedQueryParams, IsNull.notNullValue());
        assertThat(retrivedQueryParams.size(), is(2));
        assertThat(retrivedQueryParams.get(QUERY_PARAM_NAME), is(QUERY_PARAM_VALUE));
        assertThat(retrivedQueryParams.get(SECOND_QUERY_PARAM_NAME), is(SECOND_QUERY_PARAM_VALUE));
    }

    @Test
    public void getWithQueryParamMultipleValues() throws Exception
    {
        final ImmutableMap<String, Object> queryParams = ImmutableMap.<String, Object>builder()
                .put(QUERY_PARAM_NAME, Arrays.asList(QUERY_PARAM_VALUE,QUERY_PARAM_SECOND_VALUE))
                .build();
        final String url = String.format("http://localhost:%s/?" + buildQueryString(queryParams), listenPort.getNumber());
        Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        ParameterMap retrivedQueryParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS);
        assertThat(retrivedQueryParams, IsNull.notNullValue());
        assertThat(retrivedQueryParams.size(), is(1));
        assertThat(retrivedQueryParams.get(QUERY_PARAM_NAME), is(QUERY_PARAM_SECOND_VALUE));
        assertThat(retrivedQueryParams.getAll(QUERY_PARAM_NAME).size(), is(2));
        assertThat(retrivedQueryParams.getAll(QUERY_PARAM_NAME), Matchers.containsInAnyOrder(new String[] {QUERY_PARAM_VALUE, QUERY_PARAM_SECOND_VALUE}));
    }

    @Test
    public void postWithEncodedValues() throws Exception
    {
        final ImmutableMap<String, Object> queryParams = ImmutableMap.<String, Object>builder()
                .put(QUERY_PARAM_NAME, QUERY_PARAM_VALUE_WITH_SPACES)
                .build();
        final String url = String.format("http://localhost:%s/?" + buildQueryString(queryParams), listenPort.getNumber());
        Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        ParameterMap retrivedQueryParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_QUERY_PARAMS);
        assertThat(retrivedQueryParams, IsNull.notNullValue());
        assertThat(retrivedQueryParams.size(), is(1));
        assertThat(retrivedQueryParams.get(QUERY_PARAM_NAME), is(QUERY_PARAM_VALUE_WITH_SPACES));
    }

    @Test
    public void putWithOldProtocol() throws Exception
    {
        final ImmutableMap<String, Object> queryParams = ImmutableMap.<String, Object>builder()
                .put(QUERY_PARAM_NAME, Arrays.asList(QUERY_PARAM_VALUE,QUERY_PARAM_VALUE))
                .build();
        final String url = String.format("http://localhost:%s/?" + buildQueryString(queryParams), listenPort.getNumber());
        Request.Put(url).version(HttpVersion.HTTP_1_0).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY), is("PUT"));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_VERSION_PROPERTY), is(HttpProtocol.HTTP_1_0.asString()));
    }

    @Test
    public void getFullUriAndPath() throws Exception
    {
        final String url = String.format("http://localhost:%s%s", listenPort.getNumber(), CONTEXT_PATH);
        Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_URI), is(CONTEXT_PATH));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY), is(CONTEXT_PATH));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_RELATIVE_PATH), is(CONTEXT_PATH));
    }

    @Test
    public void getAllUriParams() throws Exception
    {
        final String url = String.format("http://localhost:%s/%s/%s/%s", listenPort.getNumber(), FIRST_URI_PARAM, SECOND_URI_PARAM_VALUE, THIRD_URI_PARAM_VALUE);
        Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        ParameterMap uriParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_URI_PARAMS);
        assertThat(uriParams, IsNull.notNullValue());
        assertThat(uriParams.size(), is(3));
        assertThat(uriParams.get(FIRST_URI_PARAM_NAME), is(FIRST_URI_PARAM));
        assertThat(uriParams.get(SECOND_URI_PARAM_NAME), is(SECOND_URI_PARAM_VALUE));
        assertThat(uriParams.get(THIRD_URI_PARAM_NAME), is(THIRD_URI_PARAM_VALUE));
    }

    @Test
    public void getUriParamInTheMiddle() throws Exception
    {
        final String url = String.format("http://localhost:%s/some-path/%s/some-other-path", listenPort.getNumber(), FIRST_URI_PARAM);
        Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        ParameterMap uriParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_URI_PARAMS);
        assertThat(uriParams, IsNull.notNullValue());
        assertThat(uriParams.size(), is(1));
        assertThat(uriParams.get(FIRST_URI_PARAM_NAME), is(FIRST_URI_PARAM));
    }

    @Test
    public void postUriParamEncoded() throws Exception
    {
        final String uriParamValue = "uri param value";
        final String uriParamValueEncoded = URLEncoder.encode(uriParamValue, Charsets.UTF_8.displayName());
        final String url = String.format("http://localhost:%s/some-path/%s/some-other-path", listenPort.getNumber(), uriParamValueEncoded);
        Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        ParameterMap uriParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_URI_PARAMS);
        assertThat(uriParams, notNullValue());
        assertThat(uriParams.size(), is(1));
        assertThat(uriParams.get(FIRST_URI_PARAM_NAME), is(uriParamValue));
    }

    @Test
    public void muleCorrelationIdHeader() throws Exception
    {
        final String url = String.format("http://localhost:%s/some-path", listenPort.getNumber());

        final String myCorrelationId = "myCorrelationId";
        Post(url).addHeader(MULE_CORRELATION_ID_PROPERTY, myCorrelationId).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.getCorrelationId(), is(myCorrelationId));
        String header = message.getInboundProperty(MULE_CORRELATION_ID_PROPERTY);
        assertThat(header, is(myCorrelationId));
    }

    @Test
    public void xCorrelationIdHeader() throws Exception
    {
        final String url = String.format("http://localhost:%s/some-path", listenPort.getNumber());

        final String myCorrelationId = "myCorrelationId";
        Post(url).addHeader(X_CORRELATION_ID, myCorrelationId).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.getCorrelationId(), is(myCorrelationId));
        String header = message.getInboundProperty(X_CORRELATION_ID);
        assertThat(header, is(myCorrelationId));
    }

    @Test
    public void muleOverridesXCorrelationIdHeader() throws Exception
    {
        final String url = String.format("http://localhost:%s/some-path", listenPort.getNumber());

        final String myCorrelationId = "myCorrelationId";
        final String myOtherCorrelationId = "myOtherCorrelationId";
        Post(url)
          .addHeader(X_CORRELATION_ID, myCorrelationId)
          .addHeader(MULE_CORRELATION_ID_PROPERTY, myOtherCorrelationId)
          .connectTimeout(RECEIVE_TIMEOUT)
          .execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.getCorrelationId(), is(myOtherCorrelationId));
        String header = message.getInboundProperty(X_CORRELATION_ID);
        assertThat(header, is(myCorrelationId));
        String muleHeader = message.getInboundProperty(MULE_CORRELATION_ID_PROPERTY);
        assertThat(muleHeader, is(myOtherCorrelationId));
    }

    @Test
    public void xForwardedForHeader() throws Exception
    {
        final String url = String.format("http://localhost:%s/some-path", listenPort.getNumber());

        Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.<String>getInboundProperty(HTTP_REMOTE_ADDRESS), startsWith("/127.0.0.1:"));
        assertThat(message.<String> getInboundProperty(HttpHeaders.Names.X_FORWARDED_FOR), nullValue());

        Post(url)
                 .addHeader(HttpHeaders.Names.X_FORWARDED_FOR, "clientIp, proxy1Ip")
                .connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage forwardedMessage = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(forwardedMessage.<String> getInboundProperty(HTTP_REMOTE_ADDRESS), startsWith("/127.0.0.1:"));
        assertThat(forwardedMessage.<String> getInboundProperty(HttpHeaders.Names.X_FORWARDED_FOR), is("clientIp, proxy1Ip"));
    }

    @Test
    public void getBasePath() throws Exception
    {
        final String url = String.format("http://localhost:%s%s", listenBasePort.getNumber(), API_CONTEXT_PATH);
        Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        final MuleMessage message = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_LISTENER_PATH), is("/api/*"));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY), is(API_CONTEXT_PATH));
        assertThat(message.<String>getInboundProperty(HttpConstants.RequestProperties.HTTP_RELATIVE_PATH), is(CONTEXT_PATH));
        ParameterMap uriParams = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_URI_PARAMS);
        assertThat(uriParams, notNullValue());
        assertThat(uriParams.isEmpty(), is(true));
    }

    public String buildQueryString(Map<String, Object> queryParams) throws UnsupportedEncodingException
    {
        final StringBuilder queryString = new StringBuilder();
        for (String paramName : queryParams.keySet())
        {
            final Object value = queryParams.get(paramName);
            if (value instanceof Collection)
            {
                for (java.lang.Object eachValue : (Collection)value)
                {
                    queryString.append(paramName + "=" + URLEncoder.encode(eachValue.toString(), Charset.defaultCharset().name()));
                    queryString.append("&");
                }
            }
            else
            {
                queryString.append(paramName + "=" + URLEncoder.encode(value.toString(), Charset.defaultCharset().name()));
                queryString.append("&");
            }
        }
        queryString.deleteCharAt(queryString.length() - 1);
        return queryString.toString();
    }

}
