/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.functional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mule.compatibility.module.cxf.CxfBasicTestCase.APP_SOAP_XML;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class CxfDataTypeTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.cxf.module.compatibility.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>Hello</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "cxf-datatype-conf.xml";
  }

  @Test
  public void testCxfService() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestPayload).build();
    InternalMessage received = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/hello", request,
                                                            newOptions().method(POST.name()).disableStatusCodeValidation()
                                                                .build())
        .getRight();
    Assert.assertThat(getPayloadAsString(received), not(containsString("Fault")));
  }

  @Test
  public void testCxfClient() throws Exception {
    InternalMessage received = flowRunner("helloServiceClient").withPayload("hello").run().getMessage();
    Assert.assertThat(getPayloadAsString(received), not(containsString("Fault")));
  }

  @Test
  public void testCxfProxy() throws Exception {
    InternalMessage request = InternalMessage.builder().payload(requestPayload).build();
    InternalMessage received =
        muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/hello-proxy", request,
                                     newOptions().method(POST.name()).disableStatusCodeValidation().build())
            .getRight();
    Assert.assertThat(getPayloadAsString(received), not(containsString("Fault")));
  }

  @Test
  public void testCxfSimpleService() throws Exception {
    MuleClient client = muleContext.getClient();
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    InternalMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/echo",
                                         InternalMessage.builder().payload(xml).mediaType(APP_SOAP_XML).build(),
                                         newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    Assert.assertThat(getPayloadAsString(result), not(containsString("Fault")));
  }

  @Test
  public void testCxfSimpleClient() throws Exception {
    InternalMessage received = flowRunner("helloServiceClient").withPayload("hello").run().getMessage();
    Assert.assertThat(getPayloadAsString(received), not(containsString("Fault")));
  }

  public static class EnsureXmlDataType extends EnsureDataType {

    public EnsureXmlDataType() {
      super(MediaType.XML);
    }
  }

  public static class EnsureAnyDataType extends EnsureDataType {

    public EnsureAnyDataType() {
      super(MediaType.ANY);
    }
  }

  private static class EnsureDataType implements Callable {

    private final MediaType mimeType;

    public EnsureDataType(MediaType mimeType) {
      this.mimeType = mimeType;
    }

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      if (!eventContext.getMessage().getPayload().getDataType().getMediaType().matches(mimeType)) {
        throw new RuntimeException();
      }
      return eventContext.getMessage().getPayload().getValue();
    }
  }

}
