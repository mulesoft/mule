/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mule.compatibility.core.api.config.MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.registerEndpoint;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REMOTE_SYNC_PROPERTY;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.module.client.MuleClient;
import org.mule.compatibility.transport.jms.JmsTransactionFactory;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.execution.TransactionalExecutionTemplate;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.transaction.TransactionCoordination;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MuleClientTransactionTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/client/test-client-jms-mule-config.xml";
  }

  @Test
  public void testTransactionsWithSetRollbackOnly() throws Exception {
    final MuleClient client = new MuleClient(muleContext);
    final Map<String, Serializable> props = new HashMap<>();
    props.put("JMSReplyTo", "replyTo.queue");
    props.put(MULE_REMOTE_SYNC_PROPERTY, "false");

    // Empty reply queue
    while (client.request("jms://replyTo.queue", 2000) != null) {
      // slurp
    }

    MuleTransactionConfig tc = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    tc.setFactory(new JmsTransactionFactory());

    // This enpoint needs to be registered prior to use cause we need to set
    // the transaction config so that the endpoint will "know" it is transacted
    // and not close the session itself but leave it up to the transaction.
    EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new URIBuilder("jms://test.queue", muleContext));
    endpointBuilder.setTransactionConfig(tc);
    endpointBuilder.setName("TransactedTest.Queue");
    ImmutableEndpoint inboundEndpoint = getEndpointFactory().getOutboundEndpoint(endpointBuilder);
    registerEndpoint(client.getMuleContext().getRegistry(), inboundEndpoint);

    ExecutionTemplate<Void> executionTemplate =
        TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, tc);
    executionTemplate.execute(new ExecutionCallback<Void>() {

      @Override
      public Void process() throws Exception {
        for (int i = 0; i < 100; i++) {
          client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
        }
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        assertNotNull(tx);
        tx.setRollbackOnly();
        return null;
      }
    });

    MuleMessage result = client.request("jms://replyTo.queue", 2000);
    assertNull(result);
  }

  @Test
  public void testTransactionsWithExceptionThrown() throws Exception {
    final MuleClient client = new MuleClient(muleContext);
    final Map<String, Serializable> props = new HashMap<>();
    props.put("JMSReplyTo", "replyTo.queue");
    props.put(MULE_REMOTE_SYNC_PROPERTY, "false");

    // Empty reply queue
    while (client.request("jms://replyTo.queue", 2000) != null) {
      // hmm..mesages
    }

    MuleTransactionConfig tc = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    tc.setFactory(new JmsTransactionFactory());

    // This enpoint needs to be registered prior to use cause we need to set
    // the transaction config so that the endpoint will "know" it is transacted
    // and not close the session itself but leave it up to the transaction.
    EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new URIBuilder("jms://test.queue", muleContext));
    endpointBuilder.setTransactionConfig(tc);
    endpointBuilder.setName("TransactedTest.Queue");
    ImmutableEndpoint inboundEndpoint = getEndpointFactory().getOutboundEndpoint(endpointBuilder);
    registerEndpoint(client.getMuleContext().getRegistry(), inboundEndpoint);

    ExecutionTemplate<Void> executionTemplate =
        TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, tc);
    try {
      executionTemplate.execute(new ExecutionCallback<Void>() {

        @Override
        public Void process() throws Exception {
          for (int i = 0; i < 100; i++) {
            client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
          }
          throw new Exception();
        }
      });
      fail();
    } catch (Exception e) {
      // this is ok
    }

    MuleMessage result = client.request("jms://replyTo.queue", 2000);
    assertNull(result);
  }

  @Test
  public void testTransactionsWithCommit() throws Exception {
    final MuleClient client = new MuleClient(muleContext);
    final Map<String, Serializable> props = new HashMap<>();
    props.put("JMSReplyTo", "replyTo.queue");
    props.put(MULE_REMOTE_SYNC_PROPERTY, "false");
    props.put("transacted", "true");

    // Empty reply queue
    while (client.request("jms://replyTo.queue", 2000) != null) {
      // yum!
    }

    MuleTransactionConfig tc = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    tc.setFactory(new JmsTransactionFactory());

    // This enpoint needs to be registered prior to use cause we need to set
    // the transaction config so that the endpoint will "know" it is transacted
    // and not close the session itself but leave it up to the transaction.
    EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new URIBuilder("jms://test.queue", muleContext));
    endpointBuilder.setTransactionConfig(tc);
    endpointBuilder.setName("TransactedTest.Queue");
    ImmutableEndpoint inboundEndpoint = getEndpointFactory().getOutboundEndpoint(endpointBuilder);
    registerEndpoint(client.getMuleContext().getRegistry(), inboundEndpoint);

    ExecutionTemplate<Void> executionTemplate =
        TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, tc);
    executionTemplate.execute(new ExecutionCallback<Void>() {

      @Override
      public Void process() throws Exception {
        for (int i = 0; i < 100; i++) {
          client.send("jms://test.queue", "Test Client Dispatch message " + i, props);
        }
        return null;
      }
    });

    for (int i = 0; i < 100; i++) {
      MuleMessage result = client.request("jms://replyTo.queue", 2000);
      assertNotNull(result);
    }
    MuleMessage result = client.request("jms://replyTo.queue", 2000);
    assertNull(result);
  }

  protected void emptyReplyQueue() throws Exception {
    final MuleClient client = new MuleClient(muleContext);
    MuleTransactionConfig tc = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    tc.setFactory(new JmsTransactionFactory());
    ExecutionTemplate<Void> executionTemplate =
        TransactionalExecutionTemplate.createTransactionalExecutionTemplate(muleContext, tc);
    executionTemplate.execute(new ExecutionCallback<Void>() {

      @Override
      public Void process() throws Exception {
        while (client.request("jms://replyTo.queue", 2000) != null) {
          // munch..
        }

        return null;
      }
    });
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(OBJECT_MULE_ENDPOINT_FACTORY);
  }

}
