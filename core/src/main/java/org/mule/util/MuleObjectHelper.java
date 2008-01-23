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

import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.api.MuleException;
import org.mule.api.MuleContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.filter.ObjectFilter;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transformer.TransformerUtils;

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
     * {@link org.mule.transformer.TransformerUtils#UNDEFINED} if the names list is null
     * @throws DefaultMuleException
     */
    public static List getTransformers(String names, String delim) throws DefaultMuleException
    {
        if (null != names)
        {
            List transformers = new LinkedList();
            StringTokenizer st = new StringTokenizer(names, delim);
            while (st.hasMoreTokens())
            {
                String key = st.nextToken().trim();
                Transformer transformer = RegistryContext.getRegistry().lookupTransformer(key);

                if (transformer == null)
                {
                    throw new DefaultMuleException(CoreMessages.objectNotRegistered("Transformer", key));
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

    public static ImmutableEndpoint getEndpointByProtocol(String protocol) throws MuleException
    {
        ImmutableEndpoint iprovider;
        Collection endpoints = RegistryContext.getRegistry().getEndpoints();
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            iprovider = (ImmutableEndpoint) iterator.next();
            if (iprovider.getProtocol().equals(protocol))
            {
                MuleContext muleContext = MuleServer.getMuleContext();
                EndpointBuilder builder = new EndpointURIEndpointBuilder(iprovider, muleContext);
                return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(builder);
            }
        }
        return null;
    }

    public static ImmutableEndpoint getEndpointByEndpointUri(String endpointUri, boolean wildcardMatch) throws MuleException
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

        ImmutableEndpoint iprovider;
        Collection endpoints = RegistryContext.getRegistry().getEndpoints();

        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            iprovider = (ImmutableEndpoint) iterator.next();
            if (filter.accept(iprovider.getEndpointURI()))
            {
                MuleContext muleContext = MuleServer.getMuleContext();
                EndpointBuilder builder = new EndpointURIEndpointBuilder(iprovider, muleContext);
                return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(builder);
            }
        }

        return null;
    }

}
