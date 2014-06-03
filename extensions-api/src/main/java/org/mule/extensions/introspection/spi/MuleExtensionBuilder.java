/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.spi;

import org.mule.extensions.introspection.api.Capability;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.api.MuleExtensionOperation;
import org.mule.extensions.introspection.api.MuleExtensionType;

/**
 * Implementation of the Builder design pattern to construct instances of
 * {@link org.mule.extensions.introspection.api.MuleExtension} without coupling
 * to implementations
 * <p/>
 * No user or spi component should ever create a {@link org.mule.extensions.introspection.api.MuleExtension}
 * in a way other than through this builder
 *
 * @since 1.0
 */
public interface MuleExtensionBuilder extends Builder<MuleExtension>
{

    MuleExtensionBuilder setName(String name);

    MuleExtensionBuilder setDescription(String description);

    MuleExtensionBuilder setVersion(String version);

    MuleExtensionBuilder setExtensionType(MuleExtensionType extensionType);

    MuleExtensionBuilder setMinMuleVersion(String minMuleVersion);

    MuleExtensionBuilder addConfiguration(MuleExtensionConfigurationBuilder configuration);

    <T extends MuleExtensionOperation, B> MuleExtensionBuilder addOperation(OperationBuilder<T, B> operation);

    <T extends Capability, C extends T> MuleExtensionBuilder addCapablity(Class<T> capabilityType, C capability);

    MuleExtensionConfigurationBuilder newConfiguration();

    MuleExtensionOperationBuilder newOperation();

    MuleExtensionScopeBuilder newScope();

    MuleExtensionOperationGroupBuilder newOperationGroup();

    MuleExtensionParameterBuilder newParameter();

}
