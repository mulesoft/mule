/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration.activemq;

import org.mule.transport.jms.integration.JmsClientAcknowledgeTransactionTestCase;
import org.mule.transport.jms.integration.JmsDurableTopicSingleTxTestCase;
import org.mule.transport.jms.integration.JmsDurableTopicTestCase;
import org.mule.transport.jms.integration.JmsExceptionStrategyTestCase;
import org.mule.transport.jms.integration.JmsMuleSideDurableTopicTestCase;
import org.mule.transport.jms.integration.JmsMuleSideDurableTopicXATxTestCase;
import org.mule.transport.jms.integration.JmsVendorConfiguration;

import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * This isn't used right now, but shows that we can introduce a test suite for a Jms provider using this single file and
 * the the connector xml files.  Using this would mean that it is not necessay to create a new VendorXXX test class for
 * each test.
 *
 * TODO Figure out how to integrate this cleanly to maven, Eclipse and IntelliJ
 */
public class ActiveMQTestSuite extends TestSuite
{
    public ActiveMQTestSuite()
    {

        addTest(new JmsClientAcknowledgeTransactionTestCase(getConfiguration()) {});
        addTest(new JmsDurableTopicSingleTxTestCase(getConfiguration()) {});
        addTest(new JmsDurableTopicTestCase(getConfiguration()) {});
        addTest(new JmsExceptionStrategyTestCase(getConfiguration()) {});
        addTest(new JmsMuleSideDurableTopicTestCase(getConfiguration()) {});
        addTest(new JmsMuleSideDurableTopicXATxTestCase(getConfiguration()) {});
        //etc

    }

    public JmsVendorConfiguration getConfiguration()
    {
        return new ActiveMQJmsConfiguration();
    }

    public static void main(String[] args)
    {
        ActiveMQTestSuite ts = new ActiveMQTestSuite();
        ts.run(new TestResult());

    }

}
