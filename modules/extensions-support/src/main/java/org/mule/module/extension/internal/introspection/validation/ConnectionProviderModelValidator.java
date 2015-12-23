/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.getImplementingType;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getOperationsConnectionType;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;

import com.sun.org.apache.xpath.internal.ExtensionsProvider;

/**
 * {@link ModelValidator} which applies to {@link ExtensionModel}s which either contains
 * {@link ConnectionProviderModel}s, {@link OperationModel}s which require a connection or both.
 * <p>
 * This validator makes sure that:
 * <ul>
 * <li>All operations require the same type of connections</li>
 * <li>All the {@link ExtensionsProvider}s are compatible with all the {@link ConfigurationModel}s in the extension</li>
 * <li>All the {@link ExtensionsProvider}s return connections of the same type as expected by the {@link OperationModel}s</li>
 * </ul>
 *
 * @since 4.0
 */
public final class ConnectionProviderModelValidator implements ModelValidator
{

    @Override
    public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
    {
        Class<?> connectionType = getOperationsConnectionType(extensionModel);
        validateConnectionProviders(extensionModel, connectionType);
    }

    private void validateConnectionProviders(ExtensionModel extensionModel, Class<?> connectionType)
    {
        extensionModel.getConnectionProviders().stream().forEach(providerModel -> {
            validateConfigType(providerModel, extensionModel);
            if (connectionType != null)
            {
                validateConnectionTypes(providerModel, extensionModel, connectionType);
            }
        });
    }

    private void validateConfigType(ConnectionProviderModel providerModel, ExtensionModel extensionModel)
    {
        Class<?> providerConfigType = providerModel.getConfigurationType();
        for (ConfigurationModel configurationModel : extensionModel.getConfigurationModels())
        {
            ImplementingTypeModelProperty typeProperty = configurationModel.getModelProperty(ImplementingTypeModelProperty.KEY);
            if (typeProperty != null && !providerConfigType.isAssignableFrom(typeProperty.getType()))
            {
                throw new IllegalModelDefinitionException(String.format(
                        "Configuration '%s' in Extension '%s' is of type '%s' which cannot be used with the connection provider of type '%s' "        +
                        "because it requires configs of type '%s'. Please make sure that all configuration models in the extension can be used with " +
                        "any of the defined connection providers",
                        configurationModel.getName(), extensionModel.getName(), typeProperty.getType().getName(),
                        getImplementingType(providerModel).getName(), providerConfigType.getName()));
            }
        }
    }

    private void validateConnectionTypes(ConnectionProviderModel providerModel, ExtensionModel extensionModel, Class<?> connectionType)
    {
        if (!connectionType.isAssignableFrom(providerModel.getConnectionType()))
        {
            throw new IllegalModelDefinitionException(String.format("Extension '%s' defines a connection provider of name '%s' which yields connections of type '%s'. " +
                                                                    "However, the extension's operations expect connections of type '%s'. Please make sure that all connection " +
                                                                    "providers in the extension can be used with all its operations",
                                                                    extensionModel.getName(), providerModel.getName(), providerModel.getConnectionType().getName(),
                                                                    connectionType.getName()));
        }
    }
}
