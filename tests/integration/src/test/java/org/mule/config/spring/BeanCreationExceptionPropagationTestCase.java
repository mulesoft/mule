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

import org.mule.api.MuleRuntimeException;
import org.mule.tck.FunctionalTestCase;

import org.springframework.beans.FatalBeanException;

public class BeanCreationExceptionPropagationTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/config/spring/bean-creation-exception-propagation-config.xml";
    }

    @Override
    protected boolean isStartContext()
    {
        // need to start it ourselves and catch the exception
        return false;
    }

    public void testBeanCreationExceptionPropagation()
    {
        // lookup all objects
        try
        {
            muleContext.getRegistry().lookupObjects(Object.class);
            fail("Should've failed with an exception");
        }
        catch (MuleRuntimeException e)
        {
            Throwable t = e.getCause();
            assertNotNull(t);
            assertTrue(t instanceof FatalBeanException);
        }
    }
}
