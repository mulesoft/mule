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

public class NonRootServletTestCase extends AbstractServletTestCase
{
    public NonRootServletTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, "/context/*");
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "non-root-servlet-conf-service.xml"},
            {ConfigVariant.FLOW, "non-root-servlet-conf-flow.xml"}
        });
    }

    @Test
    public void testBasic() throws Exception
    {
        doTestBasic("http://localhost:63088/context/base");
    }
}
