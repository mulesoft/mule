/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.logging;

import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.ClassRule;
import org.junit.Test;

public class StandaloneLogHandlerThreadTestCase extends AbstractLogHandlerThreadTestCase
{

    @ClassRule
    public static SystemProperty muleHome = new SystemProperty(MuleUtils.MULE_HOME, "test");


    public StandaloneLogHandlerThreadTestCase(LoggerFactoryFactory loggerFactory, String logHandlerThreadName)
    {
        super(loggerFactory, logHandlerThreadName);
    }

    @Test
    public void startsLogHandlerThreadOnStandaloneMode() throws Exception
    {
        loggerFactory.create();

        assertTrue("Did not create expected LoggerReferenceHandler instance", createdLoggerReferenceHandler);
    }
}
