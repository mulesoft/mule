/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.space;

import org.apache.commons.lang.StringUtils;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;
import org.mule.umo.space.UMOSpaceFactory;
import org.mule.util.BeanUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides generic connectivity to 'Spaces' that implement the Mule Space API, i.e.
 * Gigaspaces, JCache implementations, Rio can be accessed as well as a mule file,
 * Journal or VM space.
 */
public class SpaceConnector extends AbstractServiceEnabledConnector
{
    private UMOSpaceFactory spaceFactory;
    private Map spaceProperties;
    // TODO Mule 2.0: these are stored on the UMOManagementContext
    private Map spaces = new HashMap();

    public SpaceConnector()
    {
        super();
    }

    public String getProtocol()
    {
        return "space";
    }

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if (spaceFactory == null)
        {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "spaceFactory"), this);
        }
        if (spaceProperties != null)
        {
            BeanUtils.populateWithoutFail(spaceFactory, spaceProperties, true);
        }

    }

    public UMOSpace getSpace(String spaceUrl) throws UMOSpaceException
    {
        logger.info("looking for space: " + spaceUrl);
        UMOSpace space = (UMOSpace)spaces.get(spaceUrl);
        if (space == null)
        {
            logger.info("Space not found, creating space: " + spaceUrl);
            space = spaceFactory.create(spaceUrl);
            spaces.put(spaceUrl, space);
        }
        return space;
    }

    /**
     * Will look up a space based on the URI. If the Space is created this method
     * will honour the transaction information on the endpoint and set the space up
     * accordingly
     * 
     * @param endpoint
     * @return
     * @throws UMOSpaceException
     */
    public UMOSpace getSpace(UMOImmutableEndpoint endpoint) throws UMOSpaceException
    {
        String spaceKey = getSpaceKey(endpoint);
        logger.info("looking for space: " + spaceKey);
        UMOSpace space = (UMOSpace)spaces.get(spaceKey);
        if (space == null)
        {
            logger.info("Space not found, creating space: " + spaceKey);
            space = spaceFactory.create(endpoint);
            spaces.put(spaceKey, space);
            if (endpoint.getTransactionConfig().getFactory() != null)
            {
                space.setTransactionFactory(endpoint.getTransactionConfig().getFactory());
            }
        }
        return space;
    }

    protected String getSpaceKey(UMOImmutableEndpoint endpoint)
    {
        return endpoint.getEndpointURI().toString();
    }

    public UMOSpaceFactory getSpaceFactory()
    {
        return spaceFactory;
    }

    public void setSpaceFactory(UMOSpaceFactory spaceFactory)
    {
        this.spaceFactory = spaceFactory;
    }

    public Map getSpaceProperties()
    {
        return spaceProperties;
    }

    public void setSpaceProperties(Map spaceProperties)
    {
        this.spaceProperties = spaceProperties;
    }

    /**
     * Template method to perform any work when destroying the connectoe
     */
    protected void doDispose()
    {
        for (Iterator iterator = spaces.values().iterator(); iterator.hasNext();)
        {
            UMOSpace space = (UMOSpace)iterator.next();
            space.dispose();
        }
        spaces.clear();
    }

    /**
     * The method determines the key used to store the receiver against.
     * 
     * @param component the component for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the component
     * @return the key to store the newly created receiver against
     */
    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        return super.getReceiverKey(component, endpoint)
               + (endpoint.getFilter() != null ? '#' + endpoint.getFilter().toString() : StringUtils.EMPTY);
    }
}
