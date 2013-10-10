/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


