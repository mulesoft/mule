/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RejectOldConfigTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "old-config.xml";
    }

    @Test
    public void testParse()
    {
        // see overloaded methods
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        try {
            super.createMuleContext();
            fail("Context created with no problems - expected failure");
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            assertTrue(msg, msg.indexOf("Unable to locate NamespaceHandler for namespace [null]") > -1);
        }
        return null;
    }

}
