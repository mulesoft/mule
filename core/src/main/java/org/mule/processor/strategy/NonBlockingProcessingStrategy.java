/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.strategy;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.config.ChainedThreadingProfile;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.util.concurrent.ThreadNameHelper;

import java.util.List;

/**
 * This strategy allows Mule to use non-blocking execution model where possible and free up threads when performing IO
 * operations.
 *
 * @since 3.7
 */
public class NonBlockingProcessingStrategy extends SynchronousProcessingStrategy
{


}
