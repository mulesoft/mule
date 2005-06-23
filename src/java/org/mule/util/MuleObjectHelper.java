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
package org.mule.util;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.ObjectFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.transformer.UMOTransformer;

/**
 * <code>MuleObjectHelper</code> is a helper class to assist in finding mule
 * server objects, such as endpoint and transformers
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
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
        StringTokenizer st = new StringTokenizer(list, delim);

        UMOTransformer tempTrans = null;
        UMOTransformer currentTrans = null;
        UMOTransformer returnTrans = null;
        UMOManager manager = MuleManager.getInstance();
        String key;
        while (st.hasMoreTokens()) {
            key = st.nextToken().trim();
            tempTrans = manager.lookupTransformer(key);
            if (tempTrans == null) {
                throw new MuleException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER, "Transformer: " + key));
            }
            if (currentTrans == null) {
                currentTrans = tempTrans;
                returnTrans = tempTrans;
            } else {
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
        for (Iterator iterator = endpoints.values().iterator(); iterator.hasNext();) {
            iprovider = (UMOImmutableEndpoint) iterator.next();
            if (iprovider.getProtocol().equals(protocol)) {
                return new MuleEndpoint(iprovider);
            }
        }
        return null;
    }

    public static UMOEndpoint getEndpointByEndpointUri(String endpointUri, boolean wildcardMatch)
    {
        ObjectFilter filter;
        if (wildcardMatch) {
            filter = new WildcardFilter(endpointUri);
        } else {
            filter = new EqualsFilter(endpointUri);
        }
        UMOImmutableEndpoint iprovider;
        Map endpoints = MuleManager.getInstance().getEndpoints();
        for (Iterator iterator = endpoints.values().iterator(); iterator.hasNext();) {
            iprovider = (UMOImmutableEndpoint) iterator.next();
            if (filter.accept(iprovider.getEndpointURI())) {
                return new MuleEndpoint(iprovider);
            }
        }
        return null;
    }
}
