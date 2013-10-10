/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        doTestBasic("http://localhost:63088/base");
    }
}
