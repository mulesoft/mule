package org.mule.config.spring;

import org.mule.tck.FunctionalTestCase;
import org.mule.api.MuleRuntimeException;

import org.springframework.beans.factory.BeanCreationException;

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
            assertTrue(t instanceof BeanCreationException);
        }
    }
}
