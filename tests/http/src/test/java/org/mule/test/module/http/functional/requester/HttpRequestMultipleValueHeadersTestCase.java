/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.MULTI_MAP;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;


@Features(HTTP_EXTENSION)
@Stories(MULTI_MAP)
@Ignore("MULE-11606: Support a way to have DW handle multimaps")
public class HttpRequestMultipleValueHeadersTestCase extends AbstractHttpRequestTestCase {

  @Rule
  public SystemProperty host = new SystemProperty("host", "localhost");
  @Rule
  public SystemProperty encoding = new SystemProperty("encoding", CHUNKED);
  @Rule
  public DynamicPort port = new DynamicPort("port");

  public HttpRequestMultipleValueHeadersTestCase() {
    headers = Multimaps
        .newListMultimap(new CaseInsensitiveMapWrapper<Collection<String>>(), Lists::newArrayList);
  }

  @Override
  protected String getConfigFile() {
    return "http-request-multiple-header-config.xml";
  }

  @Test
  public void preservesOrderAndFormatWithMultipleValuedHeader() throws Exception {
    runFlow("out");

    assertThat(headers.asMap(), hasKey("multipleHeader"));
    assertThat(headers.asMap().get("multipleHeader"), isA(Iterable.class));
    assertThat(headers.asMap().get("multipleHeader"), contains("1", "2", "3"));
  }

  @Test
  public void receivesMultipleValuedHeader() throws Exception {
    Event event = runFlow("in");

    HttpResponseAttributes attributes = (HttpResponseAttributes) event.getMessage().getAttributes().getValue();
    List<String> headers = attributes.getHeaders().getAll("multipleheader");
    assertThat(headers, hasSize(3));
    assertThat(headers, contains("1", "2", "3"));
  }

}


