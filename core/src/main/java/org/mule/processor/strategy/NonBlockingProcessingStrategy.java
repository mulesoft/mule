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
 * This strategy uses a {@link javax.resource.spi.work.WorkManager} to schedule the processing of the pipeline of
 * message processors
 * in a single worker thread.
 */
public class NonBlockingProcessingStrategy extends SynchronousProcessingStrategy
{


}
