/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.apache.http.client.fluent.Request.Post;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.X_FORWARDED_FOR;
import static org.mule.service.http.api.domain.HttpProtocol.HTTP_1_1;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.service.http.api.domain.HttpProtocol;
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
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHttpMessagePropertiesTestCase extends AbstractHttpTestCase {

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
  protected String getConfigFile() {
    return "http-listener-message-properties-config.xml";
  }

  @Test
  public void get() throws Exception {
    final String url = String.format("http://localhost:%s", listenPort.getNumber());
    Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    assertThat(attributes.getRequestUri(), is(BASE_PATH));
    assertThat(attributes.getRequestPath(), is(BASE_PATH));
    assertThat(attributes.getRelativePath(), is(BASE_PATH));
    assertThat(attributes.getQueryString(), is(""));
    assertThat(attributes.getUriParams(), notNullValue());
    assertThat(attributes.getUriParams().isEmpty(), is(true));
    final Map queryParams = attributes.getQueryParams();
    assertThat(queryParams, notNullValue());
    assertThat(queryParams.size(), is(0));
    assertThat(attributes.getMethod(), is("GET"));
    assertThat(attributes.getVersion(), is(HTTP_1_1.asString()));
    assertThat(attributes.getRemoteAddress(), is(notNullValue()));
  }

  @Test
  public void getWithQueryParams() throws Exception {
    final ImmutableMap<String, Object> queryParams = ImmutableMap.<String, Object>builder()
        .put(QUERY_PARAM_NAME, QUERY_PARAM_VALUE).put(SECOND_QUERY_PARAM_NAME, SECOND_QUERY_PARAM_VALUE).build();
    final String uri = "/?" + buildQueryString(queryParams);
    final String url = String.format("http://localhost:%s" + uri, listenPort.getNumber());
    Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    assertThat(attributes.getRequestUri(), is(uri));
    assertThat(attributes.getRequestPath(), is(BASE_PATH));
    assertThat(attributes.getRelativePath(), is(BASE_PATH));
    Map<String, String> retrivedQueryParams = attributes.getQueryParams();
    assertThat(retrivedQueryParams, notNullValue());
    assertThat(retrivedQueryParams.size(), is(2));
    assertThat(retrivedQueryParams.get(QUERY_PARAM_NAME), is(QUERY_PARAM_VALUE));
    assertThat(retrivedQueryParams.get(SECOND_QUERY_PARAM_NAME), is(SECOND_QUERY_PARAM_VALUE));
  }

  @Test
  public void getWithQueryParamMultipleValues() throws Exception {
    final ImmutableMap<String, Object> queryParams = ImmutableMap.<String, Object>builder()
        .put(QUERY_PARAM_NAME, Arrays.asList(QUERY_PARAM_VALUE, QUERY_PARAM_SECOND_VALUE)).build();
    final String url = String.format("http://localhost:%s/?" + buildQueryString(queryParams), listenPort.getNumber());
    Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    ParameterMap retrivedQueryParams = attributes.getQueryParams();
    assertThat(retrivedQueryParams, notNullValue());
    assertThat(retrivedQueryParams.size(), is(1));
    assertThat(retrivedQueryParams.get(QUERY_PARAM_NAME), is(QUERY_PARAM_SECOND_VALUE));
    assertThat(retrivedQueryParams.getAll(QUERY_PARAM_NAME).size(), is(2));
    assertThat(retrivedQueryParams.getAll(QUERY_PARAM_NAME),
               Matchers.containsInAnyOrder(new String[] {QUERY_PARAM_VALUE, QUERY_PARAM_SECOND_VALUE}));
  }

  @Test
  public void postWithEncodedValues() throws Exception {
    final ImmutableMap<String, Object> queryParams =
        ImmutableMap.<String, Object>builder().put(QUERY_PARAM_NAME, QUERY_PARAM_VALUE_WITH_SPACES).build();
    final String url = String.format("http://localhost:%s/?" + buildQueryString(queryParams), listenPort.getNumber());
    Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    ParameterMap retrivedQueryParams = attributes.getQueryParams();
    assertThat(retrivedQueryParams, notNullValue());
    assertThat(retrivedQueryParams.size(), is(1));
    assertThat(retrivedQueryParams.get(QUERY_PARAM_NAME), is(QUERY_PARAM_VALUE_WITH_SPACES));
  }

  @Test
  public void putWithOldProtocol() throws Exception {
    final ImmutableMap<String, Object> queryParams =
        ImmutableMap.<String, Object>builder().put(QUERY_PARAM_NAME, Arrays.asList(QUERY_PARAM_VALUE, QUERY_PARAM_VALUE)).build();
    final String url = String.format("http://localhost:%s/?" + buildQueryString(queryParams), listenPort.getNumber());
    Request.Put(url).version(HttpVersion.HTTP_1_0).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    assertThat(attributes.getMethod(), is("PUT"));
    assertThat(attributes.getVersion(), is(HttpProtocol.HTTP_1_0.asString()));
  }

  @Test
  public void getFullUriAndPath() throws Exception {
    final String url = String.format("http://localhost:%s%s", listenPort.getNumber(), CONTEXT_PATH);
    Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    assertThat(attributes.getRequestUri(), is(CONTEXT_PATH));
    assertThat(attributes.getRequestPath(), is(CONTEXT_PATH));
    assertThat(attributes.getRelativePath(), is(CONTEXT_PATH));
  }

  @Test
  public void getAllUriParams() throws Exception {
    final String url = String.format("http://localhost:%s/%s/%s/%s", listenPort.getNumber(), FIRST_URI_PARAM,
                                     SECOND_URI_PARAM_VALUE, THIRD_URI_PARAM_VALUE);
    Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    ParameterMap uriParams = getAttributes(message).getUriParams();
    assertThat(uriParams, notNullValue());
    assertThat(uriParams.size(), is(3));
    assertThat(uriParams.get(FIRST_URI_PARAM_NAME), is(FIRST_URI_PARAM));
    assertThat(uriParams.get(SECOND_URI_PARAM_NAME), is(SECOND_URI_PARAM_VALUE));
    assertThat(uriParams.get(THIRD_URI_PARAM_NAME), is(THIRD_URI_PARAM_VALUE));
  }

  @Test
  public void getUriParamInTheMiddle() throws Exception {
    final String url = String.format("http://localhost:%s/some-path/%s/some-other-path", listenPort.getNumber(), FIRST_URI_PARAM);
    Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    ParameterMap uriParams = getAttributes(message).getUriParams();
    assertThat(uriParams, notNullValue());
    assertThat(uriParams.size(), is(1));
    assertThat(uriParams.get(FIRST_URI_PARAM_NAME), is(FIRST_URI_PARAM));
  }

  @Test
  public void postUriParamEncoded() throws Exception {
    final String uriParamValue = "uri param value";
    final String uriParamValueEncoded = URLEncoder.encode(uriParamValue, Charsets.UTF_8.displayName());
    final String url =
        String.format("http://localhost:%s/some-path/%s/some-other-path", listenPort.getNumber(), uriParamValueEncoded);
    Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    ParameterMap uriParams = getAttributes(message).getUriParams();
    assertThat(uriParams, notNullValue());
    assertThat(uriParams.size(), is(1));
    assertThat(uriParams.get(FIRST_URI_PARAM_NAME), is(uriParamValue));
  }

  @Test
  public void xForwardedForHeader() throws Exception {
    final String url = String.format("http://localhost:%s/some-path", listenPort.getNumber());

    Post(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    assertThat(attributes.getRemoteAddress(), startsWith("/127.0.0.1:"));
    assertThat(attributes.getHeaders().get(X_FORWARDED_FOR), nullValue());

    Post(url).addHeader(X_FORWARDED_FOR, "clientIp, proxy1Ip").connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage forwardedMessage = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes forwardedAttributes = getAttributes(forwardedMessage);
    assertThat(forwardedAttributes.getRemoteAddress(), startsWith("/127.0.0.1:"));
    assertThat(forwardedAttributes.getHeaders().get(X_FORWARDED_FOR.toLowerCase()), is("clientIp, proxy1Ip"));
  }

  @Test
  public void getBasePath() throws Exception {
    final String url = String.format("http://localhost:%s%s", listenBasePort.getNumber(), API_CONTEXT_PATH);
    Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    final InternalMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    HttpRequestAttributes attributes = getAttributes(message);
    assertThat(attributes.getListenerPath(), is("/api/*"));
    assertThat(attributes.getRequestPath(), is(API_CONTEXT_PATH));
    assertThat(attributes.getRelativePath(), is(CONTEXT_PATH));
    ParameterMap uriParams = attributes.getUriParams();
    assertThat(uriParams, notNullValue());
    assertThat(uriParams.isEmpty(), is(true));
  }

  public HttpRequestAttributes getAttributes(InternalMessage message) {
    assertThat(message.getPayload().getValue(), is(instanceOf(HttpRequestAttributes.class)));
    return (HttpRequestAttributes) message.getPayload().getValue();
  }

  public String buildQueryString(Map<String, Object> queryParams) throws UnsupportedEncodingException {
    final StringBuilder queryString = new StringBuilder();
    for (String paramName : queryParams.keySet()) {
      final Object value = queryParams.get(paramName);
      if (value instanceof Collection) {
        for (java.lang.Object eachValue : (Collection) value) {
          queryString.append(paramName + "=" + URLEncoder.encode(eachValue.toString(), Charset.defaultCharset().name()));
          queryString.append("&");
        }
      } else {
        queryString.append(paramName + "=" + URLEncoder.encode(value.toString(), Charset.defaultCharset().name()));
        queryString.append("&");
      }
    }
    queryString.deleteCharAt(queryString.length() - 1);
    return queryString.toString();
  }

}
