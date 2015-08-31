/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.config.DeclaredConfiguration;

/**
 * Provides implicit configurations instances which are compliant with a {@link ConfigurationModel}.
 * These are used when no configuration has been specified. A best effort is made to automatically
 * provide a default one but it may not be possible.
 *
 * @since 3.8.0
 */
interface ImplicitConfigurationFactory
{

    /**
     * Creates an implicit configuration instance
     *
     * @param extensionModel       an {@link ExtensionModel}
     * @param operationContext     an {@link OperationContext}
     * @param <C>                  the type of the configuration instance to be returned
     * @return a {@link DeclaredConfiguration} or {@code null} if it's not possible to create an implicit configuration
     */
    <C> DeclaredConfiguration<C> createImplicitConfiguration(ExtensionModel extensionModel, OperationContext operationContext);
}
