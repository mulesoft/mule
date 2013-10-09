/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
