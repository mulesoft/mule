/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration;

import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PollingTestCase extends FunctionalTestCase
{

    private static List<String> foo = new ArrayList<String>();
    private static List<String> bar = new ArrayList<String>();
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/polling-config.xml";
    }

    @Test
    public void testPolling() throws Exception
    {
        Thread.sleep(5000);
        synchronized (foo)
        {
            assertTrue(foo.size() > 0);
            for (String s: foo)
            {
                assertEquals(s, "foo");
            }
        }
        synchronized (bar)
        {
            assertTrue(bar.size() > 0);
            for (String s: bar)
            {
                assertEquals(s, "bar");
            }
        }
    }

    public static class FooComponent
    {
        public boolean process(String s)
        {
            synchronized (foo)
            {
                if (foo.size() < 10)
                {
                    foo.add(s);
                    return true;
                }
            }
            return false;
        }
    }

    public static class BarComponent
    {
        public boolean process(String s)
        {
            synchronized (bar)
            {
                if (bar.size() < 10)
                {
                    bar.add(s);
                    return true;
                }
            }
            return false;
        }
    }
}
