/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.registry.AbstractRegistry;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.guiceyfruit.Injectors;
import org.guiceyfruit.support.CloseFailedException;

/**
 * The internal Mule interface for retreiving objects from a Guice injector.  This registry is read-only since all
 * objects should be configured by {@link com.google.inject.Module} objects.  The lifecycle of objects will be
 * managed by Mule since Guice does not provide lifecycle support.
 * <p/>
 * To create modules extend the {@link org.mule.module.guice.AbstractMuleGuiceModule} since it provides hooks and helpers for
 * working with Mule configuration.  Any modules independent of Mule can just extend the Guice {@link com.google.inject.AbstractModule}
 * as normal.
 * <p/>
 * Mule will discover modules on the classpath, if you need to configure a module before passing it to the Guice injector you
 * need to implement a {@link org.mule.module.guice.GuiceModuleFactory} for your module.
 *
 * @see org.mule.module.guice.AbstractMuleGuiceModule
 * @see org.mule.module.guice.GuiceModuleFactory
 * @deprecated Guice module is deprecated and will be removed in Mule 4.
 */
@Deprecated
public class GuiceRegistry extends AbstractRegistry
{
    private Injector injector = null;

    public GuiceRegistry(MuleContext muleContext)
    {
        super("guice", muleContext);
    }


    GuiceRegistry(Injector injector, MuleContext muleContext)
    {
        this(muleContext);
        this.injector = injector;
    }

    protected void doInitialise() throws InitialisationException
    {
        //do nothing
    }

    protected void doDispose()
    {
        try
        {
            Injectors.close(injector);
        }
        catch (CloseFailedException e)
        {
            logger.error("Failed to close the Guice registry cleanly", e);
        }
    }

    public <T> T lookupObject(String key)
    {
        //Guice isn't really supposed to work this way but in Mule we need to look up objects by name only sometimes
        for (Map.Entry<Key<?>, Binding<?>> entry : injector.getBindings().entrySet())
        {
            if (entry.getKey().getAnnotation() instanceof Named)
            {
                String name = ((Named) entry.getKey().getAnnotation()).value();
                if (name.equals(key))
                {
                    Object o = entry.getValue().getProvider().get();
                    return (T) o;
                }
            }
        }
        return null;
    }

    @Override
    public <T> T lookupObject(Class<T> type) throws RegistrationException
    {
        //We have to loop through all objects since Guice will act as a factory and create a binding for the given class
        //if one is not bound in the registry.
        List<Binding<T>> bindings = injector.findBindingsByType(TypeLiteral.get(type));
        if(bindings.size()==0)
        {
            return null;

        }
        else if (bindings.size()==1)
        {
            return bindings.get(0).getProvider().get();
        }
        else
        {
            throw new RegistrationException(MessageFactory.createStaticMessage("More than one object of type: " + type + ", was found"));
        }
    }

    public <T> Map<String, T> lookupByType(Class<T> type)
    {


        return Collections.EMPTY_MAP;
//        try
//        {
//            List<Binding<T>> bindings = injector.findBindingsByType(TypeLiteral.get(type));
//            if(bindings!=null && bindings.size()>0)
//            {
//                Map<String, T> map = new HashMap<String, T>(bindings.size());
//                String name;
//                T object;
//                for (Binding<T> binding : bindings)
//                {
//                    object = binding.getProvider().get();
//
//                    if(binding.getKey().getAnnotation() instanceof Named)
//                    {
//                        name = ((Named)binding.getKey().getAnnotation()).value();
//                    }
//                    else if(object instanceof NameableObject)
//                    {
//                        name = ((NameableObject)object).getName();
//                    }
//                    else
//                    {
//                        name = "_" + object.hashCode();
//                    }
//                    map.put(name, object);
//                }
//                return map;
//            }
//            return Collections.emptyMap();
//        }
//        catch (ConfigurationException e)
//        {
//            return Collections.emptyMap();
//        }
    }

    public <T> Collection<T> lookupObjects(Class<T> type)
    {
        return Collections.emptyList();

//        try
//        {
//            List<Binding<T>> bindings = injector.findBindingsByType(TypeLiteral.get(type));
//            if(bindings!=null && bindings.size()>0)
//            {
//                List<T> list = new ArrayList<T>(bindings.size());
//                for (Binding<T> binding : bindings)
//                {
//                    list.add(binding.getProvider().get());
//                }
//                return list;
//            }
//            return Collections.emptyList();
//        }
//        catch (ConfigurationException e)
//        {
//            return Collections.emptyList();
//        }
    }

    @Override
    public <T> Collection<T> lookupLocalObjects(Class<T> type)
    {
        //just delegate to lookupObjects since there's no parent
        return lookupObjects(type);
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
