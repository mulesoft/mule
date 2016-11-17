/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.junit.Assert.fail;
import static org.mule.runtime.core.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.tck.MuleTestUtils.processAsStreamAndBlock;
import org.mule.functional.functional.FlowAssert;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import org.apache.commons.collections.Transformer;

/**
 * Provides a fluent API for running events through flows.
 * 
 * This runner is <b>not</b> thread-safe.
 */
public class FlowRunner extends FlowConstructRunner<FlowRunner> {

  private String flowName;

  private ExecutionTemplate<Event> txExecutionTemplate = callback -> callback.process();

  private Transformer responseEventTransformer = input -> input;
  private boolean nonBlocking = false;

  /**
   * Initializes this flow runner.
   * 
   * @param muleContext the context of the mule application
   * @param flowName the name of the flow to run events through
   */
  public FlowRunner(MuleContext muleContext, String flowName) {
    super(muleContext);
    this.flowName = flowName;
  }

  /**
   * Configures the flow to run inside a transaction.
   * 
   * @param action The action to do at the start of the transactional block. See {@link TransactionConfig} constants.
   * @param factory See {@link MuleTransactionConfig#setFactory(TransactionFactory)}.
   * @return this {@link FlowRunner}
   */
  public FlowRunner transactionally(TransactionConfigEnum action, TransactionFactory factory) {
    MuleTransactionConfig transactionConfig = new MuleTransactionConfig(action.getAction());
    transactionConfig.setFactory(factory);

    txExecutionTemplate = createTransactionalExecutionTemplate(muleContext, transactionConfig);

    return this;
  }

  /**
   * Configures this runner's flow to be run non-blocking.
   * 
   * @return this {@link FlowRunner}
   */
  public FlowRunner nonBlocking() {
    nonBlocking = true;
    return this;
  }

  /**
   * Runs the specified flow with the provided event and configuration, and performs a {@link FlowAssert#verify(String))}
   * afterwards.
   * 
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   * 
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  public Event run() throws Exception {
    return runAndVerify(flowName);
  }

  /**
   * Runs the specified flow with the provided event and configuration.
   * 
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   * 
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  public Event runNoVerify() throws Exception {
    return runAndVerify(new String[] {});
  }

  /**
   * Runs the specified flow with the provided event and configuration, and performs a {@link FlowAssert#verify(String))} for each
   * {@code flowNamesToVerify} afterwards.
   * 
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   * 
   * @param flowNamesToVerify the names of the flows to {@link FlowAssert#verify(String))} afterwards.
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  public Event runAndVerify(String... flowNamesToVerify) throws Exception {
    Flow flow = (Flow) getFlowConstruct();
    Event response = txExecutionTemplate.execute(getFlowExecutionCallback(flow));

    for (String flowNameToVerify : flowNamesToVerify) {
      FlowAssert.verify(flowNameToVerify);
    }

    return (Event) responseEventTransformer.transform(response);
  }

  /**
   * Dispatchs to the specified flow with the provided event and configuration, and performs a {@link FlowAssert#verify(String))}
   * afterwards.
   *
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   *
   * Dispatch behaves differently to {@link FlowRunner#run()} in that it does not propagate any exceptions to the test case or
   * return a result.
   */
  public void dispatch() {
    try {
      run();
    } catch (Exception e) {
      // Ignore
    }
  }

  private ExecutionCallback getFlowExecutionCallback(final Flow flow) {
    return () -> {
      if (nonBlocking) {
        return processAsStreamAndBlock(getOrBuildEvent(), flow);
      } else {
        return flow.process(getOrBuildEvent());
      }
    };
  }

  /**
   * Runs the specified flow with the provided event and configuration expecting a failure. Will fail if there's no failure
   * running the flow.
   *
   * @return the message exception return by the flow
   * @throws Exception
   */
  public MessagingException runExpectingException() throws Exception {
    try {
      run();
      fail("Flow executed successfully. Expecting exception");
      return null;
    } catch (MessagingException e) {
      return e;
    }
  }

  @Override
  public String getFlowConstructName() {
    return flowName;
  }
}
