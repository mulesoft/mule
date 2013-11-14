/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleRuntimeException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.springframework.beans.FatalBeanException;

public class BeanCreationExceptionPropagationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/bean-creation-exception-propagation-config.xml";
    }

    @Override
    protected boolean isStartContext()
    {
        // need to start it ourselves and catch the exception
        return false;
    }

    @Test
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
