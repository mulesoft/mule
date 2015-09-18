/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.api.MuleEvent;
import org.mule.api.extension.introspection.ConfigurationModel;
import org.mule.api.extension.introspection.ExtensionModel;
import org.mule.api.extension.runtime.ConfigurationInstance;

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
     * @param extensionModel the {@link ExtensionModel} from which a {@link ConfigurationModel} is to be selected
     * @param muleEvent      the current {@link MuleEvent}
     * @param <C>            the generic type of the returned {@link ConfigurationInstance}
     * @return a {@link ConfigurationInstance} or {@code null} if it's not possible to create an implicit configuration
     */
    <C> ConfigurationInstance<C> createImplicitConfigurationInstance(ExtensionModel extensionModel, MuleEvent muleEvent);
}
