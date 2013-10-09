/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.logging;

import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class MuleLogFactoryTestCase
{

    private MuleLogFactory mlf;

    @Before
    public void setup()
    {
        mlf = new MuleLogFactory();
    }

    @Test
    public void testGetInstanceString()
    {
        Log log = mlf.getInstance("testLog");
        assertNotNull(log);
    }

    @Test
    public void testGetInstanceStringNullClassloader()
    {
        Thread.currentThread().setContextClassLoader(null);
        Log log = mlf.getInstance("testLog");
        assertNotNull(log);
    }

    @Test
    public void testGetInstanceTwice()
    {
        Log log = mlf.getInstance("testLog");
        assertNotNull(log);
        Log log2 = mlf.getInstance("testLog");
        assertNotNull(log2);
    }

}


