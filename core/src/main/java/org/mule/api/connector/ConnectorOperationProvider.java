/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.connector;

import org.mule.api.MuleException;
import org.mule.api.client.OperationOptions;
import org.mule.api.processor.MessageProcessor;

/**
 * Provider of operation for a Mule connector.
 *
 * A Mule connector can provide an implementation of this interface in the registry and mule
 * will use it to create operations using an url and later executed them.
 *
 * The implementation must be located in the mule registry before the start phase.
 */
public interface ConnectorOperationProvider
{

    public boolean supportsUrl(final String url);

    public MessageProcessor getRequestResponseMessageProcessor(String url) throws MuleException;

    public MessageProcessor getRequestResponseMessageProcessor(String url, OperationOptions operationOptions) throws MuleException;

    public MessageProcessor getOneWayMessageProcessor(String url) throws MuleException;

    public MessageProcessor getOneWayMessageProcessor(String url, OperationOptions operationOptions) throws MuleException;

}
