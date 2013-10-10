/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.security.SecurityContext;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>MuleSession</code> is the context in which a request is executed. The
 * session manages the marshalling of events to and from components This object is
 * not usually referenced by client code directly. If needed Components should manage
 * events via the <code>MuleEventContext</code> which is obtainable via the
 * <code>UMOManager</code> or by implementing
 * <code>org.mule.api.lifecycle.Callable</code>.
 */

public interface MuleSession extends Serializable
{
    /**
     * Returns the Service associated with the session in its current execution
     * 
     * @return the Service associated with the session in its current execution
     * @see FlowConstruct
     */
    FlowConstruct getFlowConstruct();

    /**
     * Sets the Service associated with the session in its current execution
     * 
     * @see FlowConstruct
     */
    void setFlowConstruct(FlowConstruct flowConstruct);

    /**
     * Determines if this session is valid. A session becomes invalid if an exception
     * occurs while processing
     * 
     * @return true if the service is functioning properly, false otherwise
     */
    boolean isValid();

    /**
     * Determines if this session is valid. A session becomes invalid if an exception
     * occurs while processing
     * 
     * @param value true if the service is functioning properly, false otherwise
     */
    void setValid(boolean value);

    /**
     * Returns the unique id for this session
     * 
     * @return the unique id for this session
     */
    String getId();

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @param context the context for this session or null if the request is not
     *            secure.
     */
    void setSecurityContext(SecurityContext context);

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @return the context for this session or null if the request is not secure.
     */
    SecurityContext getSecurityContext();

    /**
     * Will set a session level property. These will either be stored and retrieved
     * using the underlying transport mechanism of stored using a default mechanism
     * 
     * @param key the key for the object data being stored on the session
     * @param value the value of the session data
     */
    void setProperty(String key, Object value);

    /**
     * Will retrieve a session level property.
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    <T> T getProperty(Object key);

    /**
     * Will retrieve a session level property and remove it from the session
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    Object removeProperty(Object key);

    /**
     * Returns an iterater of property keys for the session properties on this
     * session
     * 
     * @return an iterater of property keys for the session properties on this
     *         session
     * @deprecated Use getPropertyNamesAsSet() instead
     */
    Iterator getPropertyNames();

    /**
     * @return property keys for all session properties
     */
    Set<String> getPropertyNamesAsSet();

    /**
     * Merge current session with an updated version
     * Result session will contain all the properties from updatedSession
     * plus those properties in the current session that couldn't be serialized
     * In case updatedSession is null, then no change will be applied.
     *
     * @param updatedSession mule session with updated properties
     */
    void merge(MuleSession updatedSession);
}
