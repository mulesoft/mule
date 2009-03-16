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

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AbstractJmsFunctionalTestSuite 
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("JMS Functional Tests");
        suite.addTestSuite(JmsClientAcknowledgeTransactionTestCase.class);
        suite.addTestSuite(JmsConnectorJndiTestCase.class);
        suite.addTestSuite(JmsDurableTopicTestCase.class);
        suite.addTestSuite(JmsExceptionStrategyTestCase.class);
        suite.addTestSuite(JmsMessageAwareTransformersMule2685TestCase.class);
        suite.addTestSuite(JmsMuleSideDurableTopicTestCase.class);
        suite.addTestSuite(JmsMuleSideDurableTopicXATxTestCase.class);
        suite.addTestSuite(JmsQueueTestCase.class);
        suite.addTestSuite(JmsQueueWithCompressionTestCase.class);
        suite.addTestSuite(JmsQueueWithTransactionTestCase.class);
        suite.addTestSuite(JmsRemoteSyncMule2868TestCase.class);
        suite.addTestSuite(JmsSingleTransactionAlwaysBeginConfigurationTestCase.class);
        suite.addTestSuite(JmsSingleTransactionBeginOrJoinAndAlwaysBeginTestCase.class);
        suite.addTestSuite(JmsSingleTransactionComponentTestCase.class);
        suite.addTestSuite(JmsSingleTransactionNoneTestCase.class);
        suite.addTestSuite(JmsSingleTransactionRecieveAndSendTestCase.class);
        suite.addTestSuite(JmsTemporaryReplyToTestCase.class);
        suite.addTestSuite(JmsTopicTestCase.class);
        suite.addTestSuite(JmsTransformersTestCase.class);
        suite.addTestSuite(JmsXAAlwaysBeginTestCase.class);
        suite.addTestSuite(JmsXATransactionComponentTestCase.class);
        return suite;
    }
}
