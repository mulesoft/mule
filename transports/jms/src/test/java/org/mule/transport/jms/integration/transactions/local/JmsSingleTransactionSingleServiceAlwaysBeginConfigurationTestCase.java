/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration.transactions.local;

import org.mule.transport.jms.integration.AbstractJmsSingleTransactionSingleServiceTestCase;

/**
 * Test all combinations of (inbound) ALWAYS_BEGIN.  They should all pass.
 */
public class JmsSingleTransactionSingleServiceAlwaysBeginConfigurationTestCase extends
    AbstractJmsSingleTransactionSingleServiceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/transactions/local/jms-single-tx-single-service-always-begin.xml";
    }
}
