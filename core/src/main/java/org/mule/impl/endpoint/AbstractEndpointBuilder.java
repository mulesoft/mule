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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractEndpointBuilder implements UMOEndpointBuilder
{
    public static final int GET_OR_CREATE_CONNECTOR = 0;
    public static final int ALWAYS_CREATE_CONNECTOR = 1;
    public static final int NEVER_CREATE_CONNECTOR = 2;
    public static final int USE_CONNECTOR = 3;

    protected UMOConnector connector;
    protected UMOEndpointURI endpointURI;
    protected UMOTransformer transformer;
    protected UMOTransformer responseTransformer;
    protected String name;
    protected Map properties;
    protected UMOTransactionConfig transactionConfig;
    protected UMOFilter filter;
    protected boolean deleteUnacceptedMessages = false;
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

    protected void configureEndpoint(MuleEndpoint ep) throws EndpointException, InitialisationException
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
        ep.setRemoteSync(getRemoteSync(connector));
        ep.setSynchronous(getSynchronous(connector));
        
        ep.setManagementContext(managementContext);
        
        ep.initialise();

    }

    protected abstract UMOImmutableEndpoint doBuildInboundEndpoint() throws EndpointException, InitialisationException;

    protected abstract UMOImmutableEndpoint doBuildOutboundEndpoint() throws EndpointException, InitialisationException;

    protected abstract UMOImmutableEndpoint doBuildResponseEndpoint() throws EndpointException, InitialisationException;

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
        return null;
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
        return createConnector != null ? createConnector.intValue() : getDefaultCreateConnector();
    }

    protected int getDefaultCreateConnector()
    {
        return GET_OR_CREATE_CONNECTOR;
    }

    protected String getName(UMOImmutableEndpoint endpoint) throws EndpointException
    {
        return name != null ? name : ObjectNameHelper.getEndpointName(endpoint);
    }

    protected Map getProperties()
    {
        // Add properties from builder, endpointURI and then seal (make unmodifiable)
        Map props = new HashMap();
        props.putAll(properties);
        props.putAll(endpointURI.getParams());
        props = Collections.unmodifiableMap(props);
        return props;
    }

    protected boolean getSynchronous(UMOConnector connector)
    {
        return synchronous != null ? synchronous.booleanValue() : getDefaultSynchronous(connector);

    }

    protected boolean getDefaultSynchronous(UMOConnector connector)
    {
        return managementContext.getRegistry().getConfiguration().isDefaultSynchronousEndpoints();
    }

    protected boolean getRemoteSync(UMOConnector connector)
    {
        return synchronous != null ? synchronous.booleanValue() : getDefaultRemoteSync(connector);

    }

    protected boolean getDefaultRemoteSync(UMOConnector connector)
    {
        return !connector.isRemoteSyncEnabled();
    }

    protected boolean getDeleteUnacceptedMessages(UMOConnector connector)
    {
        return synchronous != null ? synchronous.booleanValue() : getDefaultDeleteUnacceptedMessages(connector);

    }

    protected boolean getDefaultDeleteUnacceptedMessages(UMOConnector connector)
    {
        return connector.isRemoteSyncEnabled();
    }

    protected String getEndpointEncoding(UMOConnector connector)
    {
        return endpointEncoding != null ? endpointEncoding : getDefaultEndpointEncoding(connector);
    }

    protected String getDefaultEndpointEncoding(UMOConnector connector)
    {
        return managementContext.getRegistry().getConfiguration().getDefaultEncoding();
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
        return 0;
    }

    protected UMOTransformer getInboundTransformer(UMOConnector connector, UMOEndpointURI endpointURI)
        throws TransportFactoryException
    {
        // #1 Transformer set on builder
        if (transformer != null)
        {
            return transformer;
        }
        // #2 Transformer specified on uri
        else if (endpointURI.getTransformers() != null)
        {
            try
            {
                return MuleObjectHelper.getTransformer(endpointURI.getTransformers(), ",");
            }
            catch (MuleException e)
            {
                throw new TransportFactoryException(e);
            }
        }
        // #3 Default Transformer
        else
        {
            return getDefaultInboundTransformer(connector);
        }
    }

    protected UMOTransformer getDefaultInboundTransformer(UMOConnector connector) throws TransportFactoryException
    {
        try
        {
            UMOTransformer trans = null;
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

            String scheme = endpointURI.getSchemeMetaInfo();

            TransportServiceDescriptor sd = (TransportServiceDescriptor) RegistryContext.getRegistry()
                .lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, overrides);
            if (sd != null)
            {
                trans = sd.createInboundTransformer();
                if (trans != null)
                {
                    trans.initialise();
                }
            }
            else
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
            }
            return trans;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

    protected UMOTransformer getOutboundTransformer(UMOConnector connector, UMOEndpointURI endpointURI) throws TransportFactoryException
    {
        // #1 Transformer set on builder
        if (transformer != null)
        {
            return transformer;
        }
        // #2 Transformer specified on uri
        else if (endpointURI.getTransformers() != null)
        {
            try
            {
                return MuleObjectHelper.getTransformer(endpointURI.getTransformers(), ",");
            }
            catch (MuleException e)
            {
                throw new TransportFactoryException(e);
            }
        }
        // #3 Default Transformer
        else
        {
            return getDefaultOutboundTransformer(connector);
        }
    }

    protected UMOTransformer getDefaultOutboundTransformer(UMOConnector connector) throws TransportFactoryException
    {
        try
        {
            UMOTransformer trans = null;
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

            String scheme = endpointURI.getSchemeMetaInfo();

            TransportServiceDescriptor sd = (TransportServiceDescriptor) RegistryContext.getRegistry()
                .lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, overrides);
            if (sd != null)
            {
                trans = sd.createOutboundTransformer();
                if (trans != null)
                {
                    trans.initialise();
                }
            }
            else
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
            }
            return trans;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

    protected UMOTransformer getResponseTransformer(UMOConnector connector, UMOEndpointURI endpointURI) throws TransportFactoryException
    {
        // #1 Transformer set on builder
        if (responseTransformer != null)
        {
            return responseTransformer;
        }
        // #2 Transformer specified on uri
        else if (endpointURI.getResponseTransformers() != null)
        {
            try
            {
                return MuleObjectHelper.getTransformer(endpointURI.getResponseTransformers(), ",");
            }
            catch (MuleException e)
            {
                throw new TransportFactoryException(e);
            }
        }
        // #3 Default Transformer
        else
        {
            return getDefaultResponeTransformer(connector);
        }
    }

    protected UMOTransformer getDefaultResponeTransformer(UMOConnector connector) throws TransportFactoryException
    {
        try
        {
            UMOTransformer trans = null;
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

            String scheme = endpointURI.getSchemeMetaInfo();

            TransportServiceDescriptor sd = (TransportServiceDescriptor) RegistryContext.getRegistry()
                .lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, overrides);
            if (sd != null)
            {
                trans = sd.createResponseTransformer();
                if (trans != null)
                {
                    trans.initialise();
                }
            }
            else
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
            }
            return trans;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

    private UMOConnector getConnector(UMOEndpointURI endpointURI, UMOManagementContext managementContext)
        throws EndpointException
    {
        String scheme = endpointURI.getFullScheme();
        UMOConnector connector;
        try
        {
            if (endpointURI.getCreateConnector() == ALWAYS_CREATE_CONNECTOR)
            {
                connector = TransportFactory.createConnector(endpointURI, managementContext);
            }
            else if (endpointURI.getCreateConnector() == NEVER_CREATE_CONNECTOR)
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

    public UMOEndpointBuilder setConnector(UMOConnector connector)
    {
        this.connector = connector;
        return this;
    }


    public UMOEndpointBuilder setTransformer(UMOTransformer transformer)
    {
        this.transformer = transformer;
        return this;
    }

    public UMOEndpointBuilder setResponseTransformer(UMOTransformer responseTransformer)
    {
        this.responseTransformer = responseTransformer;
        return this;
    }

    public UMOEndpointBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    public UMOEndpointBuilder setProperties(Map properties)
    {
        this.properties = properties;
        return this;
    }

    public UMOEndpointBuilder setTransactionConfig(UMOTransactionConfig transactionConfig)
    {
        this.transactionConfig = transactionConfig;
        return this;
    }

    public UMOEndpointBuilder setFilter(UMOFilter filter)
    {
        this.filter = filter;
        return this;
    }

    public UMOEndpointBuilder setDeleteUnacceptedMessages(boolean deleteUnacceptedMessages)
    {
        this.deleteUnacceptedMessages = deleteUnacceptedMessages;
        return this;
    }

    public UMOEndpointBuilder setSecurityFilter(UMOEndpointSecurityFilter securityFilter)
    {
        this.securityFilter = securityFilter;
        return this;
    }

    public UMOEndpointBuilder setSynchronous(Boolean synchronous)
    {
        this.synchronous = synchronous;
        return this;
    }

    public UMOEndpointBuilder setRemoteSync(Boolean remoteSync)
    {
        this.remoteSync = remoteSync;
        return this;
    }

    public UMOEndpointBuilder setRemoteSyncTimeout(int remoteSyncTimeout)
    {
        this.remoteSyncTimeout = new Integer(remoteSyncTimeout);
        return this;
    }

    public UMOEndpointBuilder setStreaming(boolean streaming)
    {
        this.streaming = new Boolean(streaming);
        return this;
    }

    public UMOEndpointBuilder setInitialState(String initialState)
    {
        this.initialState = initialState;
        return this;
    }

    public UMOEndpointBuilder setEndpointEncoding(String endpointEncoding)
    {
        this.endpointEncoding = endpointEncoding;
        return this;
    }

    public UMOEndpointBuilder setCreateConnector(int createConnector)
    {
        this.createConnector = new Integer(createConnector);
        return this;
    }

    public UMOEndpointBuilder setRegistryId(String registryId)
    {
        this.registryId = registryId;
        return this;
    }

    public UMOEndpointBuilder setManagementContext(UMOManagementContext managementContext)
    {
        this.managementContext = managementContext;
        return this;
    }

    public UMOEndpointBuilder setConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;
        return this;
    }

}
