/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.dynamic;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.transport.AbstractConnector;

/**
 * A placeholder for a connector that has not been created yet. This used by dynamic endpoints who's actual endpoint is
 * not created until the first message is received for processing. At that point the real endpoint is created and the 'NullEndpoint'
 * including this NullConnector is overwritten.
 *
 * @since 3.0
 * @see org.mule.runtime.core.endpoint.DynamicOutboundEndpoint
 */
public class NullConnector extends AbstractConnector
{
    public NullConnector(MuleContext context) throws MuleException
    {
        super(context);
        //We call Initialise here since this connector will never get added to the registry, but we still need to have
        //it initialised to avoid NPEs and will be thrown away once the first message is received by a dynamic endpoint
        initialise();
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        //do nothing
    }

    @Override
    protected void doDispose()
    {
        //do nothing
    }

    @Override
    protected void doStart() throws MuleException
    {
        //do nothing
    }

    @Override
    protected void doStop() throws MuleException
    {
        //do nothing
    }

    @Override
    protected void doConnect() throws Exception
    {
        //do nothing
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        //do nothing
    }

    @Override
    public String getProtocol()
    {
        return "dynamic";
    }
}
