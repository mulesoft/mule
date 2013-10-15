/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.model.InvocationResult;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Method;

import junit.framework.AssertionFailedError;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class EntryPointResolverMethodCacheTestCase extends AbstractMuleTestCase
{
    
    private static final String METHOD = "aMethod";
    
    @Test
    public void testMethodCaching() throws Exception
    {
        Method method = this.getClass().getMethod(METHOD, new Class[] { String.class});
        Method anotherMethod = AnotherClass.class.getMethod(METHOD, new Class[] { String.class});

        MuleEventContext eventContext = mock(MuleEventContext.class);
        MockEntryPointResolver epResolver = new MockEntryPointResolver();

        epResolver.addMethodByName(this, method, eventContext);
        Method method1 = epResolver.getMethodByName(this, METHOD, eventContext);
        assertEquals(method, method1);
        assertEquals(this.getClass(), method1.getDeclaringClass());

        AnotherClass anotherObject = new AnotherClass();
        epResolver.addMethodByName(anotherObject, anotherMethod, eventContext);
        Method anotherMethod1 = epResolver.getMethodByName(anotherObject, METHOD, eventContext);
        assertEquals(anotherMethod, anotherMethod1);
        assertEquals(AnotherClass.class, anotherMethod.getDeclaringClass());

    }
    
    public void aMethod(String payload)
    {
        // this method exists only for being cached in the test
    }
    
    private static class MockEntryPointResolver extends AbstractEntryPointResolver
    {
        public InvocationResult invoke(Object component, MuleEventContext context) throws Exception
        {
            throw new AssertionFailedError("do not invoke this method");
        }
    }

    private static class AnotherClass
    {
        public void aMethod(String payload)
        {
            // this method exists only for being cached in the test
        }
    }
    
}


