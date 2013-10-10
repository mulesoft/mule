/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * Simple in-memory JNDI context for unit testing.
 */
public class InMemoryContextFactory implements InitialContextFactory
{
    public Context getInitialContext() throws NamingException
    {
        return getInitialContext(null);
    }

    public Context getInitialContext(Hashtable environment) throws NamingException
    {
        return new InMemoryContext();
    }
}


