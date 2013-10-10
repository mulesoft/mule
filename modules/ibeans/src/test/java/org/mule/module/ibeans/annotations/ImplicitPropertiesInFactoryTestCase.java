/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.annotation.IntegrationBean;
import org.ibeans.api.CallException;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ImplicitPropertiesInFactoryTestCase extends AbstractIBeansTestCase
{
    @IntegrationBean
    private TestImplicitPropertiesinFactoryIBean testIBean;

    @Test
    public void testGetHttpMethod() throws Exception
    {
        try
        {
            testIBean.doStuff();
            fail("Should have failed since the call cannot be made");
        }
        catch (CallException e)
        {
            //expected, we can't actually connect to the service
        }
        catch (IllegalArgumentException e)
        {
            fail("It seems the HTTP method property was not set implicitly: " + e.getMessage());
        }
    }
}
