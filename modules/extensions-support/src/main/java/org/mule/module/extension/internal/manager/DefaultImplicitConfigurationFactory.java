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
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.ParameterModel;
import org.mule.extension.runtime.ConfigurationRegistrationCallback;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.config.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * Default implementation of {@link ImplicitConfigurationFactory}.
 * Implicit configurations are created from {@link ConfigurationModel configurations} which have all
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
    public ConfigurationHolder createImplicitConfiguration(ExtensionModel extensionModel, OperationContext operationContext, ConfigurationRegistrationCallback registrationCallback)
    {
        ConfigurationModel implicitConfigurationModel = getImplicitConfiguration(extensionModel);

        if (implicitConfigurationModel == null)
        {
            throw new IllegalStateException(String.format("Could not find a config for extension '%s' and none can be created automatically. Please define one", extensionModel.getName()));
        }

        synchronized (implicitConfigurationModel)
        {
            //check that another thread didn't beat us to create the instance
            if (!extensionRegistry.getExtensionState(extensionModel).getConfigurationProviders().isEmpty())
            {
                return null;
            }

            final String instanceName = String.format("%s-%s", extensionModel.getName(), implicitConfigurationModel.getName());
            ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(implicitConfigurationModel, buildImplicitConfigurationResolverSet(implicitConfigurationModel));

            Object configuration;
            try
            {
                configuration = configurationObjectBuilder.build(asOperationContextAdapter(operationContext).getEvent());
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }
            return new ConfigurationHolder(instanceName, configuration);
        }
    }

    private ResolverSet buildImplicitConfigurationResolverSet(ConfigurationModel configurationModel)
    {
        ResolverSet resolverSet = new ResolverSet();
        for (ParameterModel parameterModel : configurationModel.getParameterModels())
        {
            Object defaultValue = parameterModel.getDefaultValue();
            if (defaultValue != null)
            {
                ValueResolver<Object> valueResolver;
                if (defaultValue instanceof String && muleContext.getExpressionManager().isExpression((String) defaultValue))
                {
                    valueResolver = new TypeSafeExpressionValueResolver<>((String) defaultValue, parameterModel.getType());
                }
                else
                {
                    valueResolver = new StaticValueResolver<>(defaultValue);
                }

                resolverSet.add(parameterModel, valueResolver);
            }
        }

        return resolverSet;
    }

    private ConfigurationModel getImplicitConfiguration(ExtensionModel extensionModel)
    {
        for (ConfigurationModel configurationModel : extensionModel.getConfigurations())
        {
            if (canBeUsedImplicitly(configurationModel))
            {
                return configurationModel;
            }
        }

        return null;
    }

    private boolean canBeUsedImplicitly(ConfigurationModel configurationModel)
    {
        for (ParameterModel parameterModel : configurationModel.getParameterModels())
        {
            if (parameterModel.isRequired() && parameterModel.getDefaultValue() == null)
            {
                return false;
            }
        }

        return true;
    }

}
