/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
