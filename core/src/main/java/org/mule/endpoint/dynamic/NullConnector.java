/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.dynamic;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractConnector;

/**
 * A placeholder for a connector that has not been created yet. This used by dynamic endpoints who's actual endpoint is
 * not created until the first message is received for processing. At that point the real endpoint is created and the 'NullEndpoint'
 * including this NullConnector is overwritten.
 *
 * @since 3.0
 * @see org.mule.endpoint.DynamicOutboundEndpoint
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

    public String getProtocol()
    {
        return "dynamic";
    }
}
