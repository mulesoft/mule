/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VMAttachmentsTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "vm/vm-attachments-test.xml";
    }

    @Test
    public void testAttachments() throws Exception
    {
        DefaultMuleMessage msg = new DefaultMuleMessage("Mmm... attachments!", muleContext);
        FileDataSource ds = new FileDataSource(new File("transports/vm/src/test/resources/"
                                                        + getConfigResources()).getAbsoluteFile());
        msg.addOutboundAttachment("test-attachment", new DataHandler(ds));

        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm-in", msg);

        assertNotNull(reply);
        if (reply.getExceptionPayload() != null)
        {
            fail(reply.getExceptionPayload().getException().getCause().toString());
        }

        //TODO MULE-5000 attachments should be on the inbound
        assertEquals(1, reply.getInboundAttachmentNames().size());
        assertNotNull(reply.getInboundAttachment("mule"));
        assertTrue(reply.getInboundAttachment("mule").getContentType().startsWith("image/gif"));
    }
}
