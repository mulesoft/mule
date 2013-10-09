/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.jndi;

import org.mule.api.lifecycle.Lifecycle;

import javax.naming.NamingException;

/**
 * Defines a strategy for lookup objects by name using JNDI.
 */
public interface JndiNameResolver extends Lifecycle
{

    /**
     * Looks up an object by name.
     *
     * @param name the name of the object to search for
     * @return the object if is found
     * @throws NamingException is there is an error during the lookup.
     */
    Object lookup(String name) throws NamingException;
}
