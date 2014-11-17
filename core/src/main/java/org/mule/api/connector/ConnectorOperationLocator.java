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
 * Locator for a MessageProcessor which is an operation from a Mule connector
 * that fulfils the operation required.
 */
public interface ConnectorOperationLocator
{

    /**
     * Lookup for a request response operation from a connector
     *
     * @param url the URL that identifies the operation
     * @param operationOptions the options to use to execute the operation
     * @return the operation configured for the url and options
     * @throws MuleException
     */
    MessageProcessor locateRequestResponseConnectorOperation(String url, OperationOptions operationOptions) throws MuleException;

    /**
     * Lookup for a request response operation from a connector using the default operation configuration
     *
     * @param url the URL that identifies the operation
     * @return the operation configured for the url and default options
     * @throws MuleException
     */
    MessageProcessor locateRequestResponseConnectorOperation(String url) throws MuleException;

    /**
     * Lookup for a one way operation from a connector
     *
     * @param url the URL that identifies the operation
     * @param operationOptions the options to use to execute the operation
     * @return the operation configured for the url and options.
     * @throws MuleException
     */
    MessageProcessor locateOneWayConnectorOperation(String url, OperationOptions operationOptions) throws MuleException;

    /**
     *  Lookup for a one way operation from a connector using the default operation configuration
     *
     * @param url the URL that identifies the operation
     * @return the operation configured for the url and default options
     * @throws MuleException
     */
    MessageProcessor locateOneWayConnectorOperation(String url) throws MuleException;

}
