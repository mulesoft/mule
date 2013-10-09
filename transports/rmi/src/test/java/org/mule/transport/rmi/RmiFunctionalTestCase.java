/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.rmi;

import org.mule.transport.AbstractFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class RmiFunctionalTestCase extends AbstractFunctionalTestCase
{      
    public RmiFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        this.prefix = "rmi";
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "rmi-functional-test-service.xml"},
            {ConfigVariant.FLOW, "rmi-functional-test-flow.xml"}
        });
    }      
}
