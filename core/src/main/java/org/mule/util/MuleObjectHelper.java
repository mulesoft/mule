/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.ObjectFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Iterator;
import java.util.Map;
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
     * Builds a linked list of UMOTransformers.
     * 
     * @param list - a list of transformers separated by "delim"
     * @param delim - the character used to delimit the transformers in the list
     * @return an UMOTransformer whose method getNextTransformer() will return the
     *         next transformer in the list.
     * @throws MuleException
     */
    public static UMOTransformer getTransformer(String list, String delim) throws MuleException
    {
        StringTokenizer st = new StringTokenizer(list, delim);
        UMOTransformer currentTrans = null;
        UMOTransformer returnTrans = null;

        while (st.hasMoreTokens())
        {
            String key = st.nextToken().trim();
            UMOTransformer tempTrans = RegistryContext.getRegistry().lookupTransformer(key);

            if (tempTrans == null)
            {
                throw new MuleException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER, "Transformer: "
                                                                                            + key));
            }

            if (currentTrans == null)
            {
                currentTrans = tempTrans;
                returnTrans = tempTrans;
            }
            else
            {
                currentTrans.setNextTransformer(tempTrans);
                currentTrans = tempTrans;
            }
        }

        return returnTrans;
    }

    public static UMOEndpoint getEndpointByProtocol(String protocol) throws UMOException
    {
        UMOImmutableEndpoint iprovider;
        Map endpoints = RegistryContext.getRegistry().getEndpoints();
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

    public static UMOEndpoint getEndpointByEndpointUri(String endpointUri, boolean wildcardMatch) throws UMOException
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
        Map endpoints = RegistryContext.getRegistry().getEndpoints();

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

}
