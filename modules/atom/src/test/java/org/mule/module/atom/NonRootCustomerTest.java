/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom;

import org.junit.Test;

public class NonRootCustomerTest extends AbstractCustomerTest
{

    @Override
    protected String getConfigResources()
    {
        return "customer-nonroot-conf.xml";
    }

    @Test
    public void testCustomerProvider() throws Exception
    {
        testCustomerProvider("/base");
    }
}
