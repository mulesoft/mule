/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.source.MessageSource;
import org.mule.api.transaction.TransactionConfig;

/**
 * Context for processing one message from a {@link org.mule.api.source.MessageSource}.
 *
 * Mule {@link org.mule.api.source.MessageSource} implementations should create one instance of MessageProcessContext per message
 * that generates.
 *
 * MessageProcessContext is responsible for
 * - Define if the incoming message can be processed in a separate thread
 * - Provide access to the {@link MessageSource} of the message
 * - Provide access to the {@link FlowConstruct} were the message is going to be executed
 * - Provide access, if available, to the {@link WorkManager} to use for processing the message
 * - Provide the {@link MessageSource} transaction configuration
 */
public interface MessageProcessContext
{

    /**
     * @return true if the message can be processed in a different thread than the one it was acquired, false otherwise
     */
    boolean supportsAsynchronousProcessing();

    /**
     * @return the {@link MessageSource} that retrieve the message. Can not be null
     */
    MessageSource getMessageSource();

    /**
     * @return the {@link FlowConstruct} were the incoming message is going to be executed. Can not be null
     */
    FlowConstruct getFlowConstruct();

    /**
     * @return the {@link WorkManager} were the incoming message must be processed.
     * If null it will be executed in the same thread were the message was received
     */
    WorkManager getFlowExecutionWorkManager();

    /**
     * @return the {@link TransactionConfig} associated to the {@link MessageSource} that received the message.
     * If null then no transaction config will be used.
     */
    TransactionConfig getTransactionConfig();

    /**
     * @return the class loader to be used for processing the message.
     */
    ClassLoader getExecutionClassLoader();
}
