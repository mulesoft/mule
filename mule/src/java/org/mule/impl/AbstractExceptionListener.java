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
package org.mule.impl;

import org.mule.umo.UMOMessage;
import org.mule.umo.MessagingException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.EndpointException;
import org.mule.transformers.xml.ObjectToXml;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.ExceptionListener;
import java.util.List;
import java.util.Iterator;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

/**
 * <code>AbstractExceptionListener</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractExceptionListener implements ExceptionListener
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List endpoints = new CopyOnWriteArrayList();

    public List getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List endpoints)
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();) {
            addEndpoint((UMOEndpoint) iterator.next());
        }
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        if(endpoint!=null) {
            if(endpoint.getTransformer()==null) {
                endpoint.setTransformer(new ObjectToXml());
            }
            endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER);
            endpoints.add(endpoint);
        }
    }

    public boolean removeEndpoint(UMOEndpoint endpoint)
    {
        return endpoints.remove(endpoint);
    }

    public final void exceptionThrown(Exception e) {
        Throwable t = getExceptionType(e, RoutingException.class);
        if(t!=null) {
            RoutingException re = (RoutingException) t;
            handleRoutingException(re.getUmoMessage(), re.getEndpoint(), e);
            return;
        }

        t = getExceptionType(e, MessagingException.class);
        if(t!=null) {
            MessagingException me = (MessagingException) t;
            handleMessagingException(me.getUmoMessage(), e);
            return;
        }

        t = getExceptionType(e, LifecycleException.class);
        if(t!=null) {
            LifecycleException le = (LifecycleException) t;
            handleLifecycleException(le.getComponent(), e);
            return;
        }

        handleStandardException(e);
    }

    protected Throwable getExceptionType(Exception e, Class exceptionType) {
        Throwable current = e;
        while(current!=null) {
            if(e.getClass().isAssignableFrom(exceptionType)) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }

    public abstract void handleMessagingException(UMOMessage message, Throwable e);

    public abstract void handleRoutingException(UMOMessage message, UMOEndpoint endpoint, Throwable e);

    public abstract void handleLifecycleException(Object component, Throwable e);

    public abstract void handleStandardException(Throwable e);
}
