/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.junit.Test;

/**
 * There is a separate transaction for each service when single transaction(action:
 * BEGIN_OR_JOIN) and jms transport are used
 */
public class JmsSingleTransactionComponentTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsSingleTransactionComponentTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

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
