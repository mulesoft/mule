/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.logging;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StandaloneLogHandlerThreadTestCase extends AbstractLogHandlerThreadTestCase
{

    private static String originalSystemProperty;

    public StandaloneLogHandlerThreadTestCase(LoggerFactoryFactory loggerFactory, String logHandlerThreadName)
    {
        super(loggerFactory, logHandlerThreadName);
    }

    @BeforeClass
    public static void setUpSystemProperty()
    {
        originalSystemProperty = System.setProperty(MuleUtils.MULE_HOME, "test");
    }

    @AfterClass
    public static void restoreSystemProperty()
    {
        if (originalSystemProperty != null)
        {
            System.setProperty(MuleUtils.MULE_HOME, originalSystemProperty);
        }

    }

    @Test
    public void startsLogHandlerThreadOnStandaloneMode() throws Exception
    {
        loggerFactory.create();

        assertTrue("Did not create expected LoggerReferenceHandler instance", createdLoggerReferenceHandler);
    }
}
