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
package org.mule.providers.dq;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.RecordFormat;

/**
 * @author m999svm <p/> DQMessageReceiver
 */
public class DQMessageReceiver extends PollingMessageReceiver
{

    private DataQueue dataQueue = null;
    private RecordFormat format = null;

    public DQMessageReceiver(UMOConnector connector,
                             UMOComponent component,
                             UMOEndpoint endpoint,
                             Long frequency,
                             DataQueue pDq,
                             AS400 pAs400) throws InitialisationException
    {
        super(connector, component, endpoint, frequency);

        this.dataQueue = pDq;

        String recordDescriptor = (String) endpoint.getEndpointURI()
                                                   .getParams()
                                                   .get(DQConnector.RECORD_DESCRIPTOR_PROPERTY);
        if (recordDescriptor == null) {
            format = ((DQConnector) connector).getFormat();
            if (format == null) {
                throw new InitialisationException(new Message("dq", 1), this);
            }
        } else {
            try {
                format = DQMessageUtils.getRecordFormat(recordDescriptor, pAs400);
            } catch (Exception e) {
                throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "recordDescriptor: "
                        + recordDescriptor), e, this);
            }
        }
    }

    /**
     * @see org.mule.providers.PollingMessageReceiver#poll()
     */
    public final void poll()
    {
        try {
            DataQueueEntry entry = dataQueue.read();

            if (entry == null) {
                return;
            }

            processEntry(entry);

        } catch (Exception e) {
            handleException(e);
        }

    }

    /**
     * Process a received message entry
     * 
     * @param entry The entry
     * @throws Exception Error during process
     */
    private void processEntry(final DataQueueEntry entry) throws Exception
    {
        DQMessage message = DQMessageUtils.getDQMessage(entry.getData(), format);
        message.setSenderInformation(entry.getSenderInformation());

        UMOMessage umoMessage = new MuleMessage(connector.getMessageAdapter(message));
        routeMessage(umoMessage);
    }

    public void doConnect() throws Exception
    {
        // template method
    }

    public void doDisconnect() throws Exception
    {
        // template method
    }

}
