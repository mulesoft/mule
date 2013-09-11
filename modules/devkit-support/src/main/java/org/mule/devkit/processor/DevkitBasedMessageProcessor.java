/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.devkit.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.Transformer;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.security.oauth.OnNoTokenPolicy;
import org.mule.security.oauth.OnNoTokenPolicyAware;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.List;

public abstract class DevkitBasedMessageProcessor extends ExpressionEvaluatorSupport
    implements FlowConstructAware, MuleContextAware, Startable, Disposable, Stoppable, Initialisable
{

    private String operationName;

    /**
     * Only used on OAuth enabled connectors
     */
    private String accessTokenId;

    /**
     * Module object
     */
    protected Object moduleObject;
    /**
     * Mule Context
     */
    protected MuleContext muleContext;

    /**
     * Flow Construct
     */
    protected FlowConstruct flowConstruct;

    public DevkitBasedMessageProcessor(String operationName)
    {
        this.operationName = operationName;
    }

    /**
     * This final process implementation shields actual processing into abstract
     * method {@link
     * org.mule.security.oauth.processor.AbstractDevkitBasedMessageProcessor.
     * doProcess(MuleEvent)} Afterwards, it performs payload transformation by
     * invoking {@link
     * org.mule.security.oauth.processor.AbstractDevkitBasedMessageProcessor.
     * overwritePayload(MuleEvent, Object)} In case the processing throws
     * {@link org.mule.common.security.oauth.exception.NotAuthorizedException}, the
     * module object is casted to {@link org.mule.security.oauth.OAuthAdapter} and
     * exception handling is delegated into its
     * {@link org.mule.security.oauth.OnNoTokenPolicy}. For any other kind of
     * exception, it is logged and wrapped into a {@link org.mule.api.MuleException}
     * 
     * @param event the current mule event.
     * @return the mule event returned by the message processor or the
     *         OnNoTokenPolicy in case of NotAuthorizedException
     * @throws MuleException
     */
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            return this.doProcess(event);
        }
        catch (MessagingException messagingException)
        {
            messagingException.setProcessedEvent(event);
            throw messagingException;
        }
        catch (NotAuthorizedException e)
        {
            try
            {
                return this.handleNotAuthorized((OnNoTokenPolicyAware) moduleObject, e, event);
            }
            catch (Exception ne)
            {
                this.handleException(event, ne);
            }
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            this.handleException(event, e);
        }

        return event;
    }

    /**
     * Implementors of this class need to implement this method in order to perform
     * actual processing
     * 
     * @param event the mule event
     * @return a mule event
     * @throws Exception
     */
    protected abstract MuleEvent doProcess(MuleEvent event) throws Exception;

    private void handleException(MuleEvent event, Throwable e) throws MuleException
    {
        throw new MessagingException(CoreMessages.failedToInvoke(this.operationName), event, e);
    }

    private MuleEvent handleNotAuthorized(OnNoTokenPolicyAware policyAware,
                                          NotAuthorizedException e,
                                          MuleEvent event) throws NotAuthorizedException
    {
        OnNoTokenPolicy policy = policyAware.getOnNoTokenPolicy();
        if (policy == null)
        {
            throw new IllegalStateException("OnNoTokenPolicy cannot be null");
        }

        return policy.handleNotAuthorized(policyAware, e, event);
    }

    /**
     * Obtains the expression manager from the Mule context and initialises the
     * connector. If a target object has not been set already it will search the Mule
     * registry for a default one.
     * 
     * @throws InstantiationException
     * @throws ConfigurationException
     * @throws IllegalAccessException
     * @throws RegistrationException
     */
    protected final Object findOrCreate(Class<?> moduleClass, boolean shouldAutoCreate, MuleEvent muleEvent)
        throws IllegalAccessException, InstantiationException, ConfigurationException, RegistrationException
    {
        Object temporaryObject = moduleObject;
        if (temporaryObject == null)
        {
            temporaryObject = (muleContext.getRegistry().lookupObject(moduleClass));
            if (temporaryObject == null)
            {
                if (shouldAutoCreate)
                {
                    temporaryObject = (moduleClass.newInstance());
                    muleContext.getRegistry().registerObject(moduleClass.getName(), temporaryObject);
                }
                else
                {
                    throw new ConfigurationException(MessageFactory.createStaticMessage("Cannot find object"));
                }
            }
        }
        if (temporaryObject instanceof String)
        {
            temporaryObject = (muleContext.getExpressionManager().evaluate(((String) temporaryObject),
                muleEvent, true));
            if (temporaryObject == null)
            {
                throw new ConfigurationException(
                    MessageFactory.createStaticMessage("Cannot find object by config name"));
            }
        }
        return temporaryObject;
    }

    /**
     * Overwrites the event payload with the specified one
     */
    public final void overwritePayload(MuleEvent event, Object resultPayload) throws Exception
    {
        TransformerTemplate.OverwitePayloadCallback overwritePayloadCallback = null;
        if (resultPayload == null)
        {
            overwritePayloadCallback = new TransformerTemplate.OverwitePayloadCallback(
                NullPayload.getInstance());
        }
        else
        {
            overwritePayloadCallback = new TransformerTemplate.OverwitePayloadCallback(resultPayload);
        }
        List<Transformer> transformerList;
        transformerList = new ArrayList<Transformer>();
        transformerList.add(new TransformerTemplate(overwritePayloadCallback));
        event.getMessage().applyTransformers(event, transformerList);
    }

    /**
     * Obtains the expression manager from the Mule context and initialises the
     * connector. If a target object has not been set already it will search the Mule
     * registry for a default one.
     * 
     * @throws InitialisationException
     */
    @Override
    public void initialise() throws InitialisationException
    {
    }

    @Override
    public void start() throws MuleException
    {
    }

    @Override
    public void stop() throws MuleException
    {
    }

    @Override
    public void dispose()
    {
    }

    /**
     * Sets muleContext
     * 
     * @param value Value to set
     */
    @Override
    public final void setMuleContext(MuleContext value)
    {
        this.muleContext = value;
    }

    /**
     * Retrieves muleContext
     */
    public final MuleContext getMuleContext()
    {
        return this.muleContext;
    }

    /**
     * Sets flowConstruct
     * 
     * @param value Value to set
     */
    @Override
    public final void setFlowConstruct(FlowConstruct value)
    {
        this.flowConstruct = value;
    }

    /**
     * Retrieves flowConstruct
     */
    public final FlowConstruct getFlowConstruct()
    {
        return this.flowConstruct;
    }

    /**
     * Sets moduleObject
     * 
     * @param value Value to set
     */
    public final void setModuleObject(Object value)
    {
        this.moduleObject = value;
    }

    public String getAccessTokenId()
    {
        return accessTokenId;
    }

    public void setAccessTokenId(String accessTokenId)
    {
        this.accessTokenId = accessTokenId;
    }

}
