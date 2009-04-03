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

import javax.jms.Message;

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
        setTransacted(true);
    }
    
    protected String getConfigResources()
    {
        return "integration/jms-single-tx-component.xml";
    }

    @Test
    public void testSingleTransactionComponent() throws Exception
    {
        sendAndCommit(DEFAULT_INPUT_MESSAGE);
        
        // Receive message but roll back transaction.
        Message output = receiveAndRollback();
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, output);

        // Receive message again and commit transaction.
        output = receiveAndCommit();
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, output);

        // No more messages
        assertNull(receiveNoWait());
    }
}
