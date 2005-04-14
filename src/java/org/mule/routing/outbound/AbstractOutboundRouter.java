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
package org.mule.routing.outbound;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.PropertyExtractor;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.DefaultPropertiesExtractor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.routing.UMOOutboundRouter;

import java.util.List;

/**
 * <code>AbstractOutboundRouter</code> is a base router class that tracks statics about message
 * processing through the router.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractOutboundRouter implements UMOOutboundRouter
{
    public static final int ENABLE_CORREATION_IF_NOT_SET = 0;
    public static final int ENABLE_CORREATION_ALWAYS = 1;
    public static final int ENABLE_CORREATION_NEVER = 2;
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List endpoints = new CopyOnWriteArrayList();

    protected String replyTo = null;

    protected int enableCorrelation = ENABLE_CORREATION_IF_NOT_SET;

    protected PropertyExtractor propertyExtractor = new DefaultPropertiesExtractor();

    protected RouterStatistics routerStatistics;

    protected UMOTransactionConfig transactionConfig;


    public void dispatch(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        setMessageProperties(session, message, endpoint);
        session.dispatchEvent(message, endpoint);
        if (routerStatistics != null)
        {
            if (routerStatistics.isEnabled())
            {
                routerStatistics.incrementRoutedMessage(endpoint);
            }
        }
    }

    public UMOMessage send(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        setMessageProperties(session, message, endpoint);
        if (replyTo != null)
        {
            dispatch(session, message, endpoint);
            return null;
        }

        UMOMessage result = session.sendEvent(message, endpoint);
        if (routerStatistics != null)
        {
            if (routerStatistics.isEnabled())
            {
                routerStatistics.incrementRoutedMessage(endpoint);
            }
        }
        return result;
    }

    protected void setMessageProperties(UMOSession session, UMOMessage message, UMOEndpoint endpoint) throws UniqueIdNotSupportedException
    {
        if (replyTo != null)
        {
            //if replyTo is set we'll probably want the correlationId set as well
            message.setReplyTo(replyTo);
            message.setProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY, session.getComponent().getDescriptor().getName());
            if (logger.isDebugEnabled()) logger.debug("Setting replyTo=" + replyTo + " for outbound endpoint: " + endpoint.getEndpointURI());
        }
        if (enableCorrelation != ENABLE_CORREATION_NEVER)
        {
            boolean correlationSet = message.getCorrelationId()!=null;
            if(correlationSet && (enableCorrelation == ENABLE_CORREATION_IF_NOT_SET)) {
                logger.debug("CorrelationId is already set, not setting it again");
                return;
            } else if(correlationSet) {
                logger.debug("CorrelationId is already set, but router is configured to overwrite it");
            }

            String correlation = null;
            Object o = propertyExtractor.getPropertry(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
            if(logger.isDebugEnabled()) logger.debug("Extracted correlation Id as: " + o);
            correlation = o.toString();

            int seq = 1;
//            o = propertyExtractor.getPropertry(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, message);
//            if (logger.isDebugEnabled()) logger.debug("Extracted correlation sequence as: " + o);
//            if (o != null)
//            {
//                try
//                {
//                    seq = Integer.parseInt(o.toString());
//                } catch (NumberFormatException e)
//                {
//                    if (logger.isDebugEnabled()) logger.debug("Invalid Correlation sequence value: " + o.toString() + ". Defaulting to 1");
//                    seq = 1;
//                }
//            }

            int group = 1;
//            o = propertyExtractor.getPropertry(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, message);
//            if (logger.isDebugEnabled()) logger.debug("Extracted correlation Group size as: " + o);
//            if (o != null)
//            {
//                try
//                {
//                    group = Integer.parseInt(o.toString());
//                } catch (NumberFormatException e)
//                {
//                    if (logger.isDebugEnabled()) logger.debug("Invalid Correlation group value: " + o.toString() + ". Defaulting to 1");
//                    group = 1;
//                }
//            }

            if (logger.isDebugEnabled())
            {
                StringBuffer buf = new StringBuffer();
                buf.append("Setting Correlation info on Outbound router for endpoint: ").append(endpoint.getEndpointURI());
                buf.append("\n").append("Id=").append(correlation);
//                buf.append(", ").append("Seq=").append(seq);
//                buf.append(", ").append("Group Size=").append(group);
                logger.debug(buf.toString());
            }
            message.setCorrelationId(correlation);
//            message.setCorrelationGroupSize(group);
//            message.setCorrelationSequence(seq);
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
        if (replyTo != null)
        {
            this.replyTo = MuleManager.getInstance().lookupEndpointIdentifier(replyTo, replyTo);
        } else
        {
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

    public int getEnableCorrelation()
    {
        return enableCorrelation;
    }

    public void setEnableCorrelation(int enableCorrelation)
    {
        this.enableCorrelation = enableCorrelation;
    }

    public void setEnableCorrelationAsString(String enableCorrelation)
    {
        if(enableCorrelation!=null) {
            if(enableCorrelation.equals("ALWAYS")) {
                this.enableCorrelation = ENABLE_CORREATION_ALWAYS;
            } else if(enableCorrelation.equals("NEVER")) {
                this.enableCorrelation = ENABLE_CORREATION_NEVER;
            } else if(enableCorrelation.equals("IF_NOT_SET")) {
                this.enableCorrelation = ENABLE_CORREATION_IF_NOT_SET;
            } else {
                throw new IllegalArgumentException("Value for enableCorrelation not recognised: " + enableCorrelation);
            }
        }
    }

    public PropertyExtractor getPropertyExtractor()
    {
        return propertyExtractor;
    }

    public void setPropertyExtractor(PropertyExtractor propertyExtractor)
    {
        this.propertyExtractor = propertyExtractor;
    }

    public UMOTransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    public void setTransactionConfig(UMOTransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }
}
