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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;
import org.mule.umo.space.UMOSpaceFactory;
import org.mule.util.BeanUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides generic connectivity to 'Spaces' that implment the Mule Space Api, i.e.
 * Gigaspaces, JCache implementations, Rio can be accessed as well as a mule file, Journal or VM space.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SpaceConnector extends AbstractServiceEnabledConnector {

    private UMOSpaceFactory spaceFactory;
    private Map spaceProperties;
    //Todo Mule 2.0 these are stored on the UMOManagementContext
    private Map spaces = new HashMap();

    public SpaceConnector() {

    }

    public String getProtocol() {
        return "space";
    }

    public void doInitialise() throws InitialisationException {
        super.doInitialise();
        if(spaceFactory==null) {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "spaceFactory"), this);
        }
        if(spaceProperties!=null) {
            BeanUtils.populateWithoutFail(spaceFactory, spaceProperties, true);
        }

    }

    public UMOSpace getSpace(String spaceUrl) throws UMOSpaceException {
        logger.info("looking for space: " + spaceUrl);
        UMOSpace space = (UMOSpace)spaces.get(spaceUrl);
        if(space==null) {
            logger.info("Space not found, creating space: " + spaceUrl);
            space = spaceFactory.create(spaceUrl);
            spaces.put(spaceUrl, space);
        }
        return space;
    }

    /**
     * Will look up a space based on the URI.
     * If the Space is created this method will honour the transaction information on the endpoint and
     * set the space up accordingly
     * @param endpoint
     * @return
     * @throws UMOSpaceException
     */
    public UMOSpace getSpace(UMOEndpoint endpoint) throws UMOSpaceException {
        String spaceUrl = endpoint.getEndpointURI().toString();
        logger.info("looking for space: " + spaceUrl);
        UMOSpace space = (UMOSpace)spaces.get(spaceUrl);
        if(space==null) {
            logger.info("Space not found, creating space: " + spaceUrl);
            space = spaceFactory.create(endpoint);
            spaces.put(spaceUrl, space);
            if(endpoint.getTransactionConfig().getFactory()!=null) {
                space.setTransactionFactory(endpoint.getTransactionConfig().getFactory());
            }
        }
        return space;
    }

    public UMOSpaceFactory getSpaceFactory() {
        return spaceFactory;
    }

    public void setSpaceFactory(UMOSpaceFactory spaceFactory) {
        this.spaceFactory = spaceFactory;
    }

    public Map getSpaceProperties() {
        return spaceProperties;
    }

    public void setSpaceProperties(Map spaceProperties) {
        this.spaceProperties = spaceProperties;
    }

    /**
     * Template method to perform any work when destroying the connectoe
     */
    protected void doDispose() {
        for (Iterator iterator = spaces.values().iterator(); iterator.hasNext();) {
            UMOSpace space = (UMOSpace) iterator.next();
            space.dispose();
        }
        spaces.clear();
    }
}
