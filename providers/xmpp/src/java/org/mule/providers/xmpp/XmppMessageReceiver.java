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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import java.net.URI;

/**
 * <code>XmppMessageReceiver</code> TODO
 *
 * @author Peter Braswell
 * @author John Evans
 * @version $Revision$
 */
public class XmppMessageReceiver extends AbstractMessageReceiver
	implements PacketListener
{
	private XMPPConnection xmppConnection = null;

	public XmppMessageReceiver(AbstractConnector connector,
		UMOComponent component, UMOEndpoint endpoint)
		throws InitialisationException
	{
		create(connector, component, endpoint);
		connect(endpoint.getEndpointURI().getUri());
	}

	protected void connect(URI uri) throws InitialisationException
	{
		try
		{
			XmppConnector cnn = (XmppConnector)connector;
			xmppConnection = cnn.findOrCreateXmppConnection(endpoint.getEndpointURI());
            if(endpoint.getFilter() != null) {
                xmppConnection.addPacketListener(this, (PacketFilter)endpoint.getFilter());
            } else {
                PacketFilter filter = new PacketTypeFilter(Message.class);
                xmppConnection.addPacketListener(this, filter);
            }
		}
		catch (XMPPException e)
		{
			throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X, "XMPP Connection"), e, this);
		}
	}

    protected boolean allowFilter(UMOFilter filter) throws UnsupportedOperationException {
        return filter instanceof PacketFilter;
    }

	public void doDispose()
	{
		logger.info("Closed Xmpp Listener");
	}

	protected Work createWork(Message message)
	{
		return new XMPPWorker(message);
	}

	/**
	 * @see org.jivesoftware.smack.PacketListener#processPacket(org.jivesoftware.smack.packet.Packet)
	 */
	public void processPacket(Packet packet)
	{
		logger.debug("processing packet: " + packet.toXML());
		Message msg = (Message)packet;
		Work work = createWork(msg);
		try
		{
            getWorkManager().scheduleWork(work, WorkManager.IMMEDIATE, null, null);
		}
		catch (WorkException e)
		{
			logger.error("Xmpp Server receiver work failed: " + e.getMessage(),	e);
		}
	}

	private class XMPPWorker implements Work
	{
		Message message = null;

		public XMPPWorker(Message message)
		{
			this.message = message;
		}

		/**
		 * Accept requests from a given TCP port
		 */
		public void run()
		{
			try
			{
				logger.info("processing xmpp message from: "
					+ message.getFrom());
				UMOMessageAdapter adapter = connector
					.getMessageAdapter(message);
				logger.info("UMOMessageAdapter is a: "
					+ adapter.getClass().getName());
				UMOMessage returnMessage = routeMessage(
					new MuleMessage(adapter), endpoint.isSynchronous());

				if (returnMessage != null)
				{
					xmppConnection.sendPacket(new Message(returnMessage
						.getPayloadAsString()));
				}
			}
			catch (Exception e)
			{
				handleException(e);
			}
		}

        public void release() {
        }
	}
}