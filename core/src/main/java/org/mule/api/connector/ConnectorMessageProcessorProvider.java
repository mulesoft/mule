/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.connector;

import org.mule.api.MuleException;
import org.mule.api.client.Options;

public interface ConnectorMessageProcessorProvider
{

    public boolean supportsUrl(final String url);

    public ConnectorMessageProcessor getMessageProcessor(String url) throws MuleException;

    public ConnectorMessageProcessor getMessageProcessor(String url, Options options) throws MuleException;

    public ConnectorMessageProcessor getFireAndForgetMessageProcessor(String url) throws MuleException;

    public ConnectorMessageProcessor getFireAndForgetMessageProcessor(String url, Options options) throws MuleException;

}
