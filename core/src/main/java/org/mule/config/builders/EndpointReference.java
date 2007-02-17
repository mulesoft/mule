/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>EndpointReference</code> maintains a endpoint reference. Endpoints are
 * cloned when they are looked up for the manager, if there are container properties
 * or transformers set on the Endpoint the clone will have an inconsistent state if
 * the transformers or container properties have not been resolved. This class holds
 * the refernece and is invoked after the container properties/transformers are
 * resolved.
 */
public class EndpointReference
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(EndpointReference.class);

    private String propertyName;
    private String endpointName;
    private String address;
    private String transformer;
    private String responseTransformer;
    private String createConnector;
    private Object object;
    private Map properties;
    private UMOFilter filter;
    private UMOTransactionConfig transactionConfig;

    public EndpointReference(String propertyName,
                             String endpointName,
                             String address,
                             String transformer,
                             String responseTransformer,
                             String createConnector,
                             Object object)
    {
        this.propertyName = propertyName;
        this.endpointName = endpointName;
        this.address = address;
        this.transformer = transformer;
        this.responseTransformer = responseTransformer;
        this.object = object;
        this.createConnector = createConnector;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public String getEndpointName()
    {
        return endpointName;
    }

    public Object getObject()
    {
        return object;
    }

    public UMOTransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    public void setTransactionConfig(UMOTransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public void setFilter(UMOFilter filter)
    {
        this.filter = filter;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    public String getCreateConnector()
    {
        return createConnector;
    }

    public void setCreateConnector(String createConnector)
    {
        this.createConnector = createConnector;
    }

    public void resolveEndpoint() throws InitialisationException
    {
        try
        {
            MuleEndpoint ep = (MuleEndpoint)MuleManager.getRegistry().lookupEndpoint(endpointName);
            if (ep == null)
            {
                throw new InitialisationException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER,
                    "Endpoint '" + endpointName + "'"), this);
            }
            if (address != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Overloading endpoint uri for: " + endpointName + " from "
                                 + ep.getEndpointURI().toString() + " to " + address);
                }
                ep.setEndpointURI(new MuleEndpointURI(address));
            }
            if (createConnector != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Overloading createConnector property for endpoint: " + endpointName
                                 + " from " + ep.getCreateConnector() + " to " + createConnector);
                }
                ep.setCreateConnectorAsString(createConnector);
            }
            if (transformer != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Overloading Transformer for: " + endpointName + " from "
                                 + ep.getTransformer() + " to " + transformer);
                }
                UMOTransformer trans = MuleObjectHelper.getTransformer(transformer, " ");
                ep.setTransformer(trans);
            }

            if (responseTransformer != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Overloading responseTransformer for: " + endpointName + " from "
                                 + ep.getResponseTransformer() + " to " + responseTransformer);
                }
                UMOTransformer trans = MuleObjectHelper.getTransformer(responseTransformer, " ");
                ep.setResponseTransformer(trans);
            }

            if (filter != null)
            {
                ep.setFilter(filter);
            }
            if (properties != null)
            {
                ep.getProperties().putAll(properties);
            }
            if (transactionConfig != null)
            {
                ep.setTransactionConfig(transactionConfig);
            }

            ep.initialise();
            Method m = object.getClass().getMethod(propertyName, new Class[]{UMOEndpoint.class});
            if (m == null)
            {
                throw new InitialisationException(new Message(Messages.METHOD_X_WITH_PARAMS_X_NOT_FOUND_ON_X,
                    propertyName, UMOEndpoint.class.getName(), object.getClass().getName()), this);
            }

            m.invoke(object, new Object[]{ep});
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.CANT_SET_PROP_X_ON_X_OF_TYPE_X,
                propertyName, object.getClass().getName(), UMOEndpoint.class.getName()), e, this);
        }
    }

    public String toString()
    {
        return "EndpointReference{" + "propertyName='" + propertyName + "'" + ", endpointName='"
               + endpointName + "'" + ", address='" + address + "'" + ", transformer='" + transformer + "'"
               + ",  responseTransformer='" + responseTransformer + "'" + ", object=" + object
               + ", properties=" + properties + ", filter=" + filter + ", transactionConfig="
               + transactionConfig + "}";
    }
}
