/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.exception;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.privileged.event.Acceptor;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;

/**
 * MessagingExceptionHandlers that will only be executed if accepts to manage MuleEvent
 */
@NoImplement
public interface MessagingExceptionHandlerAcceptor extends FlowExceptionHandler, Acceptor {
}
