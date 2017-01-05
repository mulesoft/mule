/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.junit.Test;

public class VMAttachmentsTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vm/vm-attachments-test-flow.xml";
  }

  @Test
  public void testAttachments() throws Exception {
    FileDataSource ds = new FileDataSource(new File("transports/vm/src/test/resources/" + getConfigFile()).getAbsoluteFile());
    InternalMessage msg = InternalMessage.builder().payload("Mmm... attachments!")
        .addOutboundAttachment("test-attachment", new DataHandler(ds)).build();

    MuleClient client = muleContext.getClient();
    InternalMessage reply = client.send("vm-in", msg).getRight();

    assertNotNull(reply);
    assertEquals(1, reply.getInboundAttachmentNames().size());
    assertNotNull(reply.getInboundAttachment("mule"));
    assertTrue(reply.getInboundAttachment("mule").getContentType().startsWith("image/gif"));
  }
}
