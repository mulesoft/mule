/*
 * $Header: /cvsroot/mule/mule/src/java/org/mule/umo/impl/EndpointList.java,v
 * 1.10 2003/11/21 05:49:12 rossmason Exp $ $Revision$ $Date: 2003/11/21
 * 05:49:12 $
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */
package org.mule.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOFilter;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.*;

/**
 * <code>EndpointList</code> encapsulates two lists of ProviderDescriptors;
 * send endpoints and receive endpoints. It also provides helper methods to
 * query manipulate and maintain state of the lists.
 *
 * @deprecated
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EndpointList
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(EndpointList.class);

    /**
     * A map of send endpoints
     */
    private Map sendProviders = new HashMap();

    /**
     * A map of receive endpoints
     */
    private Map receiveProviders = new HashMap();

    /**
     * The name of the default send endpoint
     */
    private String defaultSend = null;

    /**
     * The name of the default receive endpoint
     */
    private String defaultReceive = null;

    /**
     * Default constructor
     */
    public EndpointList()
    {
    }

    /**
     * Returns an Iterator of receive endpoint names in the endpoint list
     *
     * @return iterator of receive endpoint names
     */
    public Iterator getReceiverNames()
    {
        return receiveProviders.keySet().iterator();
    }

    /**
     * Returns an Iterator of send endpoint names in the endpoint list
     *
     * @return iterator of send endpoint names
     */
    public Iterator getSenderNames()
    {
        return sendProviders.keySet().iterator();
    }

    /**
     * Retrieves a receive endpoint with the given name
     *
     * @param name Name of the endpoint to return
     * @return the endpoint or null if an endpoint wasn't found
     */
    public UMOEndpoint getReceiveProvider(String name)
    {
        UMOImmutableEndpoint endpoint = (UMOImmutableEndpoint) receiveProviders.get(name);
        if (endpoint == null)
        {
            return null;
        }
        else
        {
            return new MuleEndpoint(endpoint);
        }
    }

    /**
     * Retrieves a send endpoint with the given name
     *
     * @param name Name of the endpoint to return
     * @return the endpoint or null if an endpoint wasn't found
     */
    public UMOEndpoint getSendProvider(String name)
    {
        UMOImmutableEndpoint endpoint = (UMOImmutableEndpoint) sendProviders.get(name);
        if (endpoint == null)
        {
            return null;
        }
        else
        {
            return new MuleEndpoint(endpoint);
        }
    }

    /**
     * Adds a send endpoint to the list
     *
     * @param endpoint the endpoint to add
     * @param readOnly whether the endpoint should be read-only
     */
    public void addSendProvider(UMOImmutableEndpoint endpoint, boolean readOnly)
    {
        if (sendProviders.isEmpty())
        {
            defaultSend = endpoint.getName();
            if(logger.isDebugEnabled())
                logger.debug("Default Send endpoint has been set to: " + defaultSend);
        }

        //provider = validateType(provider, true);
        endpoint = configure(endpoint, readOnly, true);

        sendProviders.put(endpoint.getName(), endpoint);
        logger.debug("Added Send endpoint: " + endpoint.getName());
    }

    public void addProviderList(EndpointList list)
    {
        UMOImmutableEndpoint endpoint;
        for (Iterator iterator = list.getAll().values().iterator(); iterator.hasNext();)
        {
            endpoint = (UMOImmutableEndpoint) iterator.next();
            add(endpoint);
        }
    }
    /**
     * Adds a receive endpoint to the list
     *
     * @param endpoint the endpoint to add
     * @param readOnly whether the endpoint should be read-only
     */
    protected void addReceiveProvider(UMOImmutableEndpoint endpoint, boolean readOnly)
    {

        if (receiveProviders.isEmpty())
        {
            defaultReceive = endpoint.getName();

            //provider = validateType(provider, false);
            endpoint = configure(endpoint, readOnly, false);
        }
        receiveProviders.put(endpoint.getName(), endpoint);
        logger.debug("Added Receive endpoint: " + endpoint.getName());
    }

    /**
     * Sets the specified endpoint to be the default in the list
     *
     * @param endpoint the default endpoint
     * @throws MuleException if the endpoint is null or is not of the correct type i.e. a
     *                       receiver not a sender
     */
    public void setDefaultSendProvider(UMOImmutableEndpoint endpoint) throws MuleException
    {
        setDefaultSendProvider(endpoint, false);
    }

    /**
     * Sets the specified endpoint to be the default in the list
     *
     * @param endpoint the default endpoint
     * @param readOnly Whether the endpoint should be made read-only
     * @throws MuleException if the endpoint is null or is not of the correct type i.e. a
     *                       receiver not a sender
     */
    public void setDefaultSendProvider(UMOImmutableEndpoint endpoint, boolean readOnly) throws MuleException
    {
        if (endpoint == null)
        {
            throw new MuleException(new Message(Messages.X_IS_NULL, "Endpoint"));
        }

        //provider = validateType(provider, true);

//        if (!provider.getType().equals(UMOImmutableEndpoint.PROVIDER_TYPE_RECEIVER)
//                || endpoint.getTransformer()==null)
//        {
        if(logger.isDebugEnabled())
            logger.debug("Setting the Default Send endpoint to: " + endpoint.getName());

        UMOImmutableEndpoint p = (UMOImmutableEndpoint) sendProviders.get(endpoint.getName());

        if (p != null)
        {
            endpoint = p;
            sendProviders.remove(endpoint.getName());
        }
        endpoint = configure(endpoint, readOnly, true);
        addSendProvider(endpoint, readOnly);

        defaultSend = endpoint.getName();
    }

    /**
     * Sets the specified endpoint to be the default in the list
     *
     * @param endpoint the default endpoint
     * @throws MuleException if the endpoint is null or is not of the correct type i.e. a
     *                       sender not a receiver
     */
    public void setDefaultReceiveProvider(UMOImmutableEndpoint endpoint) throws MuleException
    {
        setDefaultReceiveProvider(endpoint, false);
    }

    /**
     * Sets the specified endpoint to be the default in the list
     *
     * @param endpoint the default endpoint
     * @param readOnly Whether the endpoint should be made read-only
     * @throws MuleException if the endpoint is null or is not of the correct type i.e. a
     *                       sender not a receiver
     */
    public void setDefaultReceiveProvider(UMOImmutableEndpoint endpoint, boolean readOnly)
            throws MuleException
    {
        if (endpoint == null)
        {
            throw new MuleException(new Message(Messages.X_IS_NULL, "Endpoint"));            
        }
        //provider = validateType(provider, false);
        //FIX
        //if (!provider.getType().equals(UMOImmutableEndpoint.PROVIDER_TYPE_SENDER)
        //|| endpoint.getTransformer()==null)
        //{
        logger.info("Setting the Default Send endpoint to: " + endpoint.getName());

        UMOImmutableEndpoint p = (MuleEndpoint) receiveProviders.get(endpoint.getName());

        if (p != null)
        {
            endpoint = p;
            receiveProviders.remove(endpoint.getName());
        }
        endpoint = configure(endpoint, readOnly, false);
        addReceiveProvider(endpoint, readOnly);
        defaultReceive = endpoint.getName();
    }

    /**
     * Returns the default Receive Provider in the list. If no default has been
     * specified, the first receive endpoint added will be used
     *
     * @return the the default receive endpoint
     */
    public UMOImmutableEndpoint getDefaultReceiveProvider()
    {
        return (UMOImmutableEndpoint) receiveProviders.get(defaultReceive);
    }

    /**
     * Returns the default Send Provider in the list. If no default has been
     * specified, the first send endpoint added will be used
     *
     * @return the the default send endpoint
     */
    public UMOImmutableEndpoint getDefaultSendProvider()
    {
        return (UMOImmutableEndpoint) sendProviders.get(defaultSend);
    }

    /**
     * Returns the total number of send endpoints
     *
     * @return the total number of send endpoints
     */
    public int getSendProvidersSize()
    {
        return sendProviders.size();
    }

    /**
     * Returns the total number of receive endpoints
     *
     * @return the total number of receive endpoints
     */
    public int getReceiveProvidersSize()
    {
        return receiveProviders.size();
    }

    /**
     * Add an endpoint to the list
     *
     * @param endpoint the proivder to add
     */
    public void add(UMOImmutableEndpoint endpoint)
    {
        add(endpoint, false);
    }

    /**
     * Add an endpoint to the list
     *
     * @param endpoint the proivder to add
     * @param readOnly Whether the proivder should be made read-only
     */
    public void add(UMOImmutableEndpoint endpoint, boolean readOnly)
    {
        if (endpoint == null)
            return;

        if (endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER))
        {
            endpoint = configure(endpoint, readOnly, false);
            addReceiveProvider(endpoint, readOnly);
        }
        else if (endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER) ||
                endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER))
        {
            endpoint = configure(endpoint, readOnly, true);
            addSendProvider(endpoint, readOnly);
        }
//        else if (provider.getType().equals(UMOImmutableEndpoint.PROVIDER_TYPE_SENDER_AND_RECEIVER))
//        {
//            endpoint = configure(provider, readOnly, false);
//            addReceiveProvider(provider, readOnly);
//            endpoint = configure(provider, readOnly, true);
//            addSendProvider(provider, readOnly);
//        }
        else
        {
            throw new IllegalArgumentException("Provider type not recognised: " + endpoint.getType());
        }
    }

    /**
     * Returns an endpoint with the given name
     *
     * @param name the name of the proivder to return
     * @return the proivder or null if an endpoint with the given name doesn't
     *         exist
     */
    public UMOEndpoint get(String name)
    {
        if (name == null)
            return null;
        UMOEndpoint endpoint = getSendProvider(name);

        if (endpoint == null)
        {
            endpoint = getReceiveProvider(name);
        }
        return endpoint;
    }

    /**
     * Removes an endpoint from the list
     *
     * @param endpoint the proivder to remove
     */
    public void remove(UMOImmutableEndpoint endpoint)
    {
        if (endpoint == null)
            return;

        if (endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER))
        {
            logger.debug("Removing Receive endpoint: " + endpoint.getName());
            receiveProviders.remove(endpoint.getName());
        }
        else if (endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER))
        {
            logger.debug("Removing Send endpoint: " + endpoint.getName());
            sendProviders.remove(endpoint.getName());
        }
        else if (endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER))
        {
            logger.debug("Removing Send and Receive endpoint: " + endpoint.getName());
            receiveProviders.remove(endpoint.getName());
            sendProviders.remove(endpoint.getName());
        }
        else
        {
            logger.warn("Provider type is notrecognised, type is: " + endpoint.getType());
        }
    }

    /**
     * Gets the total number of all endpoints in the list
     *
     * @return the total number of all endpoints in the list
     */
    public int getTotalSize()
    {
        return getSendProvidersSize() + getReceiveProvidersSize();
    }

    /**
     * Returns a list of receive endpoints that use the given protocol
     *
     * @param protocol the protocol name to select the endpoints with
     * @return a list of endpoints. If there are no endpoints registered with
     *         the specified protocol an empty list is returned.
     */
    public UMOImmutableEndpoint[] getReceiveProvidersByProtocol(String protocol)
    {
        return getProvidersByProtocol(receiveProviders, protocol);
    }

    /**
     * Returns a list of send endpoints that use the given protocol
     *
     * @param protocol the protocol name to select the endpoints with
     * @return a list of endpoints. If there are no endpoints registered with
     *         the specified protocol an empty list is returned.
     */
    public MuleEndpoint[] getSendProvidersByProtocol(String protocol)
    {
        return getProvidersByProtocol(sendProviders, protocol);
    }

    /**
     * Returns a list of all endpoints that use the given protocol
     *
     * @param protocol the protocol name to select the endpoints with
     * @return a list of endpoints. If there are no endpoints registered with
     *         the specified protocol an empty list is returned.
     */
    private MuleEndpoint[] getProvidersByProtocol(Map endpoints, String protocol)
    {
        Map.Entry entry = null;
        ArrayList list = new ArrayList();

        for (Iterator i = endpoints.entrySet().iterator(); i.hasNext();)
        {
            entry = (Map.Entry) i.next();
            if (((UMOEndpoint) entry.getValue()).getProtocol().equalsIgnoreCase(protocol))
            {
                list.add(entry.getValue());
            }
        }

        return (MuleEndpoint[]) list.toArray(new MuleEndpoint[list.size()]);
    }

    private MuleEndpoint[] getEndpointByUri(Map endpoints, String endpointUri, boolean wildcardMatch)
    {
        UMOFilter filter;
        if(wildcardMatch) {
            filter = new WildcardFilter(endpointUri);
        } else {
            filter = new EqualsFilter(endpointUri);
        }
        Map.Entry entry = null;
        ArrayList list = new ArrayList();
        UMOEndpoint endpoint;
        for (Iterator i = endpoints.entrySet().iterator(); i.hasNext();)
        {
            entry = (Map.Entry) i.next();
            endpoint = (UMOEndpoint) entry.getValue();

            if (filter.accept(endpoint.getEndpointURI()))
            {
                list.add(entry.getValue());
            }
        }

        return (MuleEndpoint[]) list.toArray(new MuleEndpoint[list.size()]);
    }

    /**
     * Returns a list of receive endpoints that use the given endpointUri
     *
     * @param endpoint the endpointUri to select the endpoints with
     * @param exactMatch determines if wildcard matching should be used
     * @return a list of endpoints. If there are no endpoints registered with
     *         the specified protocol an empty list is returned.
     */
    public UMOImmutableEndpoint[] getReceiveProvidersByEndpoint(String endpoint, boolean exactMatch)
    {
        return getEndpointByUri(receiveProviders, endpoint, exactMatch);
    }

    /**
     * Returns a list of send endpoints that use the given protocol
     *
     * @param endpoint the endpointUri to select the endpoints with
     * @param exactMatch determines if wildcard matching should be used
     * @return a list of endpoints. If there are no endpoints registered with
     *         the specified protocol an empty list is returned.
     */
    public MuleEndpoint[] getSendProvidersByEndpoint(String endpoint, boolean exactMatch)
    {
        return getEndpointByUri(sendProviders, endpoint, exactMatch);
    }

    private UMOImmutableEndpoint configure(UMOImmutableEndpoint endpoint,
                                                     boolean readOnly, boolean isSender)
    {
        MuleEndpoint configuredProvider;
        if (endpoint instanceof UMOImmutableEndpoint)
        {
            configuredProvider = new MuleEndpoint(endpoint);
        }
        else
        {
            configuredProvider = (MuleEndpoint) endpoint;
        }

        if (isSender)
        {
            configuredProvider.setType(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        }
        else
        {
            configuredProvider.setType(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);
        }

        if (readOnly)
        {
            return configuredProvider.getImmutableProvider();
        }
        else
        {
            return configuredProvider;
        }
    }

    public void clear()
    {
        receiveProviders.clear();
        sendProviders.clear();
    }

    /**
     * This methods returns an unmodifiable map of all the the endpoints registered in this list
     * @return
     */
    public Map getAll()
    {
        Map all = new HashMap(receiveProviders);
        all.putAll(sendProviders);
        return Collections.unmodifiableMap(all);
    }
}