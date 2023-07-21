/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.exception;

/**
 * This is the default exception handler for any exception which does not inherit from MessagingException, i.e, when no message is
 * in play. The exception handler will fire a notification, log exception, roll back any transaction, and trigger a reconnection
 * strategy if this is a <code>ConnectException</code>.
 */
public class DefaultSystemExceptionStrategy extends AbstractSystemExceptionStrategy {

}
