/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class RootServletTestCase extends AbstractServletTestCase
{

    public RootServletTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, "/*");
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "servlet-conf-service.xml"},
            {ConfigVariant.FLOW, "servlet-conf-flow.xml"}
        });
    }

    @Test
    public void testBasic() throws Exception
    {
        doTestBasic("http://localhost:" + httpPort.getNumber() + "/base");
    }
}
