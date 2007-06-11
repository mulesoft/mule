/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm.issues;

import org.mule.config.MuleProperties;
import org.mule.providers.AbstractConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.transformers.NoActionTransformer;

public class ServiceOverridesMule1770TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "service-overrides-mule-1770-test.xml";
    }

    public void testServiceOverrides()
    {
        AbstractConnector c = (AbstractConnector)managementContext.getRegistry().lookupConnector("test");
        assertNotNull("Connector should not be null", c);
        assertNotNull("Service overrides should not be null", c.getServiceOverrides());
        String temp =  (String)c.getServiceOverrides().get(MuleProperties.CONNECTOR_DISPATCHER_FACTORY);
        assertNotNull("DispatcherFactory override should not be null", temp);
        assertEquals(TestMessageDispatcherFactory.class.getName(), temp);
        UMOTransformer transformer = c.getDefaultInboundTransformer();
        assertNotNull("InboundTransformer should not be null", transformer);
        assertEquals(NoActionTransformer.class, transformer.getClass());
    }


    // MULE-1878
    public void testDuplicate()
    {
        AbstractConnector c1 = (AbstractConnector)managementContext.getRegistry().lookupConnector("test");
        assertNotNull("Connector should not be null", c1);
        UMOTransformer t1 = c1.getDefaultInboundTransformer();
        assertNotNull("InboundTransformer should not be null", t1);
        assertEquals(NoActionTransformer.class, t1.getClass());

        AbstractConnector c2 = (AbstractConnector)managementContext.getRegistry().lookupConnector("second");
        assertNotNull("Connector should not be null", c2);
        UMOTransformer t2 = c2.getDefaultInboundTransformer();
        assertNull("InboundTransformer should be null", t2);
    }

}
