/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.logging;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertNotNull;

public class MuleLoggerFactoryTestCase
{

    private MuleLoggerFactory mlf;

    @Before
    public void setup()
    {
        mlf = new MuleLoggerFactory();
    }

    @Test
    public void testGetLoggerString()
    {
        Logger logger = mlf.getLogger("testLogger");
        assertNotNull(logger);
    }

    @Test
    public void testGetLoggerStringClassLoader()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String loggerName = "testLogger";
        Logger logger = mlf.getLogger(loggerName, cl);
        assertNotNull(logger);
        Logger logger2 = mlf.getLogger(loggerName, cl);
        assertNotNull(logger2);
    }

    @Test
    public void testGetLoggerStringWithoutClassloader()
    {
        Logger logger = mlf.getLogger("testLogger", null);
        assertNotNull(logger);
    }

    @Test
    public void testGetLoggerStringWithRootLogger()
    {
        Logger logger = mlf.getLogger(Logger.ROOT_LOGGER_NAME, null);
        assertNotNull(logger);
    }

}


