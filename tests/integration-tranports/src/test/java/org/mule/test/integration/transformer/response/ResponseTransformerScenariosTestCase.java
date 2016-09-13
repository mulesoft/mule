/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transformer.response;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;

public class ResponseTransformerScenariosTestCase extends FunctionalTestCase {

  private static String VM_INBOUND = " inbound";
  private static String VM_OUTBOUND = " outbound";
  private static String VM_RESPONSE = " response";

  private static String VM_OUT_IN_RESP = VM_OUTBOUND + VM_INBOUND + VM_RESPONSE;

  private static String CUSTOM_RESPONSE = " customResponse";

  @ClassRule
  public static DynamicPort httpPort1 = new DynamicPort("port1");

  @ClassRule
  public static DynamicPort httpPort2 = new DynamicPort("port2");

  @ClassRule
  public static DynamicPort httpPort3 = new DynamicPort("port3");

  public ResponseTransformerScenariosTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transformer/response/response-transformer-scenarios.xml";
  }

  @Test
  public void testVmSync() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage message = client.send("vm://sync", "request", null).getRight();
    assertThat(message, notNullValue());
    assertThat(getPayloadAsString(message), is(equalTo("request" + VM_OUT_IN_RESP)));
  }

  @Test
  public void testVmSyncResponseTransformer() throws Exception {
    MuleClient client = muleContext.getClient();

    Map<String, Serializable> props = new HashMap<>();
    // This will disable the transformers configured in the VM connector's service-overrides.
    props.put(MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY, "true");

    InternalMessage message = client.send("vm://syncResponseTransformer", "request", props).getRight();
    assertThat(message, notNullValue());
    assertThat(getPayloadAsString(message), is(equalTo("request" + CUSTOM_RESPONSE)));
  }

  @Test
  public void testVmSyncOutboundEndpointResponseTransformer() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage message = client.send("vm://syncOutboundEndpointResponseTransformer", "request", null).getRight();
    assertThat(message, notNullValue());
    assertThat(getPayloadAsString(message),
               is(equalTo("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + CUSTOM_RESPONSE + VM_RESPONSE)));
  }

  @Test
  public void testJmsRemoteSync() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage message = client.send("vm://jmsSync", "request", null).getRight();
    assertThat(message, notNullValue());

    assertThat(getPayloadAsString(message), is(equalTo("request" + VM_OUT_IN_RESP)));
  }

  @Test
  public void testJmsSyncOutboundEndpointResponseTransformer() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage message = client.send("vm://jmsSyncOutboundEndpointResponseTransformer", "request", null).getRight();
    assertThat(message, notNullValue());
    assertThat(getPayloadAsString(message), is(equalTo("request" + VM_OUTBOUND + VM_INBOUND + CUSTOM_RESPONSE + VM_RESPONSE)));
  }

  @Test
  public void testChainedRouterOutboundEndpointResponseTransformer() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage message = client.send("vm://chainedRouterOutboundEndpointResponseTransformer", "request", null).getRight();
    assertThat(message, notNullValue());
    assertThat(getPayloadAsString(message), is(equalTo("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + VM_OUT_IN_RESP
        + CUSTOM_RESPONSE + CUSTOM_RESPONSE + VM_RESPONSE)));
  }

  @Test
  public void testNestedRouterOutboundEndpointResponseTransformer() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage message = client.send("vm://nestedRouterOutboundEndpointResponseTransformer", "request", null).getRight();
    assertThat(message, notNullValue());
    assertThat(getPayloadAsString(message), is(equalTo("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + CUSTOM_RESPONSE
        + CUSTOM_RESPONSE + VM_RESPONSE)));
  }
}
