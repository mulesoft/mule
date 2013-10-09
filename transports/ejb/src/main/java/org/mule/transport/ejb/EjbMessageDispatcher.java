/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ejb;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.rmi.RmiMessageDispatcher;

/**
 * Invokes a method on an EJB object stored in Jndi. A dispatcher is created for each
 * type of object invoked
 */
public class EjbMessageDispatcher extends RmiMessageDispatcher
{

    public EjbMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }
}
