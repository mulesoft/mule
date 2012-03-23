/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class PersistentUnhealthyMessageTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "vm/persistent-vmqueue-test.xml";
    }

    @BeforeClass
    public static void populateUnhealthyFiles() throws Exception
    {
        FileUtils.createFile(".mule/queuestore/flowOut/0-000-out-01.msg");
    }

    @Test
    public void testUnhealthyMessageIgnored() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://flowIn", "echo", null);
        MuleMessage result = client.request("vm://flowOut", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("echo", result.getPayload());
    }
}


