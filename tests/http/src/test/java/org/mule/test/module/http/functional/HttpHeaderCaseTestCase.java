/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.core.api.Event;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

/**
 * Set up a listener that returns form data and a requester that triggers it, all while preserving the headers name case. That way
 * we make sure the Host, Content-Type and other headers are handled correctly by both listener and requester.
 */
public class HttpHeaderCaseTestCase extends AbstractHttpTestCase {

  public static final String PRESERVE_HEADER_CASE = "org.glassfish.grizzly.http.PRESERVE_HEADER_CASE";

  @Rule
  public DynamicPort port = new DynamicPort("port");
  @Rule
  public SystemProperty headerCaseProperty = new SystemProperty(PRESERVE_HEADER_CASE, "true");

  @Override
  protected String getConfigFile() {
    return "http-header-case-config.xml";
  }

  @Test
  public void worksPreservingHeaders() throws Exception {
    Event response = runFlow("client");
    Object payload = response.getMessage().getPayload().getValue();
    assertThat(payload, is(instanceOf(ParameterMap.class)));
    assertThat(((ParameterMap) payload).keySet(), hasItem("CustomValue"));
    assertThat(((ParameterMap) payload).get("CustomValue"), is("value"));
    HttpResponseAttributes attributes = (HttpResponseAttributes) response.getMessage().getAttributes();
    assertThat(attributes.getHeaders().get(CONTENT_TYPE), is(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString()));
    assertThat(attributes.getHeaders().get("customname1"), is("customValue"));
  }
}
