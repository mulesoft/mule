/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.session;

import org.mule.api.MuleSession;
import org.mule.api.security.SecurityContext;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.UUID;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleSession</code> manages the interaction and distribution of events for Mule Services.
 */

public final class DefaultMuleSession implements MuleSession
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3380926585676521866L;

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(DefaultMuleSession.class);

    /**
     * Determines if the service is valid
     */
    private boolean valid = true;

    private String id;

    /**
     * The security context associated with the session. Note that this context will only be serialized if the
     * SecurityContext object is Serializable.
     */
    private SecurityContext securityContext;

    private Map<String, Object> properties = null;

    public DefaultMuleSession()
    {
        id = UUID.getUUID();
        properties = Collections.synchronizedMap(new CaseInsensitiveHashMap/* <String, Object> */());
    }

    public DefaultMuleSession(MuleSession session)
    {
        this.id = session.getId();
        this.securityContext = session.getSecurityContext();
        this.valid = session.isValid();

        this.properties = Collections.synchronizedMap(new CaseInsensitiveHashMap/* <String, Object> */());
        for (String key : session.getPropertyNamesAsSet())
        {
            this.properties.put(key, session.getProperty(key));
        }
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public boolean isValid()
    {
        return valid;
    }

    @Override
    public void setValid(boolean value)
    {
        valid = value;
    }

    /**
     * The security context for this session. If not null outbound, inbound and/or method invocations will be
     * authenticated using this context
     * 
     * @param context the context for this session or null if the request is not secure.
     */
    @Override
    public void setSecurityContext(SecurityContext context)
    {
        securityContext = context;
    }

    /**
     * The security context for this session. If not null outbound, inbound and/or method invocations will be
     * authenticated using this context
     * 
     * @return the context for this session or null if the request is not secure.
     */
    @Override
    public SecurityContext getSecurityContext()
    {
        return securityContext;
    }

    /**
     * Will set a session level property. These will either be stored and retrieved using the underlying
     * transport mechanism of stored using a default mechanism
     * 
     * @param key the key for the object data being stored on the session
     * @param value the value of the session data
     */
    @Override
    public void setProperty(String key, Object value)
    {
        properties.put(key, value);
    }

    /**
     * Will retrieve a session level property.
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(Object key)
    {
        return (T) properties.get(key);
    }

    /**
     * Will retrieve a session level property and remove it from the session
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    @Override
    public Object removeProperty(Object key)
    {
        return properties.remove(key);
    }

    @Override
    public Set<String> getPropertyNamesAsSet()
    {
        return Collections.unmodifiableSet(properties.keySet());
    }

    public void merge(MuleSession updatedSession)
    {
        if (updatedSession == null)
        {
            return;
        }
        Map<String, Object> oldProperties = this.properties;
        this.properties = Collections.synchronizedMap(new CaseInsensitiveHashMap/* <String, Object> */());
        for (String propertyKey : updatedSession.getPropertyNamesAsSet())
        {
            this.properties.put(propertyKey, updatedSession.<Object> getProperty(propertyKey));
        }
        for (String propertyKey : oldProperties.keySet())
        {
            if (!this.properties.containsKey(propertyKey)
                && !(oldProperties.get(propertyKey) instanceof Serializable))
            {
                this.properties.put(propertyKey, oldProperties.get(propertyKey));
            }
        }
    }

    @Override
    public Map<String, Object> getProperties()
    {
        return properties;
    }

}
