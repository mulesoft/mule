/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.vm.functional;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class VMAttachmentsTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "vm/vm-attachments-test.xml";
    }

    public void testAttachments() throws Exception
    {
         MuleMessage m = new MuleMessage("Mmm... attachments!");
        FileDataSource ds = new FileDataSource(new File("transports/vm/src/test/resources/" + getConfigResources()).getAbsoluteFile());
        m.addAttachment("test-attachment", new DataHandler(ds));
        MuleClient client = new MuleClient();
        UMOMessage msg = client.send("vm-in", m);
        assertNotNull(msg);
        if(msg.getExceptionPayload()!=null)
        {
            fail(msg.getExceptionPayload().getException().getCause().toString());
        }
        assertEquals(1, msg.getAttachmentNames().size());
        assertNotNull(msg.getAttachment("mule"));
        assertTrue(msg.getAttachment("mule").getContentType().startsWith("image/gif"));

    }
}
