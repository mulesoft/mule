/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
