/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.model.InvocationResult;
import org.mule.tck.AbstractMuleTestCase;

import java.lang.reflect.Method;

import junit.framework.AssertionFailedError;

public class EntryPointResolverMethodCacheTestCase extends AbstractMuleTestCase
{
    
    private static final String METHOD = "aMethod";
    
    public void testMethodCaching() throws Exception
    {
        Method method = this.getClass().getMethod(METHOD, new Class[] { String.class});
        
        MuleEventContext eventContext = getTestEventContext(null);
        MockEntryPointResolver epResolver = new MockEntryPointResolver();
        epResolver.addMethodByName(method, eventContext);
        
        assertEquals(method, epResolver.getMethodByName(METHOD, eventContext));   
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
    
}


