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
package org.mule.routing.outbound;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.PropertyExtractor;
import org.mule.management.stats.RouterStatistics;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.routing.UMOOutboundRouter;

import java.util.List;

/**
 * <code>AbstractOutboundRouter</code> is a base router class that tracks statics about message
 * processing through the router.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractOutboundRouter implements UMOOutboundRouter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List endpoints = new CopyOnWriteArrayList();

    protected String replyTo = null;
    protected boolean correlationId = false;

    protected PropertyExtractor correlationIdExtractor = null;
    protected PropertyExtractor correlationSequenceExtractor = null;
    protected PropertyExtractor correlationGroupExtractor = null;

    protected RouterStatistics routerStatistics;


    public void dispatch(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        setMessageProperties(session, message, endpoint);
        session.dispatchEvent(message, endpoint);
        if(routerStatistics!=null) {
            if(routerStatistics.isEnabled() ) {
                routerStatistics.incrementRoutedMessage(endpoint);
            }
        }
    }

    public UMOMessage send(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        setMessageProperties(session, message, endpoint);
        if(replyTo!=null) {
            dispatch(session, message, endpoint);
            return null;
        }

        UMOMessage result = session.sendEvent(message, endpoint);
        if(routerStatistics!=null) {
            if(routerStatistics.isEnabled() ) {
                routerStatistics.incrementRoutedMessage(endpoint);
            }
        }
        return result;
    }

    protected void setMessageProperties(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UniqueIdNotSupportedException
    {
        if(replyTo!=null) {
            message.setReplyTo(replyTo);
            message.setProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY, session.getComponent().getDescriptor().getName());
            if(logger.isDebugEnabled()) logger.debug("Setting replyTo=" + replyTo + " for outbound endpoint: " + endpoint.getEndpointURI());
        }
        if(correlationId) {
            String correlation = null;
            if(correlationIdExtractor==null) {
                correlation = message.getUniqueId();
            } else {
                Object o = correlationIdExtractor.getPropertry(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
                if(logger.isDebugEnabled()) logger.debug("Extracted correlation Id as: " + o);
                correlation = o.toString();
            }
            int seq = 1;
            if(correlationSequenceExtractor!=null) {
                Object o = correlationSequenceExtractor.getPropertry(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, message);
                if(logger.isDebugEnabled()) logger.debug("Extracted correlation sequence as: " + o);
                if(o!=null) {
                    try
                    {
                        seq = Integer.parseInt(o.toString());
                    } catch (NumberFormatException e)
                    {
                        if(logger.isDebugEnabled()) logger.debug("Invalid Correlation sequence value: " + o.toString() + ". Defaulting to 1");
                        seq=1;
                    }
                }
            }

            int group = 1;
            if(correlationGroupExtractor!=null) {
                Object o = correlationGroupExtractor.getPropertry(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, message);
                if(logger.isDebugEnabled()) logger.debug("Extracted correlation Group size as: " + o);
                if(o!=null) {
                    try
                    {
                        group = Integer.parseInt(o.toString());
                    } catch (NumberFormatException e)
                    {
                        if(logger.isDebugEnabled()) logger.debug("Invalid Correlation group value: " + o.toString() + ". Defaulting to 1");
                        group=1;
                    }
                }
            }
            if(logger.isDebugEnabled()) {
                StringBuffer buf = new StringBuffer();
                buf.append("Setting Correlation info form Outbound router for endpoint: ").append(endpoint.getEndpointURI());
                buf.append("\n").append("Id=").append(correlation);
                buf.append(", ").append("Seq=").append(seq);
                buf.append(", ").append("Group Size=").append(group);
                logger.debug(buf.toString());
            }
            message.setCorrelationId(correlation);
            message.setCorrelationGroupSize(group);
            message.setCorrelationSequence(seq);
        }
    }

    public List getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List endpoints)
    {
        this.endpoints = new CopyOnWriteArrayList(endpoints);
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoints.add(endpoint);
    }

    public boolean removeEndpoint(UMOEndpoint endpoint)
    {
        return endpoints.remove(endpoint);
    }

    public String getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(String replyTo)
    {
        if(replyTo!=null) {
            this.replyTo = MuleManager.getInstance().lookupEndpointIdentifier(replyTo, replyTo);
        } else {
            this.replyTo = null;
        }
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }

    public void setRouterStatistics(RouterStatistics routerStatistics)
    {
        this.routerStatistics = routerStatistics;
    }

    public boolean isCorrelationId()
    {
        return correlationId;
    }

    public void setCorrelationId(boolean correlationId)
    {
        this.correlationId = correlationId;
    }

    public PropertyExtractor getCorrelationIdExtractor()
    {
        return correlationIdExtractor;
    }

    public void setCorrelationIdExtractor(PropertyExtractor correlationIdExtractor)
    {
        this.correlationIdExtractor = correlationIdExtractor;
    }

    public PropertyExtractor getCorrelationSequenceExtractor()
    {
        return correlationSequenceExtractor;
    }

    public void setCorrelationSequenceExtractor(PropertyExtractor correlationSequenceExtractor)
    {
        this.correlationSequenceExtractor = correlationSequenceExtractor;
    }

    public PropertyExtractor getCorrelationGroupExtractor()
    {
        return correlationGroupExtractor;
    }

    public void setCorrelationGroupExtractor(PropertyExtractor correlationGroupExtractor)
    {
        this.correlationGroupExtractor = correlationGroupExtractor;
    }
}
