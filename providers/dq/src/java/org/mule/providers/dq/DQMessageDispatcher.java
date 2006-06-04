/*
 * $Id$
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

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>DQMessageDispatcher</code> //todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DQMessageDispatcher extends AbstractMessageDispatcher
{
    private DQConnector connector;

    /**
     * Constructor
     * 
     * @param endpoint
     *            The endpoint for this adapter
     */
    public DQMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (DQConnector)endpoint.getConnector();
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        try {
            DQMessage msg = (DQMessage)event.getMessage().getPayload();
            AS400 system = connector.getSystem();

            RecordFormat format = getRecordFormat(event.getEndpoint().getEndpointURI());
            Record rec = DQMessageUtils.getRecord(msg, format);

            DataQueue dq = new DataQueue(system, event.getEndpoint().getEndpointURI().getAddress());
            dq.write(rec.getContents());

        }
        catch (Exception e) {
            getConnector().handleException(e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("doDispatch(UMOEvent) - end");
        }
    }

    protected RecordFormat getRecordFormat(UMOEndpointURI endpointUri) throws Exception
    {
        String recordDescriptor = (String)endpointUri.getParams().get(
                DQConnector.RECORD_DESCRIPTOR_PROPERTY);
        if (recordDescriptor == null) {
            if (connector.getFormat() == null) {
                throw new IllegalArgumentException("Property " + DQConnector.RECORD_DESCRIPTOR_PROPERTY
                        + " must be set on the endpoint");
            }
            else {
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

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return null;
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint
     *            the endpoint to use when connecting to the resource
     * @param timeout
     *            the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available.
     *            If no data becomes available before the timeout elapses, null
     *            will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null
     *         will be returned if no data was avaialable
     * @throws Exception
     *             if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {

        DataQueue dq = new DataQueue(connector.getSystem(), endpoint.getEndpointURI().getAddress());
        DataQueueEntry entry = dq.read((int)timeout);
        if (entry != null) {
            RecordFormat format = getRecordFormat(endpoint.getEndpointURI());
            DQMessage message = DQMessageUtils.getDQMessage(entry.getData(), format);
            message.setSenderInformation(entry.getSenderInformation());
            return new MuleMessage(connector.getMessageAdapter(message));
        }
        return null;
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
