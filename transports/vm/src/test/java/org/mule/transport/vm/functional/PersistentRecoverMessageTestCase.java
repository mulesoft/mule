/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SerializationUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.junit.Test;

public class PersistentRecoverMessageTestCase extends FunctionalTestCase
{

    public PersistentRecoverMessageTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "vm/persistent-vmqueue-test.xml";
    }

    @Test
    public void testRecoverMessage() throws Exception
    {
        File file = FileUtils.createFile(".mule/queuestore/flowOut/0-000-out-01.msg");
        OutputStream os = new FileOutputStream(file);
        MuleEvent event = getTestEvent("echo");
        SerializationUtils.serialize(event, os);
        muleContext.start();

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("vm://flowOut", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("echo", result.getPayload());
    }
}


