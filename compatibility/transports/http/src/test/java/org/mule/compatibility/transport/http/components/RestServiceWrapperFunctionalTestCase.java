/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.component.ComponentException;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class RestServiceWrapperFunctionalTestCase extends CompatibilityFunctionalTestCase {

  protected static String TEST_REQUEST = "Test Http Request";

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Rule
  public DynamicPort port2 = new DynamicPort("port2");

  public RestServiceWrapperFunctionalTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "http-rest-service-wrapper-functional-test-flow.xml";
  }

  @Test
  public void testErrorExpressionOnRegexFilterFail() throws Exception {
    InternalMessage result =
        muleContext.getClient().send("restServiceEndpoint", InternalMessage.builder().payload(TEST_REQUEST).build()).getRight();
    assertThat(result, notNullValue());
    assertThat(result.getExceptionPayload(), notNullValue());
    assertThat(result.getExceptionPayload().getException(), hasCause(instanceOf(RestServiceException.class)));
  }

  @Test
  public void testErrorExpressionOnRegexFilterPass() throws Exception {
    InternalMessage result =
        muleContext.getClient().send("restServiceEndpoint2", InternalMessage.builder().payload(TEST_REQUEST).build()).getRight();
    assertEquals("echo=" + TEST_REQUEST, getPayloadAsString(result));
  }

  @Test
  public void testRequiredParameters() throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put("baz-header", "baz");
    props.put("bar-optional-header", "bar");

    InternalMessage result = muleContext.getClient().send("restServiceEndpoint3",
                                                          InternalMessage.builder().nullPayload().outboundProperties(props)
                                                              .build())
        .getRight();
    assertEquals("foo=boo&faz=baz&far=bar", getPayloadAsString(result));
  }

  @Test
  public void testOptionalParametersMissing() throws Exception {
    InternalMessage result = muleContext.getClient()
        .send("restServiceEndpoint3", InternalMessage.builder().nullPayload().addOutboundProperty("baz-header", "baz").build())
        .getRight();
    assertEquals("foo=boo&faz=baz", getPayloadAsString(result));
  }

  @Test
  public void testRequiredParametersMissing() throws Exception {
    InternalMessage result =
        muleContext.getClient().send("restServiceEndpoint3", InternalMessage.builder().nullPayload().build()).getRight();
    assertThat(result, notNullValue());
    assertThat(result.getExceptionPayload(), notNullValue());
    assertThat(result.getExceptionPayload().getException(), hasCause(instanceOf(ComponentException.class)));
  }

  @Test
  public void testRestServiceComponentInFlow() throws Exception {
    InternalMessage result =
        muleContext.getClient().send("vm://toFlow", InternalMessage.builder().payload(TEST_REQUEST).build()).getRight();
    assertNotNull(result);
    assertEquals("echo=Test Http Request", getPayloadAsString(result));
  }

  @Test
  public void restServiceComponentShouldPreserveContentTypeOnIncomingMessage() throws Exception {
    InternalMessage result =
        muleContext.getClient().send("vm://restservice4", InternalMessage.builder().payload(TEST_REQUEST).build()).getRight();
    assertNotNull(result);
    assertEquals("foo/bar", MediaType.parse(getPayloadAsString(result)).withoutParameters().toRfcString());
  }

  public static class CopyContentTypeFromRequest implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      return eventContext.getMessage().getPayload().getDataType().getMediaType().toRfcString();
    }
  }
}
