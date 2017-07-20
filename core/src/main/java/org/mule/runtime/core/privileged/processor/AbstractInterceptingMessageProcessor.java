/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;

/**
 * Abstract implementation of {@link InterceptingMessageProcessor} that simply provides an implementation of setNext and holds the
 * next message processor as an attribute.
 */
public abstract class AbstractInterceptingMessageProcessor extends AbstractInterceptingMessageProcessorBase
    implements InterceptingMessageProcessor {
}
