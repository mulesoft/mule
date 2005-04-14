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
package org.mule.routing.response;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.routing.UMOResponseRouter;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Iterator;
import java.util.List;

/**
 * <code>ResponseMessageRouter</code> is a router that can be used to control
 * how the response in a request/response message flow is created.  Main usecase
 * is to aggregate a set of asynchonous events into a single response
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ResponseMessageRouter extends AbstractRouterCollection implements UMOResponseMessageRouter
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ResponseMessageRouter.class);

    private List endpoints = new CopyOnWriteArrayList();

    private UMOTransformer transformer;
    
    private boolean stopProcessing = true;

    public ResponseMessageRouter()
    {
        super(RouterStatistics.TYPE_RESPONSE);
    }

    public void route(UMOEvent event) throws RoutingException
    {
        UMOResponseRouter router = null;
        for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
        {
            router = (UMOResponseRouter) iterator.next();
            router.process(event);
            //Update stats
            if (getStatistics().isEnabled())
            {
                getStatistics().incrementRoutedMessage(event.getEndpoint());
            }
        }
    }

    public UMOMessage getResponse(UMOMessage message) throws RoutingException
    {
        UMOMessage result = null;
        if (routers.size() == 0)
        {
            result = message;
        } else {
            UMOResponseRouter router = null;
            for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
            {
                router = (UMOResponseRouter) iterator.next();
                result = router.getResponse(message);
            }

            if (result==null)
            {
                //Update stats
                if (getStatistics().isEnabled())
                {
                    getStatistics().incrementNoRoutedMessage();
                }
            }
        }

        if(result!=null && transformer!=null) {
            try
            {
                result = new MuleMessage(transformer.transform(result.getPayload()), result.getProperties());
            } catch (TransformerException e)
            {
                throw new RoutingException(result, null);
            }
        }
        return result;

    }

    public void addRouter(UMOResponseRouter router)
    {
        routers.add(router);
    }

    public UMOResponseRouter removeRouter(UMOResponseRouter router)
    {
        if (routers.remove(router))
        {
            return router;
        } else
        {
            return null;
        }
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        if (endpoint != null)
        {
            endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_RESPONSE);
            endpoints.add(endpoint);
        } else
        {
            throw new NullPointerException("Endpoint cannot be null");
        }
    }

    public boolean removeEndpoint(UMOEndpoint endpoint)
    {
        return endpoints.remove(endpoint);
    }

    public List getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List endpoints)
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            UMOEndpoint endpoint = (UMOEndpoint) iterator.next();
            addEndpoint(endpoint);
        }
    }

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see org.mule.umo.routing.UMOInboundMessageRouter
     */
    public UMOEndpoint getEndpoint(String name)
    {
        UMOEndpoint endpointDescriptor;
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            endpointDescriptor = (UMOEndpoint) iterator.next();
            if (endpointDescriptor.getName().equals(name))
            {
                return endpointDescriptor;
            }
        }
        return null;
    }

    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    public void setTransformer(UMOTransformer transformer)
    {
        this.transformer = transformer;
    }
    
    public boolean isStopProcessing() 
    {
    	return stopProcessing;
    }
    
    public void setStopProcessing(boolean stopProcessing)
    {
    	this.stopProcessing = stopProcessing;
    }

}
