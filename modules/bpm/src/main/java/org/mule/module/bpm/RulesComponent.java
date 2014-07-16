/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bpm;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.component.AbstractComponent;
import org.mule.config.i18n.MessageFactory;

import java.util.Collection;

/**
 * A service backed by a Business Rules engine such as Drools. 
 */
public class RulesComponent extends AbstractComponent
{
    /** The ruleset */
    protected Rules rules;
    
    /** The underlying Rules Engine */
    protected RulesEngine rulesEngine;

    /** The resource containing the rules definition. */
    private String resource;

    /** Initial facts to be asserted at startup. */
    private Collection initialFacts;

    /** Entry point for event stream (used by CEP) */
    private String entryPoint;
    
    /** Is the knowledge base intended to be stateless? (default = false) */
    private boolean stateless = false;

    /** Are we using the knowledge base for CEP (Complex Event Processing)? (default = false) */
    private boolean cepMode = false;

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (rulesEngine == null)
        {
            try
            {
                rulesEngine = muleContext.getRegistry().lookupObject(RulesEngine.class);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
        if (rulesEngine == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("The rulesEngine property must be set for this component."), this);
        }
        if (rulesEngine instanceof Initialisable)
        {
            ((Initialisable) rulesEngine).initialise();
        }
        
        rules = new Rules(rulesEngine, resource, null, entryPoint, initialFacts, stateless, cepMode, flowConstruct, muleContext);
        // Inject a callback so that the Rules Engine may generate messages within Mule.
        rulesEngine.setMessageService(rules);        
        rules.initialise();

        super.doInitialise();
    }

    /**
     * @return a handle to the fact for future reference
     */
    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        return rules.handleEvent(event);
    }

    public void setResource(String resource)
    {
        this.resource = resource;
    }

    public String getResource()
    {
        return resource;
    }    
    
    public Collection getInitialFacts()
    {
        return initialFacts;
    }

    public void setInitialFacts(Collection initialFacts)
    {
        this.initialFacts = initialFacts;
    }
    
    public String getEntryPoint()
    {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint)
    {
        this.entryPoint = entryPoint;
    }

    public void setStateless(boolean stateless)
    {
        this.stateless = stateless;
    }

    public boolean isStateless()
    {
        return stateless;
    }

    public boolean isCepMode()
    {
        return cepMode;
    }

    public void setCepMode(boolean cepMode)
    {
        this.cepMode = cepMode;
    }
}


