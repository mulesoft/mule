/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.transformer.compression.GZipUncompressTransformer;
import org.mule.runtime.core.transformer.simple.ByteArrayToSerializable;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;

public class HttpTransformTestCase extends AbstractIntegrationTestCase {

  public static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).build();
  @Rule
  public DynamicPort httpPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort httpPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/sync/http-transform-flow.xml";
  }

  @Test
  public void testTransform() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage message = client.send(String.format("http://localhost:%d/RemoteService", httpPort1.getNumber()),
                                      getTestMuleMessage("payload"), HTTP_REQUEST_OPTIONS)
        .getRight();
    assertNotNull(message);
    GZipUncompressTransformer gu = new GZipUncompressTransformer();
    gu.setMuleContext(muleContext);
    gu.setReturnDataType(DataType.STRING);
    assertNotNull(message.getPayload());
    String result = (String) gu.transform(getPayloadAsBytes(message));
    assertThat(result, is("<string>payload</string>"));
  }

  @Test
  public void testBinary() throws Exception {
    MuleClient client = muleContext.getClient();
    ArrayList<Integer> payload = new ArrayList<Integer>();
    payload.add(42);
    MuleMessage message =
        client.send(String.format("http://localhost:%d/RemoteService", httpPort2.getNumber()),
                    getTestMuleMessage(muleContext.getObjectSerializer().serialize(payload)), HTTP_REQUEST_OPTIONS)
            .getRight();
    assertNotNull(message);
    ByteArrayToSerializable bas = new ByteArrayToSerializable();
    bas.setMuleContext(muleContext);
    assertNotNull(message.getPayload());
    Object result = bas.transform(message.getPayload());
    assertThat(result, is(payload));
  }

  @Test
  public void testBinaryWithBridge() throws Exception {
    MuleClient client = muleContext.getClient();
    Object payload = Arrays.asList(42);
    MuleMessage message = flowRunner("LocalService").withPayload(payload).run().getMessage();
    assertNotNull(message);
    ByteArrayToSerializable bas = new ByteArrayToSerializable();
    bas.setMuleContext(muleContext);
    assertNotNull(message.getPayload());
    Object result = bas.transform(message.getPayload());
    assertThat(result, is(payload));
  }
}
