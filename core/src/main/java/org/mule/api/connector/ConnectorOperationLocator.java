/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.connector;

import org.mule.api.MuleException;
import org.mule.api.client.Options;
import org.mule.api.processor.MessageProcessor;

/**
 * Locator for a MessageProcessor which is an operation from a Mule connector
 * that fulfils the operation required.
 */
public interface ConnectorOperationLocator
{

    MessageProcessor locateConnectorOperation(String url, Options options) throws MuleException;

    MessageProcessor locateConnectorOperation(String url) throws MuleException;

    MessageProcessor locateFireAndForgetConnectorOperation(String url, Options options) throws MuleException;

    MessageProcessor locateFireAndForgetConnectorOperation(String url) throws MuleException;

}
