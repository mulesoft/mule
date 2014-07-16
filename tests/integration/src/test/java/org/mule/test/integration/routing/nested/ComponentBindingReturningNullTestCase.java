/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.nested;

import static org.junit.Assert.assertTrue;

import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ComponentBindingReturningNullTestCase extends FunctionalTestCase
{
    private static boolean nullResultAccepted;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/nested/component-binding-returning-null-config.xml";
    }

    @Test
    public void componentBindingAcceptNullResult() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        client.send("vm://testInput", "test", null);

        assertTrue(nullResultAccepted);
    }

    public static interface HelloInterface
    {

        public String sayHello(String s);
    }

    public static class HelloImpl implements HelloInterface
    {
        @Override
        public String sayHello(String s)
        {
            return null;
        }
    }

    public static class Component
    {
        private HelloInterface helloInterface;

        public String invoke(String s)
        {
            String result = helloInterface.sayHello(s);
            nullResultAccepted = true;

            return result;
        }

        public void setInterface(HelloInterface helloInterface)
        {
            this.helloInterface = helloInterface;
        }

        public HelloInterface getInterface()
        {
            return helloInterface;
        }
    }
}
