/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import org.mule.extension.introspection.Capable;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;

/**
 * Extracts all the capabilities in a given extension and registers it on a builder
 *
 * @since 3.7.0
 */
public interface CapabilitiesResolver
{

    /**
     * Resolves the capabilities present in {@code extensionType} and registers them in
     * {@code builder}
     *
     * @param declaration     a {@link DeclarationDescriptor} describing the {@link Extension} to be built
     * @param capableType     the type of the {@link Capable} class
     * @throws java.lang.IllegalArgumentException if any argument is {@code null}
     */
    void resolveCapabilities(DeclarationDescriptor declaration, Class<?> capableType);
}
