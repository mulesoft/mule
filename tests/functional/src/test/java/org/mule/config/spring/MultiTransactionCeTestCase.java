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
import org.mule.util.ExceptionUtils;

public class MultiTransactionCeTestCase extends org.mule.tck.FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "multi-tx-config-ce-fails.xml";
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        try
        {
            super.createMuleContext();
            fail("Should've failed with an error message");
        }
        catch (Exception e)
        {
            Throwable t = ExceptionUtils.getRootCause(e);
            assertNotNull(t);
            assertTrue("Wrong exception?", t.getMessage().contains("EE-only feature"));
        }

        return null;
    }

    public void testParse()
    {
        // see overloaded methods
    }

}
