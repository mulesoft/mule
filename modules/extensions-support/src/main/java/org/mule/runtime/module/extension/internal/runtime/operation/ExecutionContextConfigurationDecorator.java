/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * Provides access to the actual {@link ConfigurationInstance} regardless of its decorator.
 * <p>
 * This is needed for functionality that needs the actual {@link ConfigurationInstance}, like transactions.
 *
 * @since 4.1.3
 */
public interface ExecutionContextConfigurationDecorator extends ConfigurationInstance {

  ConfigurationInstance getDecorated();
}
