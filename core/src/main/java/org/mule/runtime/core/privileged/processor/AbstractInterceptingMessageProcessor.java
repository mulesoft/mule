/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.processor;

import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;

/**
 * Abstract implementation of {@link InterceptingMessageProcessor} that simply provides an implementation of setNext and holds the
 * next message processor as an attribute.
 * 
 * @deprecated This is kept just because it is in a `privileged` package.
 */
@Deprecated
public abstract class AbstractInterceptingMessageProcessor extends AbstractInterceptingMessageProcessorBase
    implements InterceptingMessageProcessor {
}
