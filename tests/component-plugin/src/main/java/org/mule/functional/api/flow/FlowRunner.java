/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.flow;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.tck.processor.FlowAssert;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import reactor.core.publisher.MonoProcessor;

/**
 * Provides a fluent API for running events through flows.
 * <p>
 * This runner is <b>not</b> thread-safe.
 */
public class FlowRunner extends FlowConstructRunner<FlowRunner> implements Disposable {

  private String flowName;

  private ExecutionTemplate<BaseEvent> txExecutionTemplate = callback -> callback.process();

  private Function<BaseEvent, BaseEvent> responseEventTransformer = input -> input;

  private Scheduler scheduler;

  private MonoProcessor externalCompletionCallback = MonoProcessor.create();

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
   * Makes all open {@link Cursor cursors} to not be closed when the executed flow is finished but when the test is disposed
   *
   * @return {@code this} {@link FlowRunner}
   */
  public FlowRunner keepStreamsOpen() {
    eventBuilder.setExternalCompletionCallback(externalCompletionCallback);
    return this;
  }

  /**
   * Run {@link Flow} as a task of a given {@link Scheduler}.
   *
   * @param scheduler the scheduler to use to run the {@link Flow}.
   * @return this {@link FlowRunner}
   * @see {@link SchedulerService}
   */
  public FlowRunner withScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
    return this;
  }

  /**
   * Runs the specified flow with the provided event and configuration, and performs a {@link FlowAssert#verify(String))}
   * afterwards.
   * <p>
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   *
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  public BaseEvent run() throws Exception {
    return runAndVerify(flowName);
  }

  /**
   * Runs the specified flow with the provided event and configuration.
   * <p>
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   *
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  public BaseEvent runNoVerify() throws Exception {
    return runAndVerify(new String[] {});
  }

  /**
   * Runs the specified flow with the provided event and configuration, and performs a {@link FlowAssert#verify(String))} for each
   * {@code flowNamesToVerify} afterwards.
   * <p>
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   *
   * @param flowNamesToVerify the names of the flows to {@link FlowAssert#verify(String))} afterwards.
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  public BaseEvent runAndVerify(String... flowNamesToVerify) throws Exception {
    Flow flow = (Flow) getFlowConstruct();
    BaseEvent response;
    if (scheduler == null) {
      response = txExecutionTemplate.execute(getFlowRunCallback(flow));
    } else {
      try {
        response = scheduler.submit(() -> txExecutionTemplate.execute(getFlowRunCallback(flow))).get();
      } catch (ExecutionException executionException) {
        Throwable cause = executionException.getCause();
        throw cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
      }
    }
    verify(flowNamesToVerify);
    return responseEventTransformer.apply(response);
  }

  /**
   * Dispatches to the specified flow with the provided event and configuration, and performs a {@link FlowAssert#verify(String))}
   * afterwards.
   * <p>
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   * <p>
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
   * <p>
   * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
   * {@link #reset()}.
   * <p>
   * Dispatch behaves differently to {@link FlowRunner#run()} in that it does not propagate any exceptions to the test case or
   * return a result.
   */
  public void dispatchAsync() throws Exception {
    Flow flow = (Flow) getFlowConstruct();
    scheduler =
        muleContext.getSchedulerService().ioScheduler(muleContext.getSchedulerBaseConfig().withShutdownTimeout(0, SECONDS));
    try {
      scheduler.submit(() -> txExecutionTemplate.execute(getFlowDispatchCallback(flow)));
    } catch (Exception e) {
      // Ignore
    }
    FlowAssert.verify(flowName);
  }

  private ExecutionCallback<BaseEvent> getFlowRunCallback(final Flow flow) {
    // TODO MULE-13053 Update and improve FlowRunner to support non-blocking flow execution and assertions.
    return () -> flow.process(getOrBuildEvent());
  }

  private ExecutionCallback<BaseEvent> getFlowDispatchCallback(final Flow flow) {
    return () -> {
      // TODO MULE-13053 Update and improve FlowRunner to support non-blocking flow execution and assertions.
      flow.process(getOrBuildEvent());
      return null;
    };
  }

  /**
   * Verifies asserts on flowNamesToVerify
   * 
   * @param flowNamesToVerify
   * @throws Exception
   */
  private void verify(String... flowNamesToVerify) throws Exception {
    for (String flowNameToVerify : flowNamesToVerify) {
      FlowAssert.verify(flowNameToVerify);
    }
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
      runNoVerify();
      fail("Flow executed successfully. Expecting exception");
      return null;
    } catch (MessagingException e) {
      verify(getFlowConstructName());
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
      scheduler.stop();
    }

    externalCompletionCallback.onComplete();
  }
}
