/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.NameableObject;
import org.mule.api.agent.Agent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;

import com.google.inject.AbstractModule;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.ProviderMethod;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * @deprecated Guice module is deprecated and will be removed in Mule 4.
 */
@Deprecated
public class MuleSupportModule extends AbstractModule
{
    protected MuleContext muleContext;

    public MuleSupportModule(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    protected final void configure()
    {

        bindListener(Matchers.any(), new TypeListener()
        {
            public <I> void hear(TypeLiteral<I> iTypeLiteral, TypeEncounter<I> iTypeEncounter)
            {
                //iTypeEncounter.register(new MuleRegistryInjectionLister());
                iTypeEncounter.register(new MuleContextAwareInjector<I>());
                iTypeEncounter.register(new MuleBindInjector<I>());
            }
        });
        bind(MuleContext.class).toInstance(muleContext);
    }


    class MuleContextAwareInjector<I> implements MembersInjector<I>
    {
        public void injectMembers(I o)
        {
            if(o instanceof MuleContextAware)
            {
                ((MuleContextAware)o).setMuleContext(muleContext);
            }
        }
    }

    class MuleBindInjector<I> implements InjectionListener<I>
    {
        public void afterInjection(I i)
        {
            if(i instanceof ProviderMethod)
            {
                Class type = ((ProviderMethod)i).getKey().getTypeLiteral().getRawType();
                boolean bindRequired = (type.equals(Connector.class) || type.equals(Agent.class));

                Named bindTo = ((ProviderMethod)i).getMethod().getAnnotation(Named.class);
                if(bindTo!=null)
                {
                    try
                    {
                        Object o = ((ProviderMethod)i).get();
                        if(o instanceof NameableObject)
                        {
                            ((NameableObject)o).setName(bindTo.value());
                        }
                        muleContext.getRegistry().registerObject(bindTo.value(), o);
                    }
                    catch (RegistrationException e)
                    {
                        throw new MuleRuntimeException(CoreMessages.createStaticMessage("failed to bind " + bindTo.value()));
                    }
                }
                else if(bindRequired)
                {
                    throw new RuntimeException("Provider object type: " + type + ", must have a @Named annotation so that the object can be bound in Mule");
                }
            }
        }
    }

    
}
