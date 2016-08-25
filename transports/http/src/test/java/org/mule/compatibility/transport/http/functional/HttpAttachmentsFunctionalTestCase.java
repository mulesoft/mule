/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.message.ds.StringDataSource;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.activation.DataHandler;

import org.junit.Rule;
import org.junit.Test;

public class HttpAttachmentsFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-attachments-functional-test-flow.xml";
  }

  @Test
  public void testSendAttachment() throws Exception {
    FunctionalTestComponent ftc = getFunctionalTestComponent("testComponent");
    assertNotNull(ftc);
    ftc.setEventCallback((context, component, muleContext) -> {
      assertThat(context.getMessage().getDataType().getMediaType().toRfcString(),
                 is("application/octet-stream; charset=ISO-8859-1"));
      assertEquals("We should have an attachment", 1, context.getMessage().getInboundAttachmentNames().size());
      DataHandler dh = context.getMessage().getInboundAttachment("attach1");
      assertNotNull("DataHandler with name 'attach1' should not be null", dh);
      assertEquals("We should have an attachment with foo", "foo", IOUtils.toString(dh.getInputStream()));
      assertEquals("text/plain; charset=ISO-8859-1", dh.getContentType());
    });

    MuleClient client = muleContext.getClient();
    MuleMessage msg = MuleMessage.builder().payload("test")
        .addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1"))).build();

    MuleMessage result = client.send("endpoint1", msg).getRight();
    assertEquals("We should have no attachments coming back", 0, result.getInboundAttachmentNames().size());
  }


  // TODO MULE-5005 response attachments
  // @Test
  // public void testReceiveAttachment() throws Exception
  // {
  // FunctionalTestComponent ftc = getFunctionalTestComponent("testComponent");
  // assertNotNull(ftc);
  // ftc.setEventCallback(new EventCallback(){
  // public void eventReceived(MuleEventContext context, Object component) throws Exception
  // {
  // context.getResult().addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1")));
  // }
  // });
  //
  // LocalMuleClient client = muleContext.getClient();
  // MuleMessage msg = new DefaultMuleMessage("test", muleContext);
  //
  // //msg.addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1")));
  //
  // MuleMessage result = client.send("endpoint1", msg);
  // assertEquals("We should have 1 attachments coming back", 1, result.getInboundAttachmentNames().size());
  // assertEquals("There should be no outbound attachments", 0, result.getOutboundAttachmentNames().size());
  // DataHandler dh = result.getInboundAttachment("attach1");
  // assertNotNull("DataHandler with name 'attach1' should not be null", dh);
  // assertEquals("We should have an attachment with foo", "foo" , IOUtils.toString(dh.getInputStream()));
  // assertEquals("text/plain", dh.getContentType());
  // }

}
