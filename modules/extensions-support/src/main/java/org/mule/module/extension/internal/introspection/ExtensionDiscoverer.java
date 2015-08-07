/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Extension;

import java.util.List;

/**
 * A component capable of searching the classpath for extensions
 * according to the algorithm described in {@link ExtensionManager#discoverExtensions(ClassLoader)}
 *
 * @since 3.7.0
 */
public interface ExtensionDiscoverer
{

    /**
     * Performs a search for extensions according to the algorithm described in
     * {@link ExtensionManager#discoverExtensions(ClassLoader)}
     *
     * @param classLoader the {@link ClassLoader} on which the search will be performed
     * @return a {@link List} of {@link Extension}. Might be empty but it will never be {@code null}
     */
    List<Extension> discover(ClassLoader classLoader);
}
