/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.asOperationContextAdapter;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * Default implementation of {@link ImplicitConfigurationFactory}.
 * Implicit configurations are created from {@link Configuration configurations} which have all
 * parameters that are either not required or have a default value defined that's not {@code null}.
 *
 * @since 3.8.0
 */
final class DefaultImplicitConfigurationFactory implements ImplicitConfigurationFactory
{

    private final MuleContext muleContext;
    private final ExtensionRegistry extensionRegistry;

    protected DefaultImplicitConfigurationFactory(ExtensionRegistry extensionRegistry, MuleContext muleContext)
    {
        this.extensionRegistry = extensionRegistry;
        this.muleContext = muleContext;
    }

    @Override
    public ConfigurationInstanceHolder createImplicitConfigurationInstance(Extension extension, OperationContext operationContext, ConfigurationInstanceRegistrationCallback registrationCallback)
    {
        Configuration implicitConfiguration = getImplicitConfiguration(extension);

        if (implicitConfiguration == null)
        {
            throw new IllegalStateException(String.format("Could not find a config for extension '%s' and none can be created automatically. Please define one", extension.getName()));
        }

        synchronized (implicitConfiguration)
        {
            //check that another thread didn't beat us to create the instance
            if (!extensionRegistry.getExtensionState(extension).getConfigurationInstanceProviders().isEmpty())
            {
                return null;
            }

            final String instanceName = String.format("%s-%s", extension.getName(), implicitConfiguration.getName());
            ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(
                    instanceName,
                    extension,
                    implicitConfiguration,
                    buildImplicitConfigurationResolverSet(implicitConfiguration),
                    registrationCallback);

            Object configurationInstance;
            try
            {
                configurationInstance = configurationObjectBuilder.build(asOperationContextAdapter(operationContext).getEvent());
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }
            return new ConfigurationInstanceHolder(instanceName, configurationInstance);
        }
    }

    private ResolverSet buildImplicitConfigurationResolverSet(Configuration configuration)
    {
        ResolverSet resolverSet = new ResolverSet();
        for (Parameter parameter : configuration.getParameters())
        {
            Object defaultValue = parameter.getDefaultValue();
            if (defaultValue != null)
            {
                ValueResolver<Object> valueResolver;
                if (defaultValue instanceof String && muleContext.getExpressionManager().isExpression((String) defaultValue))
                {
                    valueResolver = new TypeSafeExpressionValueResolver<>((String) defaultValue, parameter.getType());
                }
                else
                {
                    valueResolver = new StaticValueResolver<>(defaultValue);
                }

                resolverSet.add(parameter, valueResolver);
            }
        }

        return resolverSet;
    }

    private Configuration getImplicitConfiguration(Extension extension)
    {
        for (Configuration configuration : extension.getConfigurations())
        {
            if (canBeUsedImplicitly(configuration))
            {
                return configuration;
            }
        }

        return null;
    }

    private boolean canBeUsedImplicitly(Configuration configuration)
    {
        for (Parameter parameter : configuration.getParameters())
        {
            if (parameter.isRequired() && parameter.getDefaultValue() == null)
            {
                return false;
            }
        }

        return true;
    }

}
