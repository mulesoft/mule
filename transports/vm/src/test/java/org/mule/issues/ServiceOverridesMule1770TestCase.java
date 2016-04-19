/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.transformer.NoActionTransformer;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.transformer.TransformerUtils;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;

import org.junit.Test;

public class ServiceOverridesMule1770TestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
