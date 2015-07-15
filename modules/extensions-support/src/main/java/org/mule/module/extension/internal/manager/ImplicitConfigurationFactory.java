/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;

/**
 * Provides implicit configurations instances which are compliant with a {@link Configuration} model.
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
     * @param extension an {@link Extension}
     * @param operationContext an {@link OperationContext}
     * @param registrationCallback a {@link ConfigurationInstanceRegistrationCallback}
     * @return a {@link ConfigurationInstanceHolder} or {@code null} if it's not possible to create an implicit configuration
     */
    ConfigurationInstanceHolder createImplicitConfigurationInstance(Extension extension, OperationContext operationContext, ConfigurationInstanceRegistrationCallback registrationCallback);
}
