/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.tck.FunctionalTestCase;

public class RejectOldConfigTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "old-config.xml";
    }

    public void testParse()
    {
        // see overloaded methods
    }

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
