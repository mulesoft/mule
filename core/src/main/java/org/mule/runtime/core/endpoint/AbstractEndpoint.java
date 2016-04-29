/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.EndpointSecurityFilter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.util.ClassUtils;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ImmutableMuleEndpoint</code> describes a Provider in the Mule Server. A
 * endpoint is a grouping of an endpoint, an endpointUri and a transformer.
 */
public abstract class AbstractEndpoint extends AbstractAnnotatedObject implements ImmutableEndpoint, Disposable
{

    private static final long serialVersionUID = -1650380871293160973L;

    public static final String PROPERTY_PROCESS_SYNCHRONOUSLY = "processSynchronously";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(AbstractEndpoint.class);

    /**
     * The endpoint used to communicate with the external system
     */
    private final Connector connector;

    /**
     * The endpointUri on which to send or receive information
     */
    private final EndpointURI endpointUri;

    private final EndpointMessageProcessorChainFactory messageProcessorsFactory;

    private final List <MessageProcessor> messageProcessors;

    private final List <MessageProcessor> responseMessageProcessors;
    
    private MessageProcessor messageProcessorChain;

    /**
     * The name for the endpoint
     */
    private final String name;

    /**
     * Any additional properties for the endpoint
     * // TODO This should be final. See MULE-3105
     * // TODO Shouldn't this be guarded from concurrent writes?
     */
    private Map properties = new HashMap();

    /**
     * The transaction configuration for this endpoint
     */
    private final TransactionConfig transactionConfig;

    /**
     * determines whether unaccepted filtered events should be removed from the
     * source. If they are not removed its up to the Message receiver to handle
     * recieving the same message again
     */
    private final boolean deleteUnacceptedMessages;

    private final MessageExchangePattern messageExchangePattern;
    
    /**
     * How long to block when performing a remote synchronisation to a remote host.
     * This property is optional and will be set to the default Synchonous MuleEvent
     * time out value if not set
     */
    private final int responseTimeout;

    /**
     * The state that the endpoint is initialised in such as started or stopped
     */
    private final String initialState;

    private final String endpointEncoding;

    private MuleContext muleContext;

    protected RetryPolicyTemplate retryPolicyTemplate;

    private String endpointBuilderName;

    private final String endpointMimeType;

    private AbstractRedeliveryPolicy redeliveryPolicy;

    private boolean disableTransportTransformer = false;

    public AbstractEndpoint(Connector connector,
                            EndpointURI endpointUri,
                            String name,
                            Map properties,
                            TransactionConfig transactionConfig,
                            boolean deleteUnacceptedMessages,
                            MessageExchangePattern messageExchangePattern,
                            int responseTimeout,
                            String initialState,
                            String endpointEncoding,
                            String endpointBuilderName,
                            MuleContext muleContext,
                            RetryPolicyTemplate retryPolicyTemplate,
                            AbstractRedeliveryPolicy redeliveryPolicy,
                            EndpointMessageProcessorChainFactory messageProcessorsFactory,
                            List <MessageProcessor> messageProcessors,
                            List <MessageProcessor> responseMessageProcessors,
                            boolean disableTransportTransformer,
                            String endpointMimeType)
    {
        this.connector = connector;
        this.endpointUri = endpointUri;
        this.name = name;
        // TODO Properties should be immutable. See MULE-3105
        // this.properties = Collections.unmodifiableMap(properties);
        this.properties.putAll(properties);
        this.transactionConfig = transactionConfig;
        this.deleteUnacceptedMessages = deleteUnacceptedMessages;

        this.responseTimeout = responseTimeout;
        this.initialState = initialState;
        this.endpointEncoding = endpointEncoding;
        this.endpointBuilderName = endpointBuilderName;
        this.muleContext = muleContext;
        this.retryPolicyTemplate = retryPolicyTemplate;
        this.redeliveryPolicy = redeliveryPolicy;
        this.endpointMimeType = endpointMimeType;
        this.disableTransportTransformer = disableTransportTransformer;
        this.messageExchangePattern = messageExchangePattern;
        this.messageProcessorsFactory = messageProcessorsFactory;
        if (messageProcessors == null)
        {
            this.messageProcessors = Collections.emptyList();
        }
        else
        {
            this.messageProcessors = messageProcessors;
        }
        if (responseMessageProcessors == null)
        {
            this.responseMessageProcessors = Collections.emptyList();
        }
        else
        {
            this.responseMessageProcessors = responseMessageProcessors;
        }
    }

    @Override
    public EndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    @Override
    public String getAddress()
    {
        EndpointURI uri = getEndpointURI();
        if (uri != null)
        {
            return uri.getUri().toString();
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getEncoding()
    {
        return endpointEncoding;
    }

    @Override
    public String getMimeType()
    {
        return endpointMimeType;
    }

    @Override
    public Connector getConnector()
    {
        return connector;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return messageProcessorsFactory;
    }

    @Override
    public List <MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
    }

    @Override
    public List <MessageProcessor> getResponseMessageProcessors()
    {
        return responseMessageProcessors;
    }

    /** @deprecated use getMessageProcessors() */
    @Deprecated
    public List<Transformer> getTransformers()
    {
        return getTransformersFromProcessorList(messageProcessors);
    }

    @Override
    public Map getProperties()
    {
        return properties;
    }

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    @Override
    public String toString()
    {
        // Use the interface to retrieve the string and set
        // the endpoint uri to a default value
        String sanitizedEndPointUri = null;
        URI uri = null;
        if (endpointUri != null)
        {
            sanitizedEndPointUri = endpointUri.toString();
            uri = endpointUri.getUri();
        }
        // The following will further sanitize the endpointuri by removing
        // the embedded password. This will only remove the password if the
        // uri contains all the necessary information to successfully rebuild the url
        if (uri != null && (uri.getRawUserInfo() != null) && (uri.getScheme() != null) && (uri.getHost() != null)
                && (uri.getRawPath() != null))
        {
            // build a pattern up that matches what we need tp strip out the password
            Pattern sanitizerPattern = Pattern.compile("(.*):.*");
            Matcher sanitizerMatcher = sanitizerPattern.matcher(uri.getRawUserInfo());
            if (sanitizerMatcher.matches())
            {
                sanitizedEndPointUri = new StringBuilder(uri.getScheme()).append("://")
                        .append(sanitizerMatcher.group(1))
                        .append(":<password>")
                        .append("@")
                        .append(uri.getHost())
                        .append(uri.getRawPath())
                        .toString();
            }
            if (uri.getRawQuery() != null)
            {
                sanitizedEndPointUri = sanitizedEndPointUri + "?" + uri.getRawQuery();
            }

        }

        return ClassUtils.getClassName(getClass()) + "{endpointUri=" + sanitizedEndPointUri + ", connector="
                + connector + ",  name='" + name + "', mep=" + messageExchangePattern + ", properties=" + properties
                + ", transactionConfig=" + transactionConfig + ", deleteUnacceptedMessages=" + deleteUnacceptedMessages
                + ", initialState=" + initialState + ", responseTimeout="
                + responseTimeout + ", endpointEncoding=" + endpointEncoding + ", disableTransportTransformer="
                + disableTransportTransformer + "}";
    }

    @Override
    public String getProtocol()
    {
        return connector.getProtocol();
    }

    @Override
    public TransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    protected static boolean equal(Object a, Object b)
    {
        return ClassUtils.equal(a, b);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        final AbstractEndpoint other = (AbstractEndpoint) obj;
        return equal(retryPolicyTemplate, other.retryPolicyTemplate)
                && equal(connector, other.connector)
                && deleteUnacceptedMessages == other.deleteUnacceptedMessages
                && equal(endpointEncoding, other.endpointEncoding)
                && equal(endpointUri, other.endpointUri)
                && equal(initialState, other.initialState)
                // don't include lifecycle state as lifecycle code includes hashing
                // && equal(initialised, other.initialised)
                && equal(messageExchangePattern, other.messageExchangePattern)
                && equal(name, other.name) 
                && equal(properties, other.properties)
                && responseTimeout == other.responseTimeout
                && equal(messageProcessors, other.messageProcessors)
                && equal(responseMessageProcessors, other.responseMessageProcessors)
                && equal(transactionConfig, other.transactionConfig)
                && disableTransportTransformer == other.disableTransportTransformer;
    }

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{this.getClass(), retryPolicyTemplate, connector,
                deleteUnacceptedMessages ? Boolean.TRUE : Boolean.FALSE,
                endpointEncoding,
                endpointUri,
                initialState,
                // don't include lifecycle state as lifecycle code includes hashing
                // initialised,
                messageExchangePattern,
                name,
                properties, 
                Integer.valueOf(responseTimeout),
                responseMessageProcessors,
                transactionConfig,
                messageProcessors,
                disableTransportTransformer ? Boolean.TRUE : Boolean.FALSE});
    }

    @Override
    public Filter getFilter()
    {
        // Call the first MessageFilter in the chain "the filter".
        for (MessageProcessor mp : messageProcessors)
        {
            if (mp instanceof MessageFilter)
            {
                return ((MessageFilter) mp).getFilter();
            }
        }
        return null;
    }

    @Override
    public boolean isDeleteUnacceptedMessages()
    {
        return deleteUnacceptedMessages;
    }

    /**
     * Returns an EndpointSecurityFilter for this endpoint. If one is not set, there
     * will be no authentication on events sent via this endpoint
     *
     * @return EndpointSecurityFilter responsible for authenticating message flow via
     *         this endpoint.
     * @see org.mule.runtime.core.api.security.EndpointSecurityFilter
     */
    @Override
    public EndpointSecurityFilter getSecurityFilter()
    {
        for (MessageProcessor mp : messageProcessors)
        {
            if (mp instanceof SecurityFilterMessageProcessor)
            {
                SecurityFilter filter = ((SecurityFilterMessageProcessor)mp).getFilter();
                if (filter instanceof EndpointSecurityFilter)
                {
                    return (EndpointSecurityFilter) filter;
                }
            }
        }

        return null;
    }

    @Override
    public MessageExchangePattern getExchangePattern()
    {
        return messageExchangePattern;
    }

    /**
     * The timeout value for remoteSync invocations
     *
     * @return the timeout in milliseconds
     */
    @Override
    public int getResponseTimeout()
    {
        if (muleContext.getConfiguration().isDisableTimeouts())
        {
            return MuleEvent.TIMEOUT_WAIT_FOREVER;
        }
        return responseTimeout;
    }

    /**
     * Sets the state the endpoint will be loaded in. The States are 'stopped' and
     * 'started' (default)
     *
     * @return the endpoint starting state
     */
    @Override
    public String getInitialState()
    {
        return initialState;
    }

    private List<Transformer> getTransformersFromProcessorList(List<MessageProcessor> processors)
    {
        List<Transformer> transformers = new LinkedList<Transformer>();
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Transformer)
            {
                transformers.add((Transformer) processor);
            }
            else if (processor instanceof MessageProcessorChain)
            {
                transformers.addAll(getTransformersFromProcessorList(((MessageProcessorChain) processor).getMessageProcessors()));
            }
        }
        return transformers;
    }

    @Override
    public Object getProperty(Object key)
    {
        return properties.get(key);
    }

    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    @Override
    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        return redeliveryPolicy;
    }

    @Override
    public String getEndpointBuilderName()
    {
        return endpointBuilderName;
    }

    @Override
    public boolean isProtocolSupported(String protocol)
    {
        return connector.supportsProtocol(protocol);
    }
    
    @Override
    public boolean isDisableTransportTransformer() 
    {
        return disableTransportTransformer;
    }

    @Override
    public void dispose()
    {
        this.muleContext = null;

        if (this.messageProcessorChain instanceof Disposable)
        {
            ((Disposable) this.messageProcessorChain).dispose();
        }

        // Don't clear this, since it changes the hash code, which can foul up shutdown processing
        // when objects have been keyed by endpoint, e.g. dispatchers
        // this.messageProcessors.clear();

        this.messageProcessorChain = null;
    }

    public MessageProcessor getMessageProcessorChain(FlowConstruct flowContruct) throws MuleException
    {
        if (messageProcessorChain == null)
        {
            messageProcessorChain = createMessageProcessorChain(flowContruct);
        }
        return messageProcessorChain;
    }

    abstract protected MessageProcessor createMessageProcessorChain(FlowConstruct flowContruct) throws MuleException;
}
