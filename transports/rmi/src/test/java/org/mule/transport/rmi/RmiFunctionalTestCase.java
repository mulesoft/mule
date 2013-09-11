/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
