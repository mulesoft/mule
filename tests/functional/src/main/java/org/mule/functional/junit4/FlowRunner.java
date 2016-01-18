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
import org.mule.api.MuleMessage;
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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.Transformer;
import org.mockito.Mockito;

/**
 * Provides a fluent API for running events through flows.
 * 
 * This runner is <b>not</b> thread-safe.
 */
public class FlowRunner
{

    private MuleContext muleContext;

    private String flowName;

    private TestEventBuilder eventBuilder = new TestEventBuilder();

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
        this.muleContext = muleContext;
        this.flowName = flowName;
    }

    /**
     * Prepares the given data to be sent as the payload of the {@link MuleEvent} to the configured flow.
     * 
     * @param payload the payload to use in the message
     * @return this {@link FlowRunner}
     */
    public FlowRunner withPayload(Object payload)
    {
        eventBuilder.withPayload(payload);

        return this;
    }

    /**
     * Prepares a property with the given key and value to be sent as an inbound property of the {@link MuleMessage} to
     * the configured flow.
     * 
     * @param key the key of the inbound property to add
     * @param value the value of the inbound property to add
     * @return this {@link FlowRunner}
     */
    public FlowRunner withInboundProperty(String key, Object value)
    {
        eventBuilder.withInboundProperty(key, value);

        return this;
    }

    /**
     * Prepares the given properties map to be sent as inbound properties of the {@link MuleMessage} to the configured
     * flow.
     * 
     * @param properties the inbound properties to add
     * @return this {@link FlowRunner}
     */
    public FlowRunner withInboundProperties(Map<String, Object> properties)
    {
        eventBuilder.withInboundProperties(properties);

        return this;
    }

    /**
     * Prepares a property with the given key and value to be sent as an outbound property of the {@link MuleMessage} to
     * the configured flow.
     * 
     * @param key the key of the outbound property to add
     * @param value the value of the outbound property to add
     * @return this {@link FlowRunner}
     */
    public FlowRunner withOutboundProperty(String key, Object value)
    {
        eventBuilder.withOutboundProperty(key, value);

        return this;
    }

    /**
     * Prepares a property with the given key and value to be sent as a session property of the {@link MuleMessage} to
     * the configured flow.
     * 
     * @param key the key of the session property to add
     * @param value the value of the session property to add
     * @return this {@link FlowRunner}
     */
    public FlowRunner withSessionProperty(String key, Object value)
    {
        eventBuilder.withSessionProperty(key, value);

        return this;
    }

    /**
     * Prepares a property with the given key and value to be sent as an invocation property of the {@link MuleMessage}
     * to the configured flow.
     * 
     * @param key the key of the invocation property to add
     * @param value the value of the invocation property to add
     * @return this {@link FlowRunner}
     */
    public FlowRunner withInvocationProperty(String key, Object value)
    {
        eventBuilder.withInvocationProperty(key, value);

        return this;
    }


    /**
     * Prepares a flow variable with the given key and value to be set in the {@link MuleMessage} to the configured
     * flow.
     * 
     * @param key the key of the flow variable to put
     * @param value the value of the flow variable to put
     * @return this {@link FlowRunner}
     */
    public FlowRunner withFlowVariable(String key, Object value)
    {
        eventBuilder.withFlowVariable(key, value);

        return this;
    }

    /**
     * Configures this runner to run this flow as one-way.
     * 
     * @return this {@link FlowRunner}
     */
    public FlowRunner asynchronously()
    {
        eventBuilder.asynchronously();

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
     * Configures the flow to run inside a transaction.
     * 
     * @param action See {@link TransactionConfig} constants
     * @param factory See {@link MuleTransactionConfig#setFactory(TransactionFactory)}.
     * @return this {@link FlowRunner}
     */
    public FlowRunner transactionally(byte action, TransactionFactory factory)
    {
        MuleTransactionConfig transactionConfig = new MuleTransactionConfig(action);
        transactionConfig.setFactory(factory);

        txExecutionTemplate = createTransactionalExecutionTemplate(muleContext, transactionConfig);
        eventBuilder.transactionally();

        return this;
    }

    /**
     * Will spy the built {@link MuleMessage} and {@link MuleEvent}. See {@link Mockito#spy(Object) spy}.
     * 
     * @return this {@link FlowRunner}
     */
    public FlowRunner spyObjects()
    {
        eventBuilder.spyObjects();

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
        Flow flow = getFlowConstruct(flowName);
        MuleEvent responseEvent = txExecutionTemplate.execute(new ExecutionCallback<MuleEvent>()
        {
            @Override
            public MuleEvent process() throws Exception
            {
                return flow.process(buildEvent());
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

    private MuleEvent requestEvent;

    /**
     * Builds a new event based on this runner's config. If one has already been built, it will return that one.
     * 
     * @return an event that would be used to go through the flow.
     */
    public MuleEvent buildEvent()
    {
        if (requestEvent == null)
        {
            Flow flow = getFlowConstruct(flowName);
            requestEvent = eventBuilder.build(muleContext, flow);
        }
        return requestEvent;
    }

    protected Flow getFlowConstruct(String flowName)
    {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
    }

    /**
     * Clears the last built requestEvent, allowing for reuse of this runner.
     */
    public void reset()
    {
        requestEvent = null;
    }
}
