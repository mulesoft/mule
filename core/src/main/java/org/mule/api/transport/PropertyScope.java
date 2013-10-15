/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A PropertyScope is used to associate a message property with a lifetime.  A scope may be very 
 * brief such as {@link #INVOCATION} which only lasts until a service has been invoked or longer 
 * running such as {@link #SESSION}.
 */
public final class PropertyScope implements Serializable
{
    private static final long serialVersionUID = -4792653762048974018L;
    
    public static final String INVOCATION_NAME = "invocation";
    public static final String INBOUND_NAME = "inbound";
    public static final String OUTBOUND_NAME = "outbound";
    public static final String SESSION_NAME = "session";
    public static final String APPLICATION_NAME = "application";

    /**
     * This scope is defined from the point that a Message is created until a service has processed the
     * message. Properties set on endpoints will be found in this scope
     */
    public static final PropertyScope INVOCATION = new PropertyScope(INVOCATION_NAME, 0);

    /**
     * This scope holds all inbound headers when a message is received. This scope is read only
     */
    public static final PropertyScope INBOUND = new PropertyScope(INBOUND_NAME, 1);

    /**
     * This is the default scope when writing properties to a message. All properties written in this scope
     * will be attached to the outbound message (or response message)
     */
    public static final PropertyScope OUTBOUND = new PropertyScope(OUTBOUND_NAME, 2);

    /**
     * Defines the scope for any properties set on the session. Mule utilises the underlying transport for controlling the
     * session where possible i.e. HttpSession. But Mule will fallback to an internal session mechanism where a session is
     * encoded on the message with an expiry time associated with it.
     */
    public static final PropertyScope SESSION = new PropertyScope(SESSION_NAME, 3);

    /**
     * This provides access to properties in the registry. By default this scope is not enabled since
     * it will most likely have a performance impact. This is a read-only scope
     */
    public static final PropertyScope APPLICATION = new PropertyScope(APPLICATION_NAME, 4);

    /**
     * An array of all scopes defined here
     */
    public static final PropertyScope[] ALL_SCOPES = new PropertyScope[]{INVOCATION, INBOUND, OUTBOUND, SESSION, APPLICATION};

    private String scope;
    private int order;

    private PropertyScope(String scope, int order)
    {
        this.scope = scope;
        this.order = order;
    }

    public static PropertyScope get(String name)
    {
        if (INVOCATION.getScopeName().equals(name))
        {
            return INVOCATION;
        }
        else if (INBOUND.getScopeName().equals(name))
        {
            return INBOUND;
        }
        else if (OUTBOUND.getScopeName().equals(name))
        {
            return OUTBOUND;
        }
        else if (SESSION.getScopeName().equals(name))
        {
            return SESSION;
        }
        else if (APPLICATION.getScopeName().equals(name))
        {
            return APPLICATION;
        }
        else
        {
            return null;
        }
    }
    
    public String getScopeName()
    {
        return scope;
    }

    public int getOrder()
    {
        return order;
    }

    @Override
    public String toString()
    {
        return getScopeName();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        PropertyScope that = (PropertyScope) o;

        if (order != that.order)
        {
            return false;
        }
        if (scope != null ? !scope.equals(that.scope) : that.scope != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result;
        result = (scope != null ? scope.hashCode() : 0);
        result = 31 * result + order;
        return result;
    }

    /**
     * Used for comparing {@link PropertyScope} instances in a map. The {@link PropertyScope#getOrder()}
     * property is used to determine the order in the map
     */
    public static class ScopeComparator implements Comparator<PropertyScope>, Serializable
    {
        private static final long serialVersionUID = -3346258000312580166L;

        public int compare(PropertyScope o, PropertyScope o1)
        {
            if (o == o1)
            {
                return 0;
            }
            if (o.equals(o1))
            {
                return 0;
            }
            return (o.getOrder() < o1.getOrder() ? -1 : 1);
        }
    }
}

