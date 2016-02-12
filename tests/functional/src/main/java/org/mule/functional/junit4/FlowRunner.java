/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.junit.Assert.fail;
import static org.mule.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.RECEIVE_TIMEOUT;

import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transport.ReplyToHandler;
import org.mule.construct.Flow;
import org.mule.functional.functional.FlowAssert;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.transaction.MuleTransactionConfig;

import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.Transformer;

/**
 * Provides a fluent API for running events through flows.
 * 
 * This runner is <b>not</b> thread-safe.
 */
public class FlowRunner extends FlowConstructRunner<FlowRunner>
{

    private String flowName;

    private ExecutionTemplate<MuleEvent> txExecutionTemplate = new ExecutionTemplate<MuleEvent>()
    {
        @Override
        public MuleEvent execute(ExecutionCallback<MuleEvent> callback) throws Exception
        {
            return callback.process();
        }
    };

    private ReplyToHandler replyToHandler;

    private Transformer responseEventTransformer = new Transformer()
    {
        @Override
        public Object transform(Object input)
        {
            return input;
        }
    };

    /**
     * Initializes this flow runner.
     * 
     * @param muleContext the context of the mule application
     * @param flowName the name of the flow to run events through
     */
    public FlowRunner(MuleContext muleContext, String flowName)
    {
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
    public FlowRunner transactionally(TransactionConfigEnum action, TransactionFactory factory)
    {
        MuleTransactionConfig transactionConfig = new MuleTransactionConfig(action.getAction());
        transactionConfig.setFactory(factory);

        txExecutionTemplate = createTransactionalExecutionTemplate(muleContext, transactionConfig);
        eventBuilder.transactionally();

        return this;
    }

    /**
     * Configures this runner's flow to be run non-blocking.
     * 
     * @return this {@link FlowRunner}
     */
    public FlowRunner nonBlocking()
    {
        replyToHandler = new SensingNullReplyToHandler();
        eventBuilder.withReplyToHandler(replyToHandler);

        responseEventTransformer = new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                MuleEvent responseEvent = (MuleEvent) input;
                SensingNullReplyToHandler nullSensingReplyToHandler = (SensingNullReplyToHandler) replyToHandler;
                if (NonBlockingVoidMuleEvent.getInstance() == responseEvent)
                {
                    try
                    {
                        if (!nullSensingReplyToHandler.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
                        {
                            throw new RuntimeException("No Non-Blocking Response");
                        }
                        if (nullSensingReplyToHandler.exception != null)
                        {
                            throw nullSensingReplyToHandler.exception;
                        }
                    }
                    catch (Exception e)
                    {
                        throw new MuleRuntimeException(e);
                    }
                }
                return nullSensingReplyToHandler.event;
            }
        };

        return this;
    }

    /**
     * Runs the specified flow with the provided event and configuration, and performs a
     * {@link FlowAssert#verify(String))} afterwards.
     * 
     * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
     * {@link #reset()}.
     * 
     * @return the resulting <code>MuleEvent</code>
     * @throws Exception
     */
    public MuleEvent run() throws MuleException, Exception
    {
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
    public MuleEvent runNoVerify() throws MuleException, Exception
    {
        return runAndVerify(new String[] {});
    }

    /**
     * Runs the specified flow with the provided event and configuration, and performs a
     * {@link FlowAssert#verify(String))} for each {@code flowNamesToVerify} afterwards.
     * 
     * If this is called multiple times, the <b>same</b> event will be sent. To force the creation of a new event, use
     * {@link #reset()}.
     * 
     * @param flowNamesToVerify the names of the flows to {@link FlowAssert#verify(String))} afterwards.
     * @return the resulting <code>MuleEvent</code>
     * @throws Exception
     */
    public MuleEvent runAndVerify(String... flowNamesToVerify) throws MuleException, Exception
    {
        Flow flow = (Flow) getFlowConstruct();
        MuleEvent responseEvent = txExecutionTemplate.execute(new ExecutionCallback<MuleEvent>()
        {
            @Override
            public MuleEvent process() throws Exception
            {
                return flow.process(getOrBuildEvent());
            }
        });
        for (String flowNameToVerify : flowNamesToVerify)
        {
            FlowAssert.verify(flowNameToVerify);
        }

        return (MuleEvent) responseEventTransformer.transform(responseEvent);
    }

    /**
     * Runs the specified flow with the provided event and configuration expecting a failure. Will fail if there's no
     * failure running the flow.
     *
     * @return the message exception return by the flow
     * @throws Exception
     */
    public MessagingException runExpectingException() throws Exception
    {
        try
        {
            run();
            fail("Flow executed successfully. Expecting exception");
            return null;
        }
        catch (MessagingException e)
        {
            return e;
        }
    }

    @Override
    public String getFlowConstructName()
    {
        return flowName;
    }
}
