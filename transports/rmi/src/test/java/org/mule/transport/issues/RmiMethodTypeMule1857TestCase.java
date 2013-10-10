/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
