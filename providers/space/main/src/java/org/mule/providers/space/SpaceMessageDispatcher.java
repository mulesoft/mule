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
package org.mule.providers.space;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;


/**
 * <code>SpaceMessageDispatcher</code> Provides generic connectivity to 'Spaces' that implement
 * the Mule Space Api, i.e. Gigaspaces, JCache imples, Rio can be accessed as well as a mule file,
 * Journal or VM space.
 *
 * The dispatcher allows Mule to dispatch events synchronously and asynchronusly to a space as well as
 * make receive calls to the space.
 *
 * @version $Revision$
 */

public class SpaceMessageDispatcher extends AbstractMessageDispatcher {


    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private UMOSpace space;
    private SpaceConnector connector;

    public SpaceMessageDispatcher(SpaceConnector connector) {
        super(connector);
        this.connector = connector;
    }

    protected UMOSpace getSpace(UMOEndpoint endpoint) throws  UMOSpaceException {

        if (space == null) {
            space = connector.getSpace(endpoint);
        }
        return space;

    }

    public void doDispatch(UMOEvent event) throws Exception {
        UMOSpace space = getSpace(event.getEndpoint());
        space.put(event.getTransformedMessage(), event.getTimeout());
    }

    public UMOMessage doSend(UMOEvent event) throws Exception {
        doDispatch(event);
        return null;
    }


    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        String destination = endpointUri.toString();

        if(logger.isInfoEnabled()) logger.info("Connecting to space '" + destination + "'");
        UMOSpace space = connector.getSpace(destination);

        Object result = space.take(timeout);
        if (result == null) {
            return null;
        }
        UMOMessage message = new MuleMessage(connector.getMessageAdapter(result));
        return message;
    }

    public Object getDelegateSession() throws UMOException {
        return null;
    }

    public void doDispose() {
        if(space!=null) space.dispose();
    }


}
