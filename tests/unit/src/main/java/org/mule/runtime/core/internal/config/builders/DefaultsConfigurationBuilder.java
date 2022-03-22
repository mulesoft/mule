/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.builders;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.module.artifact.activation.api.config.builder.MinimalConfigurationBuilder;

/**
 * Configures defaults required by Mule. This configuration builder is used to configure mule with these defaults when no other
 * ConfigurationBuilder that sets these is being used. This is used by both AbstractMuleTestCase and MuleClient. <br>
 * <br>
 * Default instances of the following are configured:
 * <ul>
 * <li>{@link SimpleRegistryBootstrap}
 * <li>{@link QueueManager}
 * <li>{@link SecurityManager}
 * <li>{@link ObjectStore}
 * </ul>
 *
 * @deprecated since 4.5.0. Use {@link MinimalConfigurationBuilder} instead
 */
@Deprecated
public class DefaultsConfigurationBuilder extends MinimalConfigurationBuilder {
}
