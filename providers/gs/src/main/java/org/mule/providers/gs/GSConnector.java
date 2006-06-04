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
package org.mule.providers.gs;

import org.mule.providers.gs.space.GSSpaceFactory;
import org.mule.providers.space.SpaceConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;

/**
 * Provides a Space connector to be used with the GigaSpaces JavaSpaces implementation
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GSConnector extends SpaceConnector {

    private long transactionTimeout = 32 * 1000;

    public GSConnector() {
        registerSupportedProtocol("rmi");
        registerSupportedProtocol("java");
        registerSupportedProtocol("jini");
        setSpaceFactory(new GSSpaceFactory());
    }


    public String getProtocol() {
        return "gs";
    }

    public long getTransactionTimeout() {
        return transactionTimeout;
    }

    public void setTransactionTimeout(long transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param component the component for which the endpoint is being registered
     * @param endpoint  the endpoint being registered for the component
     * @return the key to store the newly created receiver against
     */
    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint) {
        return endpoint.getEndpointURI().toString() + (endpoint.getFilter()!=null ? ":" + endpoint.getFilter() : "");
    }

    /////////  Do not cahce spaces /////////
    /**
     * Will look up a space based on the URI.
     * If the Space is created this method will honour the transaction information on the endpoint and
     * set the space up accordingly
     *
     * @param endpoint
     * @return
     * @throws org.mule.umo.space.UMOSpaceException
     *
     */
    public UMOSpace getSpace(UMOEndpoint endpoint) throws UMOSpaceException {
        return getSpaceFactory().create(endpoint);
    }

    public UMOSpace getSpace(String spaceUrl) throws UMOSpaceException {
        return getSpaceFactory().create(spaceUrl);
    }

    protected String getSpaceKey(UMOImmutableEndpoint endpoint) {
        String spaceKey = super.getSpaceKey(endpoint);
        spaceKey += (endpoint.getFilter()!=null ? '#' + endpoint.getFilter().toString() : "");
        return spaceKey;
    }

}
