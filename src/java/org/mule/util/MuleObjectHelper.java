/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.util;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.service.ConnectorFactory;
import org.mule.providers.service.ConnectorFactoryException;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManager;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <code>MuleObjectHelper</code> is a helper class to assist in finding mule server
 * objects, such as endpoint and transformers
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MuleObjectHelper
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleObjectHelper.class);

    public static UMOTransformer getTransformer(String list, String delim) throws MuleException
    {
        StringTokenizer st = new StringTokenizer((String) list, delim);

        UMOTransformer tempTrans = null;
        UMOTransformer currentTrans = null;
        UMOTransformer returnTrans = null;
        UMOManager manager = MuleManager.getInstance();
        String key;
        while (st.hasMoreTokens())
        {
            key = st.nextToken().trim();
            tempTrans = manager.lookupTransformer(key);
            if (tempTrans == null)
            {
                throw new MuleException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER, "Transformer: " + key));
            }
            if (currentTrans == null)
            {
                currentTrans = tempTrans;
                returnTrans = tempTrans;
            } else
            {
                currentTrans.setTransformer(tempTrans);
                currentTrans = tempTrans;
            }

        }
        return returnTrans;
    }

    public static UMOEndpoint getEndpointByProtocol(String protocol)
    {
        UMOImmutableEndpoint iprovider;
        Map endpoints = MuleManager.getInstance().getEndpoints();
        for (Iterator iterator = endpoints.values().iterator(); iterator.hasNext();)
        {
            iprovider = (UMOImmutableEndpoint) iterator.next();
            if (iprovider.getProtocol().equals(protocol))
            {
                return new MuleEndpoint(iprovider);
            }
        }
        return null;
    }

    public static UMOEndpoint getEndpointByEndpointUri(String endpointUri, boolean wildcardMatch)
    {
        UMOFilter filter;
        if(wildcardMatch) {
            filter = new WildcardFilter(endpointUri);
        } else {
            filter = new EqualsFilter(endpointUri);
        }
        UMOImmutableEndpoint iprovider;
        Map endpoints = MuleManager.getInstance().getEndpoints();
        for (Iterator iterator = endpoints.values().iterator(); iterator.hasNext();)
        {
            iprovider = (UMOImmutableEndpoint) iterator.next();
            if (filter.accept(iprovider.getEndpointURI()))
            {
                return new MuleEndpoint(iprovider);
            }
        }
        return null;
    }

//    public static UMOEndpoint getOrCreateProviderForUrl(UMOImmutableDescriptor descriptor, UMOEndpointURI url, boolean reciever) throws UMOException
//    {
//        UMOEndpoint endpoint = null;
//        if(url.getEndpointName()!=null) {
//            if(reciever) {
//                endpoint = descriptor.getInboundEndpoint();
//                if(provider!=null && ! url.getEndpointName().equals(provider.getName())) {
//                    endpoint = descriptor.getInboundRouter().getEndpointURI(url.getEndpointName());
//                }
//            } else {
//                endpoint = descriptor.getOutboundEndpoint();
//            }
//        }
//        if(provider==null) {
//            return getOrCreateProviderForUrl(url, reciever);
//        } else {
//            return endpoint;
//        }
//    }

//    public static UMOEndpoint getOrCreateProviderForUrl(UMOEndpointURI url, boolean receiver) throws UMOException
//    {
//        UMOEndpoint endpoint = null;
//        if(url.getEndpointName()!=null) {
//            endpoint = MuleManager.getInstance().lookupEndpoint(url.getEndpointName());
//            if(provider!=null && url.getEndpointURI()!=null && url.getEndpointURI().length() > 0) {
//                endpoint.setEndpointURI(url);
//            }
//        }
//        //as the endpoint name is not null, but there is no global
//        //provider configured we will create a new on
//        if(provider==null) {
//            endpoint = ConnectorFactory.createProvider(url, receiver);
//            if(url.getEndpointName()!=null) endpoint.setName(url.getEndpointName());
//        }
//        return endpoint;
//    }

//    public static UMOEndpoint createProviderForUrl(MuleEndpointURI url, String type, int createConnector) throws UMOException{
//        UMOConnector connector = getConnectorByProtocol(url.getScheme());
//        if(connector==null) {
//            throw new MuleException("There is no connector registered that supports protocol:" + url.getScheme());
//        }
//        UMOEndpoint endpoint = new MuleEndpoint();
//        endpoint.setName(url.getEndpointName());
//        endpoint.setConnector(connector);
//        endpoint.setEndpointURI(url.getEndpointURI());
//
//        if(type!=null) {
//            endpoint.setType(type);
//        }
//        return endpoint;
//    }

    public static UMOConnector getConnectorByProtocol(String protocol) {
        UMOConnector connector;
        Map connectors = MuleManager.getInstance().getConnectors();
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            connector = (UMOConnector) iterator.next();
            if (connector.getProtocol().equalsIgnoreCase(protocol))
            {
                return connector;
            }
        }
        return null;
    }

    public static UMOConnector getOrCreateConnectorByProtocol(UMOEndpointURI url) throws ConnectorFactoryException
    {
        UMOConnector connector = getConnectorByProtocol(url.getSchemeMetaInfo());
        if(connector==null) {
            connector = ConnectorFactory.createConnector(url);
            try
            {
                BeanUtils.populate(connector, url.getParams());
                MuleManager.getInstance().registerConnector(connector);

            } catch (Exception e)
            {
                throw new ConnectorFactoryException(new Message(Messages.FAILED_TO_SET_PROPERTIES_ON_X, "Connector"), e);
            }
        }
        return connector;
    }

//    public static UMOEndpoint getEndpointName(String endpointName, UMODescriptor descriptor) throws EndpointNotFoundException
//    {
//        UMOEndpoint endpoint = null;
//        if(descriptor!=null) {
//            endpoint = descriptor.getOutboundEndpoint();
//            if(provider!=null && !providerName.equals(provider.getName())) {
//                endpoint = null;
//            }else if(provider==null) {
//                endpoint = MuleManager.getInstance().lookupEndpoint(providerName);
//            }
//        } else {
//            endpoint = MuleManager.getInstance().lookupEndpoint(providerName);
//        }
//        if(provider == null) {
//            throw new EndpointNotFoundException("failed to find an outbound endpoint called: " + endpointName);
//        }
//        return endpoint;
//    }
}
