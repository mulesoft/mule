/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.junit.Test;

public class RootCustomerTest extends AbstractCustomerTest
{

    @Override
    protected String getConfigResources()
    {
        return "customer-conf.xml";
    }

    @Test
    public void testCustomerProvider() throws Exception
    {
        testCustomerProvider("");
    }
}
