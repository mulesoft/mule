/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.extensions;

import org.mule.api.MuleSession;

import java.util.Enumeration;

import org.apache.commons.collections.iterators.IteratorEnumeration;

/**
 * Provides an adapter to a DefaultMuleSession so that Axis can write to the session
 */
public class AxisMuleSession implements org.apache.axis.session.Session
{

    private MuleSession session;
    private Object lock = new Object();

    public AxisMuleSession(MuleSession session)
    {
        this.session = session;
    }

    public Object get(String string)
    {
        synchronized(lock)
        {
            return session.getProperty(string);
        }
    }

    public void set(String string, Object object)
    {
        synchronized(lock)
        {
            session.setProperty(string, object);
        }
    }

    public void remove(String string)
    {
        synchronized(lock)
        {
            session.removeProperty(string);
        }
    }

    public Enumeration getKeys()
    {
        synchronized(lock)
        {
            return new IteratorEnumeration(session.getPropertyNamesAsSet().iterator());
        }
    }

    public void setTimeout(int i)
    {
         // TODO not supported
    }

    public int getTimeout()
    {
        return 0;
    }

    public void touch()
    {
        // nothing here to touch
    }

    public void invalidate()
    {
        synchronized(lock)
        {
            session.setValid(false);
        }
    }

    public Object getLockObject()
    {
        return lock;
    }
}
