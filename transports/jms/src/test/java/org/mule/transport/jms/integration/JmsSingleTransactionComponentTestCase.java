/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.junit.Test;

/**
 * There is a separate transaction for each service when single transaction(action:
 * BEGIN_OR_JOIN) and jms transport are used
 */
public class JmsSingleTransactionComponentTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "integration/jms-single-tx-component.xml";
    }

    @Test
    public void testSingleTransactionComponent() throws Exception
    {
        send(scenarioCommit);
        // Receive message but roll back transaction.
        receive(scenarioRollback);
        // Receive message again and commit transaction.
        receive(scenarioCommit);
        // Verify there is no more message to receive.
        receive(scenarioNotReceive);
    }
}
