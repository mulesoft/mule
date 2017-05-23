/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.request;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.REQUEST_BUILDER;

import org.mule.runtime.http.api.domain.ParameterMap;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.EmptyHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_SERVICE)
@Stories(REQUEST_BUILDER)
public class HttpRequestBuilderTestCase {

  public static final String URI_VALUE = "someUri";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private HttpRequestBuilder builder = HttpRequest.builder();
  private String name = "name";
  private String value = "value";

  @Test
  public void defaultRequest() {
    HttpRequest request = builder.setUri("someUri").build();
    assertThat(request.getMethod(), is("GET"));
    assertThat(request.getUri(), is("someUri"));
    assertThat(request.getEntity(), is(instanceOf(EmptyHttpEntity.class)));
    assertThat(request.getHeaderNames(), empty());
    assertThat(request.getQueryParams().keySet(), empty());
  }

  @Test
  public void failWithoutUri() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(containsString("URI must be specified"));
    builder.build();
  }

  @Test
  public void complexResponse() {
    ParameterMap paramMap = new ParameterMap();

    paramMap.put(name, value);
    HttpRequest request = builder.setEntity(new ByteArrayHttpEntity("test".getBytes()))
        .setUri(URI_VALUE)
        .setMethod(POST)
        .setQueryParams(paramMap)
        .setHeaders(paramMap)
        .addHeader(name.toUpperCase(), value.toUpperCase())
        .build();

    assertThat(request.getUri(), is(URI_VALUE));
    assertThat(request.getMethod(), is(POST.name()));
    assertThat(request.getEntity(), is(instanceOf(ByteArrayHttpEntity.class)));
    assertThat(request.getHeaderNames(), hasItems(name));
    assertThat(request.getHeaderValues(name), hasItems(value, value.toUpperCase()));
    ParameterMap requestQueryParams = request.getQueryParams();
    assertThat(requestQueryParams.keySet(), hasItems(name));
    assertThat(requestQueryParams.getAll(name), hasItems(value));
  }

  @Test
  public void headerManipulation() {
    builder.setUri(URI_VALUE);
    assertThat(builder.build().getHeaderNames(), empty());

    String otherValue = "otherValue";
    ParameterMap parameterMap = new ParameterMap();
    parameterMap.put(name, value);
    parameterMap.put(name, otherValue);

    // add multiple valued header through parameter map and individually
    builder.setHeaders(parameterMap);
    builder.addHeader(name, value);
    assertThat(builder.getHeaderValues(name), hasItems(value, otherValue, value));
    assertThat(builder.build().getHeaderValues(name), hasItems(value, otherValue, value));

    // remove header
    builder.removeHeader(name);
    assertThat(builder.build().getHeaderNames(), empty());
  }
}
