/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;

import org.mule.functional.functional.FlowAssert;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.internal.streaming.StreamingManagerAdapter;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import org.apache.commons.collections.Transformer;

/**
 * Provides a fluent API for running events through flows.
 * 
 * This runner is <b>not</b> thread-safe.
 */
public class FlowRunner extends FlowConstructRunner<FlowRunner> implements Disposable {

  private String flowName;

  private ExecutionTemplate<Event> txExecutionTemplate = callback -> callback.process();

  private Transformer responseEventTransformer = input -> input;

  private Scheduler scheduler;

  private StreamingManagerAdapter streamingManager;

  /**
   * Initializes this flow runner.
   * 
   * @param muleContext the context of the mule application
   * @param flowName the name of the flow to run events through
   */
  public FlowRunner(MuleContext muleContext, String flowName) {
    super(muleContext);
    this.flowName = flowName;
    try {
      streamingManager = muleContext.getRegistry().lookupObject(StreamingManagerAdapter.class);
    } catch (RegistrationException e) {
      throw new RuntimeException(e);
    }
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
    Event response = txExecutionTemplate.execute(getFlowRunCallback(flow));

    for (String flowNameToVerify : flowNamesToVerify) {
      FlowAssert.verify(flowNameToVerify);
    }

    return (Event) responseEventTransformer.transform(response);
  }

  /**
   * Dispatches to the specified flow with the provided event and configuration, and performs a {@link FlowAssert#verify(String))}
   * afterwards.
   *
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   *
   * Dispatch behaves differently to {@link FlowRunner#run()} in that it does not propagate any exceptions to the test case or
   * return a result.
   */
  public void dispatch() throws Exception {
    Flow flow = (Flow) getFlowConstruct();
    try {
      txExecutionTemplate.execute(getFlowDispatchCallback(flow));
    } catch (Exception e) {
      // Ignore
    }
    FlowAssert.verify(flowName);
  }

  /**
   * Dispatches to the specified flow with the provided event and configuration in a new IO thread, and performs a
   * {@link FlowAssert#verify(String))} afterwards.
   *
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   *
   * Dispatch behaves differently to {@link FlowRunner#run()} in that it does not propagate any exceptions to the test case or
   * return a result.
   */
  public void dispatchAsync() throws Exception {
    Flow flow = (Flow) getFlowConstruct();
    scheduler = muleContext.getSchedulerService().ioScheduler();
    try {
      scheduler.submit(() -> txExecutionTemplate.execute(getFlowDispatchCallback(flow)));
    } catch (Exception e) {
      // Ignore
    }
    FlowAssert.verify(flowName);
  }

  private ExecutionCallback<Event> getFlowRunCallback(final Flow flow) {
    return () -> {
      Event event = getOrBuildEvent();
      try {
        Event result = flow.process(event);
        event.getContext().success(result);
        streamingManager.success(event);
        return result;
      } catch (Exception e) {
        event.getContext().error(e);
        streamingManager.error(event);
        throw e;
      }
    };
  }

  private ExecutionCallback<Event> getFlowDispatchCallback(final Flow flow) {
    return () -> {
      flow.process(getOrBuildEvent());
      return null;
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

  @Override
  public void dispose() {
    if (scheduler != null) {
      scheduler.stop(0, SECONDS);
    }
  }
}
