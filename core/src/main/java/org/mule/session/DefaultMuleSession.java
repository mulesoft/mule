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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.security.SecurityContext;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.UUID;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleSession</code> manages the interaction and distribution of events for Mule Services.
 */

public final class DefaultMuleSession implements MuleSession, DeserializationPostInitialisable
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
     * The Mule service associated with the session
     * <p/>
     * Note: This object uses custom serialization via the writeObject()/readObject() methods.
     */
    private transient FlowConstruct flowConstruct = null;

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

    private transient Map<String, Object> serializedData = null;

    public DefaultMuleSession()
    {
        id = UUID.getUUID();
        properties = Collections.synchronizedMap(new CaseInsensitiveHashMap/* <String, Object> */());
    }

    @Deprecated
    public DefaultMuleSession(MuleContext context)
    {
        this();
    }

    public DefaultMuleSession(FlowConstruct flowConstruct)
    {
        this();
        this.flowConstruct = flowConstruct;
    }

    @Deprecated
    public DefaultMuleSession(FlowConstruct flowConstruct, MuleContext muleContext)
    {
        this(flowConstruct);
    }

    public DefaultMuleSession(MuleSession session)
    {
        this.id = session.getId();
        this.securityContext = session.getSecurityContext();
        this.flowConstruct = session.getFlowConstruct();
        this.valid = session.isValid();

        this.properties = Collections.synchronizedMap(new CaseInsensitiveHashMap/* <String, Object> */());
        for (String key : session.getPropertyNamesAsSet())
        {
            this.properties.put(key, session.getProperty(key));
        }
    }

    @Deprecated
    public DefaultMuleSession(MuleSession session, MuleContext muleContext)
    {
        this(session);
    }

    /**
     * Copy the session, changing only the flow construct. This can be used for synchronous calls from one
     * flow construct to another.
     */
    public DefaultMuleSession(MuleSession source, FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
        DefaultMuleSession session = (DefaultMuleSession) source;
        this.id = session.id;
        this.properties = session.properties;
        this.securityContext = session.securityContext;
        this.valid = session.valid;
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
     * @return Returns the service.
     */
    @Override
    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
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

    // //////////////////////////
    // Serialization methods
    // //////////////////////////

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        // Can be null if service call originates from MuleClient
        if (serializedData != null)
        {
            Object serviceName = serializedData.get("serviceName");
            if (serviceName != null)
            {
                out.writeObject(serviceName);
            }
        }
        else
        {
            if (getFlowConstruct() != null)
            {
                out.writeObject(getFlowConstruct() != null ? getFlowConstruct().getName() : "null");
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        serializedData = new HashMap<String, Object>();

        try
        {
            // Optional
            serializedData.put("serviceName", in.readObject());
        }
        catch (OptionalDataException e)
        {
            // ignore
        }
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.util.store.DeserializationPostInitialisable} is used. This will get invoked after the
     * object has been deserialized passing in the current mulecontext when using either
     * {@link org.mule.transformer.wire.SerializationWireFormat},
     * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat}, or the
     * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
     * 
     * @param context the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    public void initAfterDeserialisation(MuleContext context) throws MuleException
    {
        // this method can be called even on objects that were not serialized. In this case,
        // the temporary holder for serialized data is not initialized and we can just return
        if (serializedData == null)
        {
            return;
        }

        String serviceName = (String) serializedData.get("serviceName");
        // Can be null if service call originates from MuleClient
        if (serviceName != null)
        {
            flowConstruct = context.getRegistry().lookupFlowConstruct(serviceName);
        }
        serializedData = null;
    }
}
