/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryContext;
import org.mule.api.transport.Connectable;

public class TestNotSerializableConnectable implements Connectable
{

    @Override
    public void initialise() throws InitialisationException
    {
    }

    @Override
    public void start() throws MuleException
    {
    }

    @Override
    public void stop() throws MuleException
    {
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void connect() throws Exception
    {
    }

    @Override
    public void disconnect() throws Exception
    {
    }

    @Override
    public boolean isConnected()
    {
        return false;
    }

    @Override
    public String getConnectionDescription()
    {
        return null;
    }

    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        return null;
    }
}
