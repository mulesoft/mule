/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

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
