/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * A test suite for JUnit 4 consisting of all JMS functional tests which have been 
 * parameterized in order to support more than one JMS provider.  This test suite
 * can be run/debugged conveniently from an IDE (e.g., in Eclipse "Run As/JUnit Test").
 */
@RunWith(Suite.class)
@SuiteClasses({JmsClientAcknowledgeTransactionTestCase.class,
              JmsConnectorJndiTestCase.class,
              JmsDurableTopicTestCase.class,
              JmsDeadLetterQueueTestCase.class,
              JmsMessageAwareTransformersMule2685TestCase.class,
              JmsMuleSideDurableTopicTestCase.class,
              JmsQueueTestCase.class,
              JmsQueueMessageTypesTestCase.class,
              JmsQueueWithCompressionTestCase.class,
              JmsQueueWithTransactionTestCase.class,
              JmsRemoteSyncMule2868TestCase.class,
              JmsSingleTransactionAlwaysBeginConfigurationTestCase.class,
              JmsSingleTransactionBeginOrJoinAndAlwaysBeginTestCase.class,
              JmsSingleTransactionComponentTestCase.class,
              JmsSingleTransactionNoneTestCase.class,
              JmsSingleTransactionRecieveAndSendTestCase.class,
              JmsTemporaryReplyToTestCase.class,
              JmsTopicTestCase.class,
              JmsTransformersTestCase.class,
              JmsXAAlwaysBeginTestCase.class})
              
public class JmsFunctionalTestSuite
{
    // empty
}
