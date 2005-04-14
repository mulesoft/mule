/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.email;

import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

/**
 * <code>MailMessageDispatcher</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MailMessageDispatcher extends AbstractMessageDispatcher
{
    private MailConnector connector;

    public MailMessageDispatcher(MailConnector connector)
    {
        super(connector);
        this.connector = connector;
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        connector.getSmtpConnector().getDispatcher(event.getEndpoint().getEndpointURI().getAddress()).dispatch(event);
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        return connector.getSmtpConnector().getDispatcher(event.getEndpoint().getEndpointURI().getAddress()).send(event);
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        return connector.getPop3Connector().getDispatcher(endpointUri.getAddress()).receive(endpointUri, timeout);
    }

    public Object getDelegateSession() throws UMOException
    {
        return connector.getSmtpConnector().getDispatcher("ANY").getDelegateSession();
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose()
    {
    }
}
