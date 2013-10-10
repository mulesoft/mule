/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;

import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;
import org.mule.tck.transformer.NoActionTransformer;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.AbstractConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ServiceOverridesMule1770TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "issues/service-overrides-mule-1770-test.xml";
    }

    @Test
    public void testServiceOverrides()
    {
        AbstractConnector c = (AbstractConnector)muleContext.getRegistry().lookupConnector("test");
        assertNotNull("Connector should not be null", c);
        assertNotNull("Service overrides should not be null", c.getServiceOverrides());
        String temp =  (String)c.getServiceOverrides().get(MuleProperties.CONNECTOR_DISPATCHER_FACTORY);
        assertNotNull("DispatcherFactory override should not be null", temp);
        assertEquals(TestMessageDispatcherFactory.class.getName(), temp);
        Transformer transformer = TransformerUtils.firstOrNull(c.getDefaultInboundTransformers(null));
        assertNotNull("InboundTransformer should not be null", transformer);
        assertEquals(NoActionTransformer.class, transformer.getClass());
    }

    // MULE-1878
    @Test
    public void testDuplicate()
    {
        AbstractConnector c1 = (AbstractConnector)muleContext.getRegistry().lookupConnector("test");
        assertNotNull("Connector should not be null", c1);
        Transformer t1 = TransformerUtils.firstOrNull(c1.getDefaultInboundTransformers(null));
        assertNotNull("InboundTransformer should not be null", t1);
        assertEquals(NoActionTransformer.class, t1.getClass());

        AbstractConnector c2 = (AbstractConnector)muleContext.getRegistry().lookupConnector("second");
        assertNotNull("Connector should not be null", c2);
        Transformer t2 = TransformerUtils.firstOrNull(c2.getDefaultInboundTransformers(null));
        assertNull("InboundTransformer should be null", t2);
    }

}
