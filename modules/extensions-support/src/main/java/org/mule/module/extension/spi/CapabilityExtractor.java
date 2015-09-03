/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.spi;

import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.introspection.Capable;
import org.mule.extension.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.introspection.declaration.fluent.OperationDescriptor;

import java.lang.reflect.Method;


/**
 * A component capable of extracting one specific capability
 * out of a {@link Class} that composes a {@link Capable}.
 * <p/>
 * An extension might have capabilities at different levels (configurations, operations or the
 * extension itself). This interface provides specific methods to look for those capabilities
 * at each specific level.
 * <p/>
 * Because actual capabilities might be defined across several modules (or even extensions!)
 * the actual extractors are fetched through SPI, using a {@link ServiceRegistry}.
 * <p/>
 * Each implementation of this class has to aim to one and only one specific capability type. It's
 * this extractor's responsibility to ignore the capabilities which are outside of its domain
 * and to ignore any extension types which don't support the given capability
 *
 * @since 3.7.0
 */
public interface CapabilityExtractor
{

    /**
     * Looks for a specific capability in the given {@code extensionType} and returns it.
     *
     * @param declarationDescriptor the descriptor that is being built
     * @param extensionType         the type of the {@link Capable} class
     * @return the extracted capability or {@code null} if none found
     */
    Object extractExtensionCapability(DeclarationDescriptor declarationDescriptor, Class<?> extensionType);

    /**
     * Looks for a specific capability in the given {@code configType} and returns it.
     *
     * @param configurationDescriptor the descriptor of the config being described
     * @param configType              the type of the {@link Capable} class
     * @return the extracted capability or {@code null} if none found
     */
    Object extractConfigCapability(ConfigurationDescriptor configurationDescriptor, Class<?> configType);

    /**
     * Looks for a specific capability in the given {@code operationMethod} and returns it.
     *
     * @param operationDescriptor the descriptor of the operation being described
     * @param operationMethod     the java {@link Method} on which the operation's logic is implemented
     * @return the extracted capability or {@code null} if none found
     */
    Object extractOperationCapability(OperationDescriptor operationDescriptor, Method operationMethod);
}
