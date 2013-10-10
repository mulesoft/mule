/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.remoting;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpringRemotingTestCase extends FunctionalTestCase
{

    private static final String SPRING_HTTP_ENDPOINT = "http://localhost:8003/springService";

    @Override
    protected String getConfigResources()
    {
        return "spring-remoting-mule-config.xml";
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
