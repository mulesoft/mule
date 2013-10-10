/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.models;


import org.mule.model.seda.SedaModel;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigureModelTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/models/direct-pipeline-test-config.xml";
    }

    @Test
    public void testConfigure()
    {
        assertTrue(muleContext.getRegistry().lookupModel("main") instanceof SedaModel);
        assertEquals("main", muleContext.getRegistry().lookupModel("main").getName());
    }
}
