/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.bpm.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.construct.Flow;
import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.ProcessComponent;
import org.mule.module.bpm.test.TestBpms;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the Spring XML namespace for the BPM transport.
 */
public class BpmNamespaceHandlerTestCase extends AbstractServiceAndFlowTestCase
{
    public BpmNamespaceHandlerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "bpm-namespace-config-service.xml"},
            {ConfigVariant.FLOW, "bpm-namespace-config-flow.xml"}});
    }

    @Test
    public void testDefaultsComponent() throws Exception
    {
        ProcessComponent c;
        
        if (variant.equals(ConfigVariant.FLOW))
        {
            c = (ProcessComponent) ((Flow) muleContext.getRegistry().lookupObject("Service1")).getMessageProcessors()
                .get(0);
        }
        else
        {
            c = (ProcessComponent) muleContext.getRegistry().lookupService("Service1").getComponent();
        }

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
        ProcessComponent c;

        if (variant.equals(ConfigVariant.FLOW))
        {
            c = (ProcessComponent) ((Flow) muleContext.getRegistry().lookupObject("Service2")).getMessageProcessors()
                .get(0);
        }
        else
        {
            c = (ProcessComponent) muleContext.getRegistry().lookupService("Service2").getComponent();
        }
        
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
