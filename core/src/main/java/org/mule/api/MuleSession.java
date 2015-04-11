/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.security.SecurityContext;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.SessionHandler;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>MuleSession</code> is the context in which a request is processed by Mule. The scope of the
 * MuleSession context includes all Mule Flows and Services that the request is routed through on the same or
 * different Mule instances. A MuleSession instance has a unique id, session scope properties and an optional
 * security context.
 * <p>
 * In order for the session to be propagated from one Flow or Service to the next a transports that support
 * message properties needs to be used. A {@link SessionHandler} is used to store the session in an outbound
 * message property and then retrieve it from an inbound property using a specific strategy.
 *
 * @see SessionHandler
 */
public interface MuleSession extends Serializable
{

    /**
     * Determines if this session is valid. A session becomes invalid if an exception occurs while processing
     * 
     * @return true if the session is valid, false otherwise
     */
    boolean isValid();

    /**
     * Determines if this session is valid. A session becomes invalid if an exception occurs while processing
     * 
     * @param value true if the session is valid, false otherwise
     */
    void setValid(boolean value);

    /**
     * Returns the unique id for this session
     * 
     * @return the unique id for this session
     */
    String getId();

    /**
     * The security context for this session. If not null outbound, inbound and/or method invocations will be
     * authenticated using this context
     * 
     * @param context the context for this session or null if the request is not secure.
     */
    void setSecurityContext(SecurityContext context);

    /**
     * The security context for this session. If not null outbound, inbound and/or method invocations will be
     * authenticated using this context
     * 
     * @return the context for this session or null if the request is not secure.
     */
    SecurityContext getSecurityContext();

    /**
     * Will set a session scope property.
     * 
     * @param key the key for the object data being stored on the session
     * @param value the value of the session data
     */
    void setProperty(String key, Serializable value);

    /**
     * Will set a session scope property.
     *
     * @param key the key for the object data being stored on the session
     * @param value the value of the session data
     * @param dataType the data type for the property value
     */
    void setProperty(String key, Serializable value, DataType<?> dataType);
    
    @Deprecated
    void setProperty(String key, Object value);
    
    /**
     * Will retrieve a session scope property.
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    <T> T getProperty(String key);

    @Deprecated
    <T> T getProperty(Object key);

    /**
     * Will retrieve a session scope property and remove it from the session
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    Object removeProperty(String key);

    @Deprecated
    Object removeProperty(Object key);
    
    /**
     * Returns an iterater of property keys for the session properties on this
     * session
     * 
     * @return an iterater of property keys for the session properties on this
     *         session
     * @deprecated Use getPropertyNamesAsSet() instead  (Will be removed in 4.0)
     */
    Iterator getPropertyNames();

    /**
     * @return property keys for all session properties
     */
    Set<String> getPropertyNamesAsSet();

    /**
     * Merge current session with an updated version Result session will contain all the properties from
     * updatedSession plus those properties in the current session that couldn't be serialized In case
     * updatedSession is null, then no change will be applied.
     * 
     * @param updatedSession mule session with updated properties
     */
    void merge(MuleSession updatedSession);
    
    void clearProperties();
    
    /**
     * WARNING: This method will always return null unless you created the DefaultMuleSession with a
     * flowConstruct or set one using the setter. This method should not be used, and is only here for
     * backwards compatibility
     */
    @Deprecated
    FlowConstruct getFlowConstruct();

    /**
     * WARNING: This method should not be used, and is only here for backwards compatibility
     */
    @Deprecated
    void setFlowConstruct(FlowConstruct flowConstruct);

    /**
     * Retrieves a session scope property data type
     *
     * @param name the name for the session property
     * @return the property data type or null if the property does not exist
     */
    DataType<?> getPropertyDataType(String name);
}
