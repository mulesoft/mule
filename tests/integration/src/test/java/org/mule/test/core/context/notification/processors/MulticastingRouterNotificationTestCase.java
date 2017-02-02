/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.junit.Assert.assertNotNull;

import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.Factory;
import org.junit.Test;

public class MulticastingRouterNotificationTestCase extends AbstractMessageProcessorNotificationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml";
  }

  private Factory specificationFactory;

  @Test
  public void all() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // Two routes with chain with one element
        .serial(prePost())
        .serial(prePost())
        .serial(post())
        .serial(prePost()) // MP after the Scope;
    ;

    assertNotNull(flowRunner("all").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void all2() throws Exception {
    specificationFactory = () -> new Node()
        // All
        // Two routes with chain with two first one is interceptiong elements
        .serial(pre())
        // CollectionSplitter
        .serial(pre())
        // Logger
        .serial(prePost())
        // CollectionSplitter
        .serial(post())
        // CollectionSplitter
        .serial(pre())
        // Logger
        .serial(prePost())
        // CollectionSplitter
        .serial(post())
        // All
        .serial(post())
        // Logger
        .serial(prePost()) // MP after the Scope;
    ;

    List<String> testList = Arrays.asList("test");
    assertNotNull(flowRunner("all2").withPayload(testList).run());

    assertNotifications();
  }

  @Test
  public void all3() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // Two routes with no chain with one element
        .serial(prePost())
        .serial(prePost())
        .serial(post())
        .serial(prePost()) // MP after the Scope;
    ;

    assertNotNull(flowRunner("all3").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return (RestrictedNode) specificationFactory.create();
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}
}
