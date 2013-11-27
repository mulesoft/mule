/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bpm.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.construct.Flow;
import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.ProcessComponent;
import org.mule.module.bpm.test.TestBpms;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * Tests the Spring XML namespace for the BPM transport.
 */
public class BpmNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "bpm-namespace-config-flow.xml";
    }

    @Test
    public void testDefaultsComponent() throws Exception
    {
        ProcessComponent c = (ProcessComponent) ((Flow) muleContext.getRegistry().lookupObject("Service1")).getMessageProcessors().get(0);
        assertNotNull(c);

        assertEquals("test.def", c.getResource());
        assertNull(c.getProcessIdField());

        // BPMS gets set explicitly in config
        BPMS bpms = c.getBpms();
        assertNotNull(bpms);
        assertEquals(TestBpms.class, bpms.getClass());
        assertEquals("bar", ((TestBpms) bpms).getFoo());
    }

    @Test
    public void testConfigComponent() throws Exception
    {
        ProcessComponent c = (ProcessComponent) ((Flow) muleContext.getRegistry().lookupObject("Service2")).getMessageProcessors().get(0);
        assertNotNull(c);

        assertEquals("test.def", c.getResource());
        assertEquals("myId", c.getProcessIdField());

        // BPMS gets set implicitly via MuleRegistry.lookupObject(BPMS.class)
        BPMS bpms = c.getBpms();
        assertNotNull(bpms);
        assertEquals(TestBpms.class, bpms.getClass());
        assertEquals("bar", ((TestBpms) bpms).getFoo());
    }

}
