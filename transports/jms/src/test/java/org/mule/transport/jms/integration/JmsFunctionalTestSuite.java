/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

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
              JmsMuleSideDurableTopicXATxTestCase.class,
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
              JmsXAAlwaysBeginTestCase.class,
              JmsXATransactionComponentTestCase.class})
              
public class JmsFunctionalTestSuite
{
    // empty
}
