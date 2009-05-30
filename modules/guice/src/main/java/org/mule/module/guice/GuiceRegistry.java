/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.registry.AbstractRegistry;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The internal Mule interface for retreiving objects from a Guice injector.  This registry is read-only.  The lifecycle of objects will be
 * managed by Mule since Guice does not provide lifecycle support.
 */
public class GuiceRegistry extends AbstractRegistry
{
    private Injector injector = null;

    public GuiceRegistry()
    {
        super("guice");
    }


    GuiceRegistry(Injector injector)
    {
        this();
        this.injector = injector;
    }

    protected void doInitialise() throws InitialisationException
    {
        //do nothing
    }

    protected void doDispose()
    {
        //nothing to do
    }

    public Object lookupObject(String key)
    {
        return null;
    }

    public <T> T lookupObject(Class<T> clazz)
    {
        return injector.getInstance(clazz);
    }

    public <T> Collection lookupObjects(Class<T> type)
    {
        try
        {
            List<T> list = new ArrayList<T>(1);
            list.add(injector.getInstance(type));
            return list;
        }
        catch (ConfigurationException e)
        {
            return Collections.EMPTY_LIST;
        }
    }

    public void registerObject(String key, Object value) throws RegistrationException
    {
        throw new UnsupportedOperationException("registerObject");
    }

    public void registerObject(String key, Object value, Object metadata) throws RegistrationException
    {
        throw new UnsupportedOperationException("registerObject");
    }

    public void registerObjects(Map objects) throws RegistrationException
    {
        throw new UnsupportedOperationException("registerObjects");
    }

    public void unregisterObject(String key) throws RegistrationException
    {
        throw new UnsupportedOperationException("unregisterObject");
    }

    public void unregisterObject(String key, Object metadata) throws RegistrationException
    {
        throw new UnsupportedOperationException("unregisterObject");
    }

    public boolean isReadOnly()
    {
        return true;
    }

    public boolean isRemote()
    {
        return false;
    }
}
