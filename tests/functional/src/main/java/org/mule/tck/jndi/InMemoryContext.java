/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Simple in-memory JNDI context for unit testing.
 */
public class InMemoryContext implements Context
{
    private Map context = new HashMap();

    public Object lookup(Name name) throws NamingException
    {
        return context.get(name);
    }

    public Object lookup(String name) throws NamingException
    {
        return context.get(name);
    }

    public void bind(Name name, Object obj) throws NamingException
    {
        context.put(name, obj);
    }

    public void bind(String name, Object obj) throws NamingException
    {
        context.put(name, obj);
    }

    public void unbind(Name name) throws NamingException
    {
        context.remove(name);
    }

    public void unbind(String name) throws NamingException
    {
        context.remove(name);
    }

    public void rebind(Name name, Object obj) throws NamingException
    {
        unbind(name);
        bind(name, obj);
    }

    public void rebind(String name, Object obj) throws NamingException
    {
        unbind(name);
        bind(name, obj);
    }

    //////////////////////////////////////////////////////////////////////
    // The remaining methods are not implemented.
    //////////////////////////////////////////////////////////////////////
    
    public Object addToEnvironment(String propName, Object propVal) throws NamingException
    {
        return null;
    }

    public void close() throws NamingException
    {
        // nop
    }

    public Name composeName(Name name, Name prefix) throws NamingException
    {
        return null;
    }

    public String composeName(String name, String prefix) throws NamingException
    {
        return null;
    }

    public Context createSubcontext(Name name) throws NamingException
    {
        return null;
    }

    public Context createSubcontext(String name) throws NamingException
    {
        return null;
    }

    public void destroySubcontext(Name name) throws NamingException
    {
        // nop
    }

    public void destroySubcontext(String name) throws NamingException
    {
        // nop
    }

    public Hashtable getEnvironment() throws NamingException
    {
        return null;
    }

    public String getNameInNamespace() throws NamingException
    {
        return null;
    }

    public NameParser getNameParser(Name name) throws NamingException
    {
        return null;
    }

    public NameParser getNameParser(String name) throws NamingException
    {
        return null;
    }

    public NamingEnumeration list(Name name) throws NamingException
    {
        return null;
    }

    public NamingEnumeration list(String name) throws NamingException
    {
        return null;
    }

    public NamingEnumeration listBindings(Name name) throws NamingException
    {
        return null;
    }

    public NamingEnumeration listBindings(String name) throws NamingException
    {
        return null;
    }

    public Object lookupLink(Name name) throws NamingException
    {
        return null;
    }

    public Object lookupLink(String name) throws NamingException
    {
        return null;
    }

    public Object removeFromEnvironment(String propName) throws NamingException
    {
        return null;
    }

    public void rename(Name oldName, Name newName) throws NamingException
    {
        // nop
    }

    public void rename(String oldName, String newName) throws NamingException
    {
        // nop
    }
}


