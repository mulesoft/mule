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
import org.mule.config.spring.parsers.specific.URIBuilder;
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

    protected URIBuilder uriBuilder;
    protected UMOConnector connector;
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
    protected String encoding;
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
        UMOEndpointURI endpointURI = uriBuilder.getEndpoint();
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

        boolean remoteSync = getRemoteSync(connector);
        ep.setRemoteSync(remoteSync);
        if (remoteSync)
        {
            ep.setSynchronous(true);
        }
        else
        {
            ep.setSynchronous(getSynchronous(connector, ep));
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
        ep.setResponseTransformers(getResponseTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    protected UMOImmutableEndpoint doBuildResponseEndpoint() throws InitialisationException, EndpointException
    {
        ResponseEndpoint ep = new ResponseEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getInboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    protected boolean getSynchronous(UMOConnector connector, UMOImmutableEndpoint endpoint)
    {
        return synchronous != null ? synchronous.booleanValue() : getDefaultSynchronous(connector, endpoint);
    } 

    protected boolean getDefaultSynchronous(UMOConnector connector, UMOImmutableEndpoint endpoint)
    {
        if (connector != null && connector.isSyncEnabled(endpoint))
        {
            return true;
        }
        else
        {
            return RegistryContext.getConfiguration().isDefaultSynchronousEndpoints();
        }
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
        return getConnector(uriBuilder.getEndpoint(), managementContext);
    }

    protected int getCreateConnector()
    {
        if (createConnector != null)
        {
            return createConnector.intValue();
        }
        else
        {
            return uriBuilder.getEndpoint().getCreateConnector();
        }
    }

    protected int getDefaultCreateConnector()
    {
        return GET_OR_CREATE_CONNECTOR;
    }

    protected String getName(UMOImmutableEndpoint endpoint)
    {
        String uriName = uriBuilder.getEndpoint().getEndpointName();
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
        if (null != uriBuilder)
        {
            UMOEndpointURI endpointURI = uriBuilder.getEndpoint();
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
        // what is this for?!
        if (connector == null || connector.isRemoteSyncEnabled())
        {
            return false;
        }
        else
        {
            return false;
        }
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
        return encoding != null ? encoding : getDefaultEndpointEncoding(connector);
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
                uriBuilder.getEndpoint().getSchemeMetaInfo(), getOverrides(connector)));
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
                uriBuilder.getEndpoint().getSchemeMetaInfo(), getOverrides(connector)));
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
                uriBuilder.getEndpoint().getSchemeMetaInfo(), getOverrides(connector)));
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
        String scheme = uriBuilder.getEndpoint().getFullScheme();
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
            else if (uriBuilder.getEndpoint().getConnectorName() != null)
            {
                connector = managementContext.getRegistry().lookupConnector(uriBuilder.getEndpoint().getConnectorName());
                if (connector == null)
                {
                    throw new TransportFactoryException(CoreMessages.objectNotRegistered("Connector",
                        uriBuilder.getEndpoint().getConnectorName()));
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
    public void setProperty(Object key, Object value)
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

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;

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

    public URIBuilder getEndpointBuilder()
    {
        return uriBuilder;
    }

    public void setURIBuilder(URIBuilder URIBuilder)
    {
        this.uriBuilder = URIBuilder;

    }
    
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectionStrategy == null) ? 0 : connectionStrategy.hashCode());
        result = prime * result + ((connector == null) ? 0 : connector.hashCode());
        result = prime * result + ((createConnector == null) ? 0 : createConnector.hashCode());
        result = prime * result + ((deleteUnacceptedMessages == null) ? 0 : deleteUnacceptedMessages.hashCode());
        result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
        result = prime * result + ((uriBuilder == null) ? 0 : uriBuilder.getEndpoint().hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((initialState == null) ? 0 : initialState.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((remoteSync == null) ? 0 : remoteSync.hashCode());
        result = prime * result + ((remoteSyncTimeout == null) ? 0 : remoteSyncTimeout.hashCode());
        result = prime * result + ((responseTransformers == null) ? 0 : responseTransformers.hashCode());
        result = prime * result + ((securityFilter == null) ? 0 : securityFilter.hashCode());
        result = prime * result + ((streaming == null) ? 0 : streaming.hashCode());
        result = prime * result + ((synchronous == null) ? 0 : synchronous.hashCode());
        result = prime * result + ((transactionConfig == null) ? 0 : transactionConfig.hashCode());
        result = prime * result + ((transformers == null) ? 0 : transformers.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final AbstractEndpointBuilder other = (AbstractEndpointBuilder) obj;
        if (connectionStrategy == null)
        {
            if (other.connectionStrategy != null) return false;
        }
        else if (!connectionStrategy.equals(other.connectionStrategy)) return false;
        if (connector == null)
        {
            if (other.connector != null) return false;
        }
        else if (!connector.equals(other.connector)) return false;
        if (createConnector == null)
        {
            if (other.createConnector != null) return false;
        }
        else if (!createConnector.equals(other.createConnector)) return false;
        if (deleteUnacceptedMessages == null)
        {
            if (other.deleteUnacceptedMessages != null) return false;
        }
        else if (!deleteUnacceptedMessages.equals(other.deleteUnacceptedMessages)) return false;
        if (encoding == null)
        {
            if (other.encoding != null) return false;
        }
        else if (!encoding.equals(other.encoding)) return false;
        if (uriBuilder == null)
        {
            if (other.uriBuilder != null) return false;
        }
        else if (null == other.uriBuilder) return false;
        else if (!uriBuilder.getEndpoint().equals(other.uriBuilder.getEndpoint())) return false;
        if (filter == null)
        {
            if (other.filter != null) return false;
        }
        else if (!filter.equals(other.filter)) return false;
        if (initialState == null)
        {
            if (other.initialState != null) return false;
        }
        else if (!initialState.equals(other.initialState)) return false;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else if (!name.equals(other.name)) return false;
        if (properties == null)
        {
            if (other.properties != null) return false;
        }
        else if (!properties.equals(other.properties)) return false;
        if (remoteSync == null)
        {
            if (other.remoteSync != null) return false;
        }
        else if (!remoteSync.equals(other.remoteSync)) return false;
        if (remoteSyncTimeout == null)
        {
            if (other.remoteSyncTimeout != null) return false;
        }
        else if (!remoteSyncTimeout.equals(other.remoteSyncTimeout)) return false;
        if (responseTransformers == null)
        {
            if (other.responseTransformers != null) return false;
        }
        else if (!responseTransformers.equals(other.responseTransformers)) return false;
        if (securityFilter == null)
        {
            if (other.securityFilter != null) return false;
        }
        else if (!securityFilter.equals(other.securityFilter)) return false;
        if (streaming == null)
        {
            if (other.streaming != null) return false;
        }
        else if (!streaming.equals(other.streaming)) return false;
        if (synchronous == null)
        {
            if (other.synchronous != null) return false;
        }
        else if (!synchronous.equals(other.synchronous)) return false;
        if (transactionConfig == null)
        {
            if (other.transactionConfig != null) return false;
        }
        else if (!transactionConfig.equals(other.transactionConfig)) return false;
        if (transformers == null)
        {
            if (other.transformers != null) return false;
        }
        else if (!transformers.equals(other.transformers)) return false;
        return true;
    }

    public Object clone() throws CloneNotSupportedException
    {
        UMOEndpointBuilder builder = (UMOEndpointBuilder) super.clone();
        builder.setConnector(connector);
        builder.setURIBuilder(uriBuilder);
        builder.setTransformers(transformers);
        builder.setResponseTransformers(responseTransformers);
        builder.setName(name);
        builder.setProperties(properties);
        builder.setTransactionConfig(transactionConfig);
        builder.setFilter(filter);
        builder.setSecurityFilter(securityFilter);
        builder.setInitialState(initialState);
        builder.setEncoding(encoding);
        builder.setRegistryId(registryId);
        builder.setManagementContext(managementContext);
        builder.setConnectionStrategy(connectionStrategy);

        if (deleteUnacceptedMessages != null)
        {
            builder.setDeleteUnacceptedMessages(deleteUnacceptedMessages.booleanValue());
        }
        if (createConnector != null)
        {
            builder.setCreateConnector(createConnector.intValue());
        }
        if (synchronous != null)
        {
            builder.setSynchronous(synchronous.booleanValue());
        }
        if (remoteSync != null)
        {
            builder.setRemoteSync(remoteSync.booleanValue());
        }
        if (remoteSyncTimeout != null)
        {
            builder.setRemoteSyncTimeout(remoteSyncTimeout.intValue());
        }
        if (streaming != null)
        {
            builder.setStreaming(streaming.booleanValue());

        }

        return builder;
    }
    
}
