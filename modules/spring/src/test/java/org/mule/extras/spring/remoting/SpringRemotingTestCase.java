/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.remoting;

import org.mule.tck.FunctionalTestCase;

import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

public class SpringRemotingTestCase extends FunctionalTestCase
{
    private static final String SPRING_HTTP_ENDPOINT = "http://localhost:8003/springService";

    protected String getConfigResources()
    {
        return "spring-remoting-mule-config.xml";
    }

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
