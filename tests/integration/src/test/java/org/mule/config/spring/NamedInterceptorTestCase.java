/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class NamedInterceptorTestCase extends AbstractInterceptorTestCase
{
    public NamedInterceptorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/config/spring/named-interceptor-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/config/spring/named-interceptor-test-flow.xml"}});
    }

    @Test
    public void testInterceptor() throws MuleException, InterruptedException
    {
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://in", MESSAGE, null);
        assertMessageIntercepted();
    }
}
