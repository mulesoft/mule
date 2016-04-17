/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.session;

import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.transformer.types.TypedValue;
import org.mule.runtime.core.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.util.UUID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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

    private Map<String, TypedValue> properties;

    public DefaultMuleSession()
    {
        id = UUID.getUUID();
        properties = Collections.synchronizedMap(new CaseInsensitiveHashMap());
    }

    public DefaultMuleSession(MuleSession session)
    {
        this.id = session.getId();
        this.securityContext = session.getSecurityContext();
        this.valid = session.isValid();

        this.properties = Collections.synchronizedMap(new CaseInsensitiveHashMap());
        for (String key : session.getPropertyNamesAsSet())
        {
            this.properties.put(key, createTypedValue(session, key));
        }
    }

    private TypedValue createTypedValue(MuleSession session, String key)
    {
        return new TypedValue(session.getProperty(key), session.getPropertyDataType(key));
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
        if (!(value instanceof Serializable))
        {
            logger.warn(CoreMessages.sessionPropertyNotSerializableWarning(key));
        }

        DataType dataType = DataTypeFactory.createFromObject(value);

        properties.put(key, new TypedValue(value, dataType));
    }

    @Override
    public void setProperty(String key, Object value, DataType dataType)
    {
        if (!(value instanceof Serializable))
        {
            logger.warn(CoreMessages.sessionPropertyNotSerializableWarning(key));
        }
        properties.put(key, new TypedValue(value, dataType));
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
        Iterator<Entry<String, TypedValue>> propertyIterator = properties.entrySet().iterator();
        while (propertyIterator.hasNext())
        {
            final Entry<String, TypedValue> entry = propertyIterator.next();
            if (entry.getValue().getValue() instanceof Serializable)
            {
                propertyIterator.remove();
            }
        }
        for (String updatedPropertyKey : updatedSession.getPropertyNamesAsSet())
        {
            this.properties.put(updatedPropertyKey, createTypedValue(updatedSession, updatedPropertyKey));
        }
    }

    public Map<String, Object> getProperties()
    {
        Map<String, Object> result = new HashMap<>();
        for (String key : properties.keySet())
        {
            TypedValue typedValue = properties.get(key);
            result.put(key, typedValue.getValue());
        }

        return result;
    }

    public Map<String, TypedValue> getExtendedProperties()
    {
        return properties;
    }

    void removeNonSerializableProperties()
    {
        Iterator<Entry<String, TypedValue>> propertyIterator = properties.entrySet().iterator();
        while (propertyIterator.hasNext())
        {
            final Entry<String, TypedValue> entry = propertyIterator.next();
            if (!(entry.getValue().getValue() instanceof Serializable))
            {
                logger.warn(CoreMessages.propertyNotSerializableWasDropped(entry.getKey()));
                propertyIterator.remove();
            }
        }
    }

    @Override
    public void setProperty(String key, Serializable value)
    {
        setProperty(key, value, DataTypeFactory.createFromObject(value));
    }

    @Override
    public void setProperty(String key, Serializable value, DataType<?> dataType)
    {
        properties.put(key, new TypedValue(value, dataType));
    }

    @Override
    public <T> T getProperty(String key)
    {
        TypedValue typedValue = properties.get(key);
        return typedValue == null ? null : (T) typedValue.getValue();
    }

    @Override
    public Object removeProperty(String key)
    {
        return properties.remove(key);
    }

    // //////////////////////////
    // Serialization methods
    // //////////////////////////

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        // Temporally replaces the properties to write only serializable values into the stream
        DefaultMuleSession copy = new DefaultMuleSession(this);
        copy.removeNonSerializableProperties();
        Map<String, TypedValue> backupProperties = properties;
        try
        {
            properties = copy.properties;
            out.defaultWriteObject();
        }
        finally
        {
            properties = backupProperties;
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
    }

    @Override
    public void clearProperties()
    {
        properties.clear();
    }

    @Override
    public DataType<?> getPropertyDataType(String name)
    {
        TypedValue typedValue = properties.get(name);

        return typedValue== null ? null : typedValue.getDataType();
    }
}
