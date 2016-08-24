/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.functional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mule.runtime.module.cxf.CxfBasicTestCase.APP_SOAP_XML;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class CxfDataTypeTestCase extends FunctionalTestCase {

  private static final String requestPayload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
      + "           xmlns:hi=\"http://example.cxf.module.runtime.mule.org/\">\n" + "<soap:Body>\n" + "<hi:sayHi>\n"
      + "    <arg0>Hello</arg0>\n" + "</hi:sayHi>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "cxf-datatype-conf.xml";
  }

  @Test
  public void testCxfService() throws Exception {
    MuleMessage request = MuleMessage.builder().payload(requestPayload).build();
    MuleMessage received = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/hello", request,
                                                        newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    Assert.assertThat(getPayloadAsString(received), not(containsString("Fault")));
  }

  @Test
  public void testCxfClient() throws Exception {
    MuleMessage received = flowRunner("helloServiceClient").withPayload("hello").run().getMessage();
    Assert.assertThat(getPayloadAsString(received), not(containsString("Fault")));
  }

  @Test
  public void testCxfProxy() throws Exception {
    MuleMessage request = MuleMessage.builder().payload(requestPayload).build();
    MuleMessage received = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/hello-proxy", request,
                                                        newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    Assert.assertThat(getPayloadAsString(received), not(containsString("Fault")));
  }

  @Test
  public void testCxfSimpleService() throws Exception {
    MuleClient client = muleContext.getClient();
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/echo",
                                     MuleMessage.builder().payload(xml).mediaType(APP_SOAP_XML).build(),
                                     newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    Assert.assertThat(getPayloadAsString(result), not(containsString("Fault")));
  }

  @Test
  public void testCxfSimpleClient() throws Exception {
    MuleMessage received = flowRunner("helloServiceClient").withPayload("hello").run().getMessage();
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
      if (!eventContext.getMessage().getDataType().getMediaType().matches(mimeType)) {
        throw new RuntimeException();
      }
      return eventContext.getMessage().getPayload();
    }
  }

}
