/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

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
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDevkitBasedMessageProcessor extends AbstractConnectedProcessor
    implements FlowConstructAware, MuleContextAware, Startable, Disposable, Stoppable, Initialisable
{

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
    public final void initialise() throws InitialisationException
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
    public final void dispose()
    {
    }

}
