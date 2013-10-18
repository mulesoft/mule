/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.issues;

import org.mule.transport.AbstractFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class RmiMethodTypeMule1857TestCase extends AbstractFunctionalTestCase
{  
    public RmiMethodTypeMule1857TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        this.prefix = "rmi"; 
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "rmi-method-type-1857-test-service.xml"},
            {ConfigVariant.FLOW, "rmi-method-type-1857-test-flow.xml"}
        });
    }      
    
}
