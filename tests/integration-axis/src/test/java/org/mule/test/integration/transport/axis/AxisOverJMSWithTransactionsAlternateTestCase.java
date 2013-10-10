/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.axis;

/**
 * Embedded transaction factory
 */
public class AxisOverJMSWithTransactionsAlternateTestCase extends AbstractAxisOverJMSWithTransactionsTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/axis/axis-over-jms-config-alternate.xml";
    }
}
