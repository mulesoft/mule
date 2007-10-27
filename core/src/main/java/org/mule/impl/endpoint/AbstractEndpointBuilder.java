/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleTransactionConfig;
import org.mule.providers.AbstractConnector;
import org.mule.providers.ConnectionStrategy;
import org.mule.providers.SingleAttemptConnectionStrategy;
import org.mule.providers.service.TransportFactory;
import org.mule.providers.service.TransportFactoryException;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;
import org.mule.util.ObjectNameHelper;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract endpoint builder used for externalizing the complex creation logic of endpoints out of the
 * endpoint instance itself. <br/> The use of a builder allows i) Endpoints to be configured once and created
 * in a repeatable fashion (global endpoints), ii) Allow for much more extensibility in endpoint creation for
 * transport specific endpoints, streaming endpoints etc.<br/>

 */
public abstract class AbstractEndpointBuilder implements UMOEndpointBuilder
{
    public static final int GET_OR_CREATE_CONNECTOR = 0;
    public static final int ALWAYS_CREATE_CONNECTOR = 1;

    public static final int NEVER_CREATE_CONNECTOR = 2;
    public static final int USE_CONNECTOR = 3;

    protected UMOConnector connector;
    protected UMOEndpointURI endpointURI;
    protected List transformers = TransformerUtils.UNDEFINED;
    protected List responseTransformers = TransformerUtils.UNDEFINED;
    protected String name;
    protected Map properties = new HashMap();
    protected UMOTransactionConfig transactionConfig;
    protected UMOFilter filter;
    protected Boolean deleteUnacceptedMessages;
    protected UMOEndpointSecurityFilter securityFilter;
    protected Boolean synchronous;
    protected Boolean remoteSync;
    protected Integer remoteSyncTimeout;
    protected Boolean streaming;
    protected String initialState = UMOImmutableEndpoint.INITIAL_STATE_STARTED;
    protected String endpointEncoding;
    protected Integer createConnector;
    protected String registryId = null;
    protected UMOManagementContext managementContext;
    protected ConnectionStrategy connectionStrategy;

    public UMOImmutableEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException
    {
        return doBuildInboundEndpoint();
    }

    public UMOImmutableEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        return doBuildOutboundEndpoint();
    }

    public UMOImmutableEndpoint buildResponseEndpoint() throws EndpointException, InitialisationException
    {
        return doBuildResponseEndpoint();
    }

    protected void configureEndpoint(MuleEndpoint ep) throws InitialisationException, EndpointException
    {
        // protected String registryId = null; ??
        endpointURI.initialise();
        ep.setEndpointURI(endpointURI);
        ep.setCreateConnector(getCreateConnector());
        UMOConnector connector = getConnector();
        ep.setConnector(connector);

        // Do not inherit from connector
        ep.setSecurityFilter(getSecurityFilter());
        ep.setTransactionConfig(getTransactionConfig());
        ep.setProperties(getProperties());
        ep.setName(getName(ep));

        // Can inherit from connector
        ep.setConnectionStrategy(getConnectionStrategy(connector));
        ep.setDeleteUnacceptedMessages(getDeleteUnacceptedMessages(connector));
        ep.setEncoding(getEndpointEncoding(connector));
        ep.setFilter(getFilter(connector));
        ep.setInitialState(getInitialState(connector));
        ep.setRemoteSyncTimeout(getRemoteSyncTimeout(connector));
        ep.setStreaming(getStreaming(connector));

        boolean remoteSync = getRemoteSync(connector);
        ep.setRemoteSync(remoteSync);
        if (remoteSync)
        {
            ep.setSynchronous(true);
        }
        else
        {
            // Don't use default values for sync when configuring endpoint as with other attributes as
            // itcauses issue with XFireConnector. For now null=unset, and
            // default value is resolved in isSynchronous() method.
            if (synchronous != null)
            {
                ep.setSynchronous(synchronous.booleanValue());
            }
        }
        ep.setManagementContext(managementContext);
    }

    protected UMOImmutableEndpoint doBuildInboundEndpoint() throws InitialisationException, EndpointException
    {
        InboundEndpoint ep = new InboundEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getInboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        ep.setResponseTransformers(getResponseTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    protected UMOImmutableEndpoint doBuildOutboundEndpoint() throws InitialisationException, EndpointException
    {
        OutboundEndpoint ep = new OutboundEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getOutboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    protected UMOImmutableEndpoint doBuildResponseEndpoint() throws InitialisationException, EndpointException
    {
        ResponseEndpoint ep = new ResponseEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getInboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    protected boolean getStreaming(UMOConnector connector)
    {
        return streaming != null ? streaming.booleanValue() : getDefaultStreaming(connector);
    }

    protected boolean getDefaultStreaming(UMOConnector connector)
    {
        return false;
    }

    protected ConnectionStrategy getConnectionStrategy(UMOConnector connector)
    {
        return connectionStrategy != null ? connectionStrategy : getDefaultConnectionStrategy(connector);
    }

    protected ConnectionStrategy getDefaultConnectionStrategy(UMOConnector connector)
    {
        return new SingleAttemptConnectionStrategy();
    }

    protected UMOTransactionConfig getTransactionConfig()
    {
        return transactionConfig != null ? transactionConfig : getDefaultTransactionConfig();
    }

    protected UMOTransactionConfig getDefaultTransactionConfig()
    {
        return new MuleTransactionConfig();
    }

    protected UMOEndpointSecurityFilter getSecurityFilter()
    {
        return securityFilter != null ? securityFilter : getDefaultSecurityFilter();
    }

    protected UMOEndpointSecurityFilter getDefaultSecurityFilter()
    {
        return null;
    }

    protected UMOConnector getConnector() throws EndpointException
    {
        return connector != null ? connector : getDefaultConnector();
    }

    protected UMOConnector getDefaultConnector() throws EndpointException
    {
        return getConnector(endpointURI, managementContext);
    }

    protected int getCreateConnector()
    {
        if (createConnector != null)
        {
            return createConnector.intValue();
        }
        else
        {
            return endpointURI.getCreateConnector();
        }
    }

    protected int getDefaultCreateConnector()
    {
        return GET_OR_CREATE_CONNECTOR;
    }

    protected String getName(UMOImmutableEndpoint endpoint)
    {
        String uriName = endpointURI.getEndpointName();
        return name != null ? name :
                (StringUtils.isNotEmpty(uriName) ? uriName : ObjectNameHelper.getEndpointName(endpoint));
    }

    protected Map getProperties()
    {
        // Add properties from builder, endpointURI and then seal (make unmodifiable)
        Map props = new HashMap();
        if (properties != null)
        {
            props.putAll(properties);
        }
        if (endpointURI.getParams() != null)
        {
            props.putAll(endpointURI.getParams());
        }
        props = Collections.unmodifiableMap(props);
        return props;
    }

    protected boolean getRemoteSync(UMOConnector connector)
    {
        return remoteSync != null ? remoteSync.booleanValue() : getDefaultRemoteSync(connector);

    }

    protected boolean getDefaultRemoteSync(UMOConnector connector)
    {
        return false;
    }

    protected boolean getDeleteUnacceptedMessages(UMOConnector connector)
    {
        return deleteUnacceptedMessages != null
                                               ? deleteUnacceptedMessages.booleanValue()
                                               : getDefaultDeleteUnacceptedMessages(connector);

    }

    protected boolean getDefaultDeleteUnacceptedMessages(UMOConnector connector)
    {
        return false;
    }

    protected String getEndpointEncoding(UMOConnector connector)
    {
        return endpointEncoding != null ? endpointEncoding : getDefaultEndpointEncoding(connector);
    }

    protected String getDefaultEndpointEncoding(UMOConnector connector)
    {
        if (managementContext != null)
        {
            return managementContext.getRegistry().getConfiguration().getDefaultEncoding();
        }
        else
        {
            return System.getProperty("file.encoding");
        }
    }

    protected UMOFilter getFilter(UMOConnector connector)
    {
        return filter != null ? filter : getDefaultFilter(connector);

    }

    protected UMOFilter getDefaultFilter(UMOConnector connector)
    {
        return null;
    }

    protected String getInitialState(UMOConnector connector)
    {
        return initialState != null ? initialState : getDefaultInitialState(connector);

    }

    protected String getDefaultInitialState(UMOConnector connector)
    {
        return UMOImmutableEndpoint.INITIAL_STATE_STARTED;
    }

    protected int getRemoteSyncTimeout(UMOConnector connector)
    {
        return remoteSyncTimeout != null ? remoteSyncTimeout.intValue() : getDefaultRemoteSyncTimeout(connector);

    }

    protected int getDefaultRemoteSyncTimeout(UMOConnector connector)
    {
        return managementContext.getRegistry().getConfiguration().getDefaultSynchronousEventTimeout();
    }

    protected List getInboundTransformers(UMOConnector connector, UMOEndpointURI endpointURI)
        throws TransportFactoryException
    {
        // #1 Transformers set on builder
        if (TransformerUtils.isDefined(transformers))
        {
            return transformers;
        }

        // #2 Transformer specified on uri
        List transformers = getTransformersFromString(endpointURI.getTransformers());
        if (TransformerUtils.isDefined(transformers))
        {
            return transformers;
        }

        // #3 Default Transformer
        return getDefaultInboundTransformers(connector);
    }

    protected List getDefaultInboundTransformers(UMOConnector connector) throws TransportFactoryException
    {
        try
        {
            return TransformerUtils.getDefaultInboundTransformers(getNonNullServiceDescriptor(
                endpointURI.getSchemeMetaInfo(), getOverrides(connector)));
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

    protected List getOutboundTransformers(UMOConnector connector, UMOEndpointURI endpointURI)
        throws TransportFactoryException
    {
        // #1 Transformers set on builder
        if (TransformerUtils.isDefined(transformers))
        {
            return transformers;
        }

        // #2 Transformer specified on uri
        List transformers = getTransformersFromString(endpointURI.getTransformers());
        if (TransformerUtils.isDefined(transformers))
        {
            return transformers;
        }

        // #3 Default Transformer
        return getDefaultOutboundTransformers(connector);
    }

    protected List getDefaultOutboundTransformers(UMOConnector connector) throws TransportFactoryException
    {
        try
        {
            return TransformerUtils.getDefaultOutboundTransformers(getNonNullServiceDescriptor(
                endpointURI.getSchemeMetaInfo(), getOverrides(connector)));
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

    protected List getResponseTransformers(UMOConnector connector, UMOEndpointURI endpointURI)
        throws TransportFactoryException
    {
        // #1 Transformers set on builder
        if (TransformerUtils.isDefined(responseTransformers))
        {
            return responseTransformers;
        }

        // #2 Transformer specified on uri
        List transformers = getTransformersFromString(endpointURI.getResponseTransformers());
        if (TransformerUtils.isDefined(transformers))
        {
            return transformers;
        }

        // #3 Default Transformer
        return getDefaultResponseTransformers(connector);
    }

    protected List getDefaultResponseTransformers(UMOConnector connector) throws TransportFactoryException
    {
        try
        {
            return TransformerUtils.getDefaultResponseTransformers(getNonNullServiceDescriptor(
                endpointURI.getSchemeMetaInfo(), getOverrides(connector)));
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

    private List getTransformersFromString(String transformers) throws TransportFactoryException
    {
        try
        {
            return MuleObjectHelper.getTransformers(transformers, ",");
        }
        catch (MuleException e)
        {
            throw new TransportFactoryException(e);
        }
    }

    private Properties getOverrides(UMOConnector connector)
    {
        // Get connector specific overrides to set on the descriptor
        Properties overrides = new Properties();
        if (connector instanceof AbstractConnector)
        {
            Map so = ((AbstractConnector) connector).getServiceOverrides();
            if (so != null)
            {
                overrides.putAll(so);
            }
        }
        return overrides;
    }

    private TransportServiceDescriptor getNonNullServiceDescriptor(String scheme, Properties overrides)
        throws ServiceException
    {
        TransportServiceDescriptor sd = (TransportServiceDescriptor) RegistryContext.getRegistry()
            .lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, overrides);
        if (null != sd)
        {
            return sd;
        }
        else
        {
            throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
        }
    }

    private UMOConnector getConnector(UMOEndpointURI endpointURI, UMOManagementContext managementContext)
        throws EndpointException
    {
        String scheme = endpointURI.getFullScheme();
        UMOConnector connector;
        try
        {
            if (getCreateConnector() == ALWAYS_CREATE_CONNECTOR)
            {
                connector = TransportFactory.createConnector(endpointURI, managementContext);
                connector.setManagementContext(managementContext);
                managementContext.getRegistry().registerConnector(connector, managementContext);
            }
            else if (getCreateConnector() == NEVER_CREATE_CONNECTOR)
            {
                connector = TransportFactory.getConnectorByProtocol(scheme);
            }
            else if (endpointURI.getConnectorName() != null)
            {
                connector = managementContext.getRegistry().lookupConnector(endpointURI.getConnectorName());
                if (connector == null)
                {
                    throw new TransportFactoryException(CoreMessages.objectNotRegistered("Connector",
                        endpointURI.getConnectorName()));
                }
            }
            else
            {
                connector = TransportFactory.getConnectorByProtocol(scheme);
                if (connector == null)
                {
                    connector = TransportFactory.createConnector(endpointURI, managementContext);
                    managementContext.getRegistry().registerConnector(connector, managementContext);
                }
            }
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }

        if (connector == null)
        {
            Message m = CoreMessages.failedToCreateObjectWith("Endpoint", "endpointURI: " + endpointURI);
            m.setNextMessage(CoreMessages.objectIsNull("connector"));
            throw new TransportFactoryException(m);

        }
        return connector;
    }

    // Builder setters

    public void setConnector(UMOConnector connector)
    {
        this.connector = connector;

    }
    
    public void addTransformer(UMOTransformer transformer)
    {
        if (transformers == TransformerUtils.UNDEFINED)
        {
            transformers = new ArrayList();
        }
        transformers.add(transformer);
    }

    public void setTransformers(List transformers)
    {
        this.transformers = transformers;
    }

    public void setResponseTransformers(List responseTransformers)
    {
        this.responseTransformers = responseTransformers;

    }

    public void setName(String name)
    {
        this.name = name;

    }

    public void setProperties(Map properties)
    {
        this.properties = properties;

    }

    /**
     * Sets a property on the endpoint
     *
     * @param key the property key
     * @param value the value of the property
     */
    public void setProperty(String key, Object value)
    {
        properties.put(key, value);
    }

    public void setTransactionConfig(UMOTransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;

    }

    public void setFilter(UMOFilter filter)
    {
        this.filter = filter;

    }

    public void setDeleteUnacceptedMessages(boolean deleteUnacceptedMessages)
    {
        this.deleteUnacceptedMessages = new Boolean(deleteUnacceptedMessages);

    }

    public void setSecurityFilter(UMOEndpointSecurityFilter securityFilter)
    {
        this.securityFilter = securityFilter;

    }

    public void setSynchronous(boolean synchronous)
    {
        this.synchronous = new Boolean(synchronous);

    }

    public void setRemoteSync(boolean remoteSync)
    {
        this.remoteSync = new Boolean(remoteSync);

    }

    public void setRemoteSyncTimeout(int remoteSyncTimeout)
    {
        this.remoteSyncTimeout = new Integer(remoteSyncTimeout);

    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = new Boolean(streaming);

    }

    public void setInitialState(String initialState)
    {
        this.initialState = initialState;

    }

    public void setEndpointEncoding(String endpointEncoding)
    {
        this.endpointEncoding = endpointEncoding;

    }

    public void setCreateConnector(int createConnector)
    {
        this.createConnector = new Integer(createConnector);

    }

    public void setRegistryId(String registryId)
    {
        this.registryId = registryId;

    }

    public void setManagementContext(UMOManagementContext managementContext)
    {
        this.managementContext = managementContext;

    }

    public void setConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;

    }

    public UMOEndpointURI getEndpointURI()
    {
        return endpointURI;
    }

    public void setEndpointURI(UMOEndpointURI endpointURI)
    {
        this.endpointURI = endpointURI;

    }

}
