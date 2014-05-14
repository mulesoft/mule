/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import org.mule.extensions.ExtensionsManager;
import org.mule.extensions.introspection.Extension;

import java.util.List;

/**
 * A component capable of searching the classpath for extensions
 * according to the algorithm described in {@link ExtensionsManager#discoverExtensions(ClassLoader)}
 *
 * @since 3.7.0
 */
public interface ExtensionDiscoverer
{

    /**
     * Performs a search for extensions
     * according to the algorithm described in {@link ExtensionsManager#discoverExtensions(ClassLoader)}
     *
     * @param classLoader the {@link ClassLoader} in which path perform the search
     * @return a {@link List} of {@link Extension}. Might be empty
     * but it will never be {@code null}
     */
    List<Extension> discover(ClassLoader classLoader);
}
