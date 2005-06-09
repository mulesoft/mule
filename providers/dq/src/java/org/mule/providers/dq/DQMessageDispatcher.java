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

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;

/**
 * @author m999svm <p/> DQMessageDispatcher
 */
public class DQMessageDispatcher extends AbstractMessageDispatcher
{
    private DQConnector connector;

    /**
     * Constructor
     * 
     * @param connector The connector
     */
    public DQMessageDispatcher(DQConnector connector)
    {
        super(connector);
        this.connector = connector;
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        try {
            DQMessage msg = (DQMessage) event.getMessage().getPayload();
            AS400 system = connector.getSystem();

            RecordFormat format = getRecordFormat(event.getEndpoint().getEndpointURI());
            Record rec = DQMessageUtils.getRecord(msg, format);

            DataQueue dq = new DataQueue(system, event.getEndpoint().getEndpointURI().getAddress());
            dq.write(rec.getContents());

        } catch (Exception e) {
            getConnector().handleException(e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("doDispatch(UMOEvent) - end");
        }
    }

    protected RecordFormat getRecordFormat(UMOEndpointURI endpointUri) throws Exception
    {
        String recordDescriptor = (String) endpointUri.getParams().get(DQConnector.RECORD_DESCRIPTOR_PROPERTY);
        if (recordDescriptor == null) {
            if (connector.getFormat() == null) {
                throw new IllegalArgumentException("Property " + DQConnector.RECORD_DESCRIPTOR_PROPERTY
                        + " must be set on the endpoint");
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Defaulting to connector format: " + connector.getRecordFormat());
                }
                return connector.getFormat();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Using endpoint-specific format: " + connector.getRecordFormat());
        }
        return DQMessageUtils.getRecordFormat(recordDescriptor, connector.getSystem());
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return null;
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        DataQueue dq = new DataQueue(connector.getSystem(), endpointUri.getAddress());
        if (dq != null) {
            DataQueueEntry entry = dq.read((int) timeout);
            if (entry != null) {
                RecordFormat format = getRecordFormat(endpointUri);
                DQMessage message = DQMessageUtils.getDQMessage(entry.getData(), format);
                message.setSenderInformation(entry.getSenderInformation());
                return new MuleMessage(connector.getMessageAdapter(message));
            }
        }
        return null;
    }

    public void doDispose()
    {
    }
}
