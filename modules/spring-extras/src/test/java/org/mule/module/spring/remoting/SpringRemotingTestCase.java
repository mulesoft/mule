/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

public class SpringRemotingTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String SPRING_HTTP_ENDPOINT = "http://localhost:8003/springService";

    public SpringRemotingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "spring-remoting-mule-config-service.xml"},
            {ConfigVariant.FLOW, "spring-remoting-mule-config-flow.xml"}
        });
    }

    @Test
    public void testHttpInvokeSpringService() throws Exception
    {
        ComplexData cd = new ComplexData("Foo", new Integer(13));
        HttpInvokerProxyFactoryBean invoker = new HttpInvokerProxyFactoryBean();
        invoker.setServiceInterface(WorkInterface.class);
        invoker.setServiceUrl(SPRING_HTTP_ENDPOINT);
        invoker.afterPropertiesSet();
        WorkInterface worker = (WorkInterface)invoker.getObject();
        ComplexData data = worker.executeComplexity(cd);
        assertNotNull(data);
        assertEquals(data.getSomeString(), "Foo Received");
        assertEquals(data.getSomeInteger(), new Integer(14));
    }
}
