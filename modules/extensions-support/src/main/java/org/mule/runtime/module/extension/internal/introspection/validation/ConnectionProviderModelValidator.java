/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationsConnectionType;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;

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
            if (connectionType != null)
            {
                validateConnectionTypes((RuntimeConnectionProviderModel) providerModel, extensionModel, connectionType);
            }
        });
    }

    private void validateConnectionTypes(RuntimeConnectionProviderModel providerModel, ExtensionModel extensionModel, Class<?> connectionType)
    {
        final Class extensionConnectionType = providerModel.getConnectionType();
        if (!connectionType.isAssignableFrom(extensionConnectionType))
        {
            throw new IllegalConnectionProviderModelDefinitionException(String.format("Extension '%s' defines a connection provider of name '%s' which yields connections of type '%s'. " +
                                                                                      "However, the extension's operations expect connections of type '%s'. Please make sure that all connection " +
                                                                                      "providers in the extension can be used with all its operations",
                                                                                      extensionModel.getName(),
                                                                                      providerModel.getName(),
                                                                                      extensionConnectionType.getName(),
                                                                                      connectionType.getName()));
        }
    }
}
