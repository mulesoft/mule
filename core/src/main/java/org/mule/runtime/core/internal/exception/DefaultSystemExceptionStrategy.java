/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

/**
 * This is the default exception handler for any exception which does not inherit from MessagingException, i.e, when no message is
 * in play. The exception handler will fire a notification, log exception, roll back any transaction, and trigger a reconnection
 * strategy if this is a <code>ConnectException</code>.
 */
public class DefaultSystemExceptionStrategy extends AbstractSystemExceptionStrategy {

}
