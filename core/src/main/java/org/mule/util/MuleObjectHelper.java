/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.MuleException;
import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.ObjectFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * <code>MuleObjectHelper</code> is a helper class to assist in finding mule server
 * objects, such as endpoint and transformers
 * @deprecated these methods are bad and need to be removed
 */
// @ThreadSafe
public final class MuleObjectHelper
{

    /** Do not instanciate. */
    private MuleObjectHelper ()
    {
        // no-op
    }

    /**
     * Builds a list of UMOTransformers.
     * 
     * @param names - a list of transformers separated by "delim"
     * @param delim - the character used to delimit the transformers in the list
     * @return a list (possibly empty) of transformers or
     * {@link org.mule.transformers.TransformerUtils#UNDEFINED} if the names list is null
     * @throws MuleException
     */
    public static List getTransformers(String names, String delim) throws MuleException
    {
        if (null != names)
        {
            List transformers = new LinkedList();
            StringTokenizer st = new StringTokenizer(names, delim);
            while (st.hasMoreTokens())
            {
                String key = st.nextToken().trim();
                UMOTransformer transformer = RegistryContext.getRegistry().lookupTransformer(key);

                if (transformer == null)
                {
                    throw new MuleException(CoreMessages.objectNotRegistered("Transformer", key));
                }
                transformers.add(transformer);
            }
            return transformers;
        }
        else
        {
            return TransformerUtils.UNDEFINED;
        }
    }

    public static UMOImmutableEndpoint getEndpointByProtocol(String protocol) throws UMOException
    {
        UMOImmutableEndpoint iprovider;
        Collection endpoints = RegistryContext.getRegistry().getEndpoints();
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            iprovider = (UMOImmutableEndpoint) iterator.next();
            if (iprovider.getProtocol().equals(protocol))
            {
                UMOManagementContext managementContext = MuleServer.getManagementContext();
                UMOEndpointBuilder builder = new EndpointURIEndpointBuilder(iprovider, managementContext);
                return managementContext.getRegistry().lookupEndpointFactory().createInboundEndpoint(builder,
                    managementContext);
            }
        }
        return null;
    }

    public static UMOImmutableEndpoint getEndpointByEndpointUri(String endpointUri, boolean wildcardMatch) throws UMOException
    {
        ObjectFilter filter;

        if (wildcardMatch)
        {
            filter = new WildcardFilter(endpointUri);
        }
        else
        {
            filter = new EqualsFilter(endpointUri);
        }

        UMOImmutableEndpoint iprovider;
        Collection endpoints = RegistryContext.getRegistry().getEndpoints();

        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            iprovider = (UMOImmutableEndpoint) iterator.next();
            if (filter.accept(iprovider.getEndpointURI()))
            {
                UMOManagementContext managementContext = MuleServer.getManagementContext();
                UMOEndpointBuilder builder = new EndpointURIEndpointBuilder(iprovider, managementContext);
                return managementContext.getRegistry().lookupEndpointFactory().createInboundEndpoint(builder,
                    managementContext);
            }
        }

        return null;
    }

}
