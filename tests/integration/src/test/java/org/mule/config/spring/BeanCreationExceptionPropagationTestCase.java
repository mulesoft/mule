/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.MuleRuntimeException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.springframework.beans.FatalBeanException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BeanCreationExceptionPropagationTestCase extends FunctionalTestCase
{

    @Override
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
