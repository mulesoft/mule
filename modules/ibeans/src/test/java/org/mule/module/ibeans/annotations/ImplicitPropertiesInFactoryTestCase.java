/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
