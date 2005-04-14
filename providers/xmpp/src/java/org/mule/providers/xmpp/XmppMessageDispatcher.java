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
package org.mule.providers.xmpp;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>XmppMessageDispatcher</code> allows Mule events to be sent and recieved over Xmpp
 *
 * @author Peter Braswell
 * @version $Revision$
 */

public class XmppMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory
            .getLog(XmppMessageDispatcher.class);

    private XmppConnector connector;

    private SynchronizedBoolean initialized = new SynchronizedBoolean(false);

    private XMPPConnection xmppConnection = null;

    public XmppMessageDispatcher(AbstractConnector connector) {
        super(connector);
        this.connector = (XmppConnector)connector;
    }

    protected synchronized void initialize(UMOEndpointURI uri) throws InitialisationException {
		logger.debug("initialise()");

		if (!initialized.get())
		{
			try
			{
				xmppConnection = connector.findOrCreateXmppConnection(uri);

				if (!xmppConnection.isConnected())
				{
                    throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X_WITH_X, "XMPP Connection", uri), this);

				}
                initialized.set(true);
			}
			catch (XMPPException e)
			{
                throw new InitialisationException(e, this);
			}

		}
	}

	public void doDispose()
	{
		logger.debug("doDipose()");
		if (null != xmppConnection)
		{
			xmppConnection.close();
		}
		initialized.set(false);
	}

	public void doDispatch(UMOEvent event) throws Exception
	{
		logger.info("in doDispatch()");
		initialize(event.getEndpoint().getEndpointURI());
		try
		{
			Message message = (Message)event.getTransformedMessage();
			logger.debug("Transformed message: " + message.toXML());
			while (!xmppConnection.isConnected() && !initialized.get())
			{
				initialize(null);
				Thread.sleep(150);
			}
			try
			{
				xmppConnection.createChat(message.getTo()).sendMessage(message);
				logger.debug("message successfully sent");
			}
			catch (IllegalStateException ex)
			{
				connector.exceptionThrown(ex);
			}
		}
		catch (ClassCastException ex)
		{
			connector.handleException(ex);
		}
	}

	public UMOMessage doSend(UMOEvent event)
		throws Exception
	{
		logger.debug("doSend()");
		doDispatch(event);
		return null;
	}

	public UMOMessage receive(UMOEndpointURI endpointUri, long timeout)
		throws Exception
	{
		logger.debug("receive()");

		// TODO: This.
		throw new UnsupportedOperationException("xmpp receive not implemented yet");
	}

	public Object getDelegateSession()
	{
		return null;
	}
}
