/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

public class SimpleContext implements Context
{
    /** What holds the bindings. */
    protected Map<String, Object> bindings = new HashMap<String, Object>();

    /** Context's environment. */
    private Hashtable<String, Object> environment;

    public SimpleContext()
    {
        super();
    }

    @Override
    public Object lookupLink(Name name) throws NamingException
    {
        // unsupported
        return null;
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException
    {
        // unsupported
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException
    {
        // unsupported
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
    {
        // unsupported
        return null;
    }

    @Override
    public Object lookup(Name name) throws NamingException
    {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException
    {
        Object rc = bindings.get(name);
        if (rc == null)
        {
            throw new NameNotFoundException(name);
        }
        return rc;
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException
    {
        bind(name.toString(), obj);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException
    {
        if (bindings.containsKey(name))
        {
            throw new NameAlreadyBoundException(name);
        }
        bindings.put(name, obj);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException
    {
        rebind(name.toString(), obj);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException
    {
        bindings.put(name, obj);
    }

    @Override
    public void unbind(Name name) throws NamingException
    {
        unbind(name.toString());
    }

    @Override
    public void unbind(String name) throws NamingException
    {
        if (bindings.remove(name) == null)
        {
            throw new NameNotFoundException(name);
        }
    }

    public void rename(Attributes.Name oldName, Attributes.Name newName) throws NamingException
    {
        rename(oldName.toString(), newName.toString());
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException
    {
        if (!bindings.containsKey(oldName))
        {
            throw new NameNotFoundException(oldName);
        }
        if (bindings.containsKey(newName))
        {
            throw new NameAlreadyBoundException(newName);
        }

        bindings.put(newName, bindings.remove(oldName));
    }

    public NamingEnumeration<NameClassPair> list(Attributes.Name name) throws NamingException
    {
        return list(name.toString());
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException
    {
        if (name.length() > 0)
        {
            throw new OperationNotSupportedException("subcontexts not supported");
        }

        final Iterator<Map.Entry<String, Object>> i = bindings.entrySet().iterator();
        return new NamingEnumeration<NameClassPair>()
        {
            @Override
            public NameClassPair next()
            {
                Map.Entry<String, Object> e = i.next();
                return new NameClassPair(e.getKey(), e.getValue().getClass().getName());
            }

            @Override
            public boolean hasMore()
            {
                return i.hasNext();
            }

            @Override
            public void close()
            {
                // noop
            }

            @Override
            public boolean hasMoreElements()
            {
                return hasMore();
            }

            @Override
            public NameClassPair nextElement()
            {
                return next();
            }
        };
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
    {
        return listBindings(name.toString());
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException
    {
        if (name.length() > 0)
        {
            throw new OperationNotSupportedException("subcontexts not supported");
        }

        final Iterator<Map.Entry<String, Object>> i = bindings.entrySet().iterator();
        return new NamingEnumeration<Binding>()
        {
            @Override
            public Binding next()
            {
                Map.Entry<String, Object> e = i.next();
                return new Binding(e.getKey(), e.getValue());
            }

            @Override
            public boolean hasMore()
            {
                return i.hasNext();
            }

            @Override
            public void close()
            {
                // noop
            }

            @Override
            public boolean hasMoreElements()
            {
                return hasMore();
            }

            @Override
            public Binding nextElement()
            {
                return next();
            }
        };
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException
    {
        destroySubcontext(name.toString());
    }

    @Override
    public void destroySubcontext(String name) throws NamingException
    {
        throw new OperationNotSupportedException("subcontexts not supported");
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException
    {
        return createSubcontext(name.toString());
    }

    @Override
    public Context createSubcontext(String name) throws NamingException
    {
        throw new OperationNotSupportedException("subcontexts not supported");
    }

    public Object lookupLink(Attributes.Name name) throws NamingException
    {
        return lookupLink(name.toString());
    }

    @Override
    public Object lookupLink(String name) throws NamingException
    {
        return lookup(name);
    }

    public NameParser getNameParser(Attributes.Name name)
    {
        return getNameParser(name.toString());
    }

    @Override
    public NameParser getNameParser(String name)
    {
        throw new UnsupportedOperationException("getNameParser");
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException
    {
        return composeName(new CompositeName(name), new CompositeName(prefix)).toString();
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException
    {
        Name result = (Name) prefix.clone();
        result.addAll(name);
        return result;
    }

    @Override
    public Object addToEnvironment(String key, Object val)
    {
        initEnvironment();
        return environment.put(key, val);
    }

    @Override
    public Object removeFromEnvironment(String key)
    {
        initEnvironment();
        return environment.remove(key);
    }

    @Override
    public Hashtable<String, Object> getEnvironment()
    {
        initEnvironment();
        return environment;
    }

    private void initEnvironment()
    {
        if (environment == null)
        {
            environment = new Hashtable<String, Object>();
        }
    }

    @Override
    public void close()
    {
        // noop
    }

    @Override
    public String getNameInNamespace()
    {
        return "";
    }
}
