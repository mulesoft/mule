/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class VMAttachmentsTestCase extends AbstractServiceAndFlowTestCase
{
    public VMAttachmentsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "vm/vm-attachments-test-service.xml"},
            {ConfigVariant.FLOW, "vm/vm-attachments-test-flow.xml"}
        });
    }

    @Test
    public void testAttachments() throws Exception
    {
        MuleMessage msg = getTestMuleMessage("Mmm... attachments!");
        FileDataSource ds = new FileDataSource(new File("transports/vm/src/test/resources/"
                                                        + getConfigFile()).getAbsoluteFile());
        msg.addOutboundAttachment("test-attachment", new DataHandler(ds));

        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("vm-in", msg);

        assertNotNull(reply);
        if (reply.getExceptionPayload() != null)
        {
            fail(reply.getExceptionPayload().getException().getCause().toString());
        }

        assertEquals(1, reply.getInboundAttachmentNames().size());
        assertNotNull(reply.getInboundAttachment("mule"));
        assertTrue(reply.getInboundAttachment("mule").getContentType().startsWith("image/gif"));
    }
}
