/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bpm;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Business rules.
 */
public class Rules implements Initialisable, Disposable, MessageService
{
    /** The underlying Rules Engine */
    private final RulesEngine rulesEngine;

    /** The resource containing the rules definition  */
    private final String resource;

    /** Provider-specific configuration data */
    private final Object configuration;
    
    /** Entry point for event stream (used by CEP) */
    private String entryPoint;
    
    /** Initial facts to be asserted at startup. */
    private Collection initialFacts;
    
    /** Is the knowledge base intended to be stateless? (default = false) */
    private boolean stateless;

    /** Are we using the knowledge base for CEP (Complex Event Processing)? (default = false) */
    private boolean cepMode;

    /** Provider-specific object holding all stateful information about the Rules session */
    private transient Object sessionData;
    
    protected transient MuleContext muleContext;

    /** Needed for exception handling. */
    private transient FlowConstruct flowConstruct;

    protected static final Log logger = LogFactory.getLog(Rules.class);

    public Rules(RulesEngine rulesEngine, String resource, Object configuration, String entryPoint, Collection initialFacts, boolean stateless, boolean cepMode, FlowConstruct flowConstruct, MuleContext muleContext)
    {
        this.rulesEngine = rulesEngine;
        this.resource = resource;
        this.configuration = configuration;
        this.entryPoint = entryPoint;
        this.initialFacts = initialFacts;
        this.stateless = stateless;
        this.cepMode = cepMode;
        this.flowConstruct = flowConstruct;
        this.muleContext = muleContext;
    }

    public void initialise() throws InitialisationException
    {
        try
        {
            sessionData = rulesEngine.createSession(this);
            
            // Insert any initial data into the knowledge base
            for (Object obj : initialFacts)
            {
                logger.debug("Adding initial data to the knowledge base: " + obj);
                rulesEngine.assertFact(this, obj);
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public void dispose()
    {
        if (sessionData != null)
        {
            try
            {
                rulesEngine.disposeSession(sessionData);
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage());
            }
            finally
            {
                sessionData = null;
            }
        }
    }

    protected Object handleEvent(MuleEvent event) throws Exception
    {
        Object payload = event.getMessage().getPayload();
        logger.debug("Adding message payload to the knowledge base: " + payload);
        if (cepMode)
        {
            return rulesEngine.assertEvent(this, payload, entryPoint);
        }
        else
        {
            return rulesEngine.assertFact(this, payload);
        }
    }

    // TODO This method should probably use the LocalMuleClient instead of re-inventing the wheel
    public MuleMessage generateMessage(String endpoint, Object payload, Map messageProperties, MessageExchangePattern exchangePattern) throws MuleException
    {
        MuleMessage message;
        if (payload instanceof MuleMessage)
        {
            message = (MuleMessage) payload;
        }
        else
        {
            message = new DefaultMuleMessage(payload, muleContext);
        }
        message.addProperties(messageProperties, PropertyScope.INBOUND);
        message.addProperties(messageProperties, PropertyScope.INVOCATION);

        //TODO should probably cache this
        EndpointBuilder endpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder(endpoint);
        endpointBuilder.setExchangePattern(exchangePattern);
        OutboundEndpoint ep = endpointBuilder.buildOutboundEndpoint();
       
        DefaultMuleEvent event = new DefaultMuleEvent(message, ep.getExchangePattern(), flowConstruct);

        RequestContext.setEvent(event);
        MuleEvent resultEvent = ep.process(event);
        
        MuleMessage response = null;
        if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent))
        {
            response = resultEvent.getMessage();
            if (response.getExceptionPayload() != null)
            {
                throw new DispatchException(MessageFactory.createStaticMessage("Unable to send or route message"), event, ep, response.getExceptionPayload().getRootException());
            }
        }        
        return response;
    }

    public String getResource()
    {
        return resource;
    }

    public Object getConfiguration()
    {
        return configuration;
    }

    public String getEntryPointLabel()
    {
        return entryPoint;
    }

    public Object getSessionData()
    {
        return sessionData;
    }

    public Collection getInitialVariables()
    {
        return initialFacts;
    }

    public boolean isStateless()
    {
        return stateless;
    }

    public boolean isCepMode()
    {
        return cepMode;
    }
}
