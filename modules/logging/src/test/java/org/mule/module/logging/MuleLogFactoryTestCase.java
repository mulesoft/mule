/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


