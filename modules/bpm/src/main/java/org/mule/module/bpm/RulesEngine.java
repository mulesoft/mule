/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bpm;

import org.mule.api.NameableObject;


/**
 * A generic interface for any Rules Engine.  Theoretically, any Rules Engine can be "plugged into" 
 * Mule if it implements this interface.  
 * 
 * @see MessageService
 */
public interface RulesEngine extends NameableObject
{
    /**
     * Inject a callback so that the Rules Engine may generate messages within Mule.
     * This method is REQUIRED.
     * 
     * @param messageService An interface within Mule which the rules may call to generate Mule messages.
     */
    public void setMessageService(MessageService messageService);

    /**
     * Create a provider-specific session for interacting with the Rules Engine.
     * This method is REQUIRED.
     * 
     * @param ruleset
     * @return an initialized rules session
     */
    public Object createSession(Rules ruleset) throws Exception;
    
    /**
     * Dispose of a provider-specific session if necessary.
     * This method is OPTIONAL.
     * 
     * @param session - an initialized rules session
     */
    public void disposeSession(Object session) throws Exception;
    
    /**
     * Assert a fact in the knowledge base.
     * This method is REQUIRED.
     *
     * @param ruleset 
     * @param fact to assert
     * @return a handle to the fact for future reference
     */
    public Object assertFact(Rules ruleset, Object fact) throws Exception;

    /**
     * Retract a fact from the knowledge base.
     * This method is REQUIRED.
     *
     * @param ruleset 
     * @param fact to retract
     */
    public void retractFact(Rules ruleset, Object fact) throws Exception;

    /**
     * Add an event to the event stream.  This is used for CEP.
     * This method is OPTIONAL.
     *
     * @param ruleset 
     * @param event
     * @param entryPoint for the event stream
     * @return a handle to the event for future reference
     */
    public Object assertEvent(Rules ruleset, Object event, String entryPoint) throws Exception;
}
