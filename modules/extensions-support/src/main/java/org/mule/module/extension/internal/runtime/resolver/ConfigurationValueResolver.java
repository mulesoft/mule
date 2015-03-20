/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.NamedObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;

/**
 * A {@link ValueResolver} for returning instances that implement
 * a {@link Configuration}. Those instances are created through the
 * {@link Configuration#getInstantiator()} component.
 * It supports both static and dynamic configurations (understanding by static
 * that non of its parameters have expressions, and dynamic that at least one of them does).
 * <p/>
 * In the case of static configurations, it will always return the same instance, in the case of
 * dynamic, it will evaluate those expressions and only return the same instance for equivalent
 * instances of {@link ResolverSetResult}. Those instances will be cached and discarded
 * after one minute of inactivity.
 * <p/>
 * A {@link ResolverSet} is used for evaluating the attributes and creating new instances.
 * It also implements {@link NamedObject} since configurations are named and unique from a user's
 * point of view. Notice however that the named object is this resolver and in the case of
 * dynamic configurations instances are not likely to be unique
 * <p/>
 * The generated instance will be registered with the {@code extensionManager}
 * through {@link ExtensionManager#registerConfigurationInstance(Configuration, String, Object)}
 *
 * @since 3.7.0
 */
public final class ConfigurationValueResolver implements ValueResolver<Object>, NamedObject
{

    private final String name;
    private final ValueResolver delegate;

    public ConfigurationValueResolver(String name,
                                      Configuration configuration,
                                      ResolverSet resolverSet,
                                      MuleContext muleContext)
    {
        this.name = name;

        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);

        if (resolverSet.isDynamic())
        {
            delegate = new DynamicConfigurationValueResolver(name, configuration, configurationObjectBuilder, resolverSet, muleContext);
        }
        else
        {
            try
            {
                Object config = configurationObjectBuilder.build(getInitialiserEvent(muleContext));
                muleContext.getExtensionManager().registerConfigurationInstance(configuration, name, config);
                delegate = new StaticValueResolver<>(config);
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
    }

    /**
     * Returns an instance associated with the given {@code event}
     *
     * @param event a {@link MuleEvent}
     * @return a configuration instance
     * @throws {@link MuleException}
     */
    @Override
    public Object resolve(MuleEvent event) throws MuleException
    {
        return delegate.resolve(event);
    }

    /**
     * Whether the generated configurations are dynamic or not
     */
    @Override
    public boolean isDynamic()
    {
        return delegate.isDynamic();
    }


    private MuleEvent getInitialiserEvent(MuleContext muleContext)
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(null, muleContext), REQUEST_RESPONSE, (FlowConstruct) null);
    }

    @Override
    public String getName()
    {
        return name;
    }
}