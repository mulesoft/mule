/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.api.MuleContext;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;
import org.junit.Test;

public class ConflictedHttpsTlsConfigTestCase extends FunctionalTestCase
{
    private int configNumber;

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    @Override
    protected String getConfigResources()
    {
        return "conflicted-https-config-" + configNumber +".xml";
    }

    @Test
    public void testConfigs() throws Exception
    {
        for (configNumber = 1; configNumber <= 3; configNumber++)
        {
            try
            {
                super.createMuleContext();
                fail("No conflict seen");
            }
            catch (Exception ex)
            {
                assertExceptionIsOfType(ex, CheckExclusiveAttributes.CheckExclusiveAttributesException.class);
            }
        }
    }
    
    public void assertExceptionIsOfType(Throwable ex, Class type)
    {
        Set<Throwable> seen = new HashSet<Throwable>();
        
        while (true)
        {
            if (type.isInstance(ex))
            {
                return;
            }
            else if (ex == null || seen.contains(ex))
            {
                fail("Bad exception type");
            }
            else
            {
                seen.add(ex);
                ex = ex.getCause();
            }
        }
    }
}


