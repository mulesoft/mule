/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.models;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.mule.model.seda.SedaModel;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

@Ignore("MULE-2742")
public class ConfigureModelTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
