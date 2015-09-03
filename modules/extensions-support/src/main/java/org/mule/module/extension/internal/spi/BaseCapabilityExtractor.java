/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.spi;

import org.mule.extension.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.introspection.declaration.fluent.OperationDescriptor;
import org.mule.module.extension.spi.CapabilityExtractor;

import java.lang.reflect.Method;

/**
 * Base implementation of {@link CapabilityExtractor} which provides null implementations for all its
 * extract methods.
 * <p/>
 * Because it's quite often for a particular {@link CapabilityExtractor} to only consider extraction at
 * one specific level only, extending this class allows developers to only implement the levels you care about.
 *
 * @since 4.0
 */
public abstract class BaseCapabilityExtractor implements CapabilityExtractor
{

    /**
     * @return {@code null}
     */
    @Override
    public Object extractExtensionCapability(DeclarationDescriptor declarationDescriptor, Class<?> extensionType)
    {
        return null;
    }

    /**
     * @return {@code null}
     */
    @Override
    public Object extractConfigCapability(ConfigurationDescriptor configurationDescriptor, Class<?> configType)
    {
        return null;
    }

    /**
     * @return {@code null}
     */
    @Override
    public Object extractOperationCapability(OperationDescriptor operationDescriptor, Method operationMethod)
    {
        return null;
    }
}
