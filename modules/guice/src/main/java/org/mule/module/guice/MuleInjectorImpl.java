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

import org.mule.util.ClassUtils;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;

import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Module;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * A wrapper of the GuiceInjectorImpl that provides support for String bindings.
 */
public class MuleInjectorImpl implements Injector
{
    private Injector injector;

    private Map<String, Object> stringBindings;

    MuleInjectorImpl(Injector injector, Map<String, Object> stringBindings)
    {
        this.injector = injector;
        this.stringBindings = stringBindings;
    }

    public void injectMembers(Object o)
    {
        injector.injectMembers(o);
    }

    public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> tTypeLiteral)
    {
        return injector.getMembersInjector(tTypeLiteral);
    }

    public <T> MembersInjector<T> getMembersInjector(Class<T> tClass)
    {
        return injector.getMembersInjector(tClass);
    }

    public Map<Key<?>, Binding<?>> getBindings()
    {
        return injector.getBindings();
    }

    public <T> Binding<T> getBinding(Key<T> tKey)
    {
        return injector.getBinding(tKey);
    }

    public <T> Binding<T> getBinding(Class<T> tClass)
    {
        return injector.getBinding(tClass);
    }

    public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> tTypeLiteral)
    {
        return injector.findBindingsByType(tTypeLiteral);
    }

    public <T> Provider<T> getProvider(Key<T> tKey)
    {
        return injector.getProvider(tKey);
    }

    public <T> Provider<T> getProvider(Class<T> tClass)
    {
        return injector.getProvider(tClass);
    }

    public <T> T getInstance(Key<T> tKey)
    {
        return injector.getInstance(tKey);
    }

    public <T> T getInstance(Class<T> tClass)
    {
        return injector.getInstance(tClass);
    }

    public Injector getParent()
    {
        return injector.getParent();
    }

    public Injector createChildInjector(Iterable<? extends Module> iterable)
    {
        return injector.createChildInjector(iterable);
    }

    public Injector createChildInjector(Module... modules)
    {
        return injector.createChildInjector(modules);
    }

    //Additional methods

    public Object getInstance(String binding)
    {
        Object key = stringBindings.get(binding);
        if (key == null)
        {
            try
            {
                key = ClassUtils.loadClass(binding, getClass());
            }
            catch (ClassNotFoundException e)
            {
                return null;
                //throw new MuleRuntimeException(CoreMessages.failedToLoad(binding), e);
            }
        }
        if (key instanceof Key)
        {
            return injector.getInstance((Key) key);
        }
        else
        {
            return injector.getInstance((Class) key);
        }
    }

}
