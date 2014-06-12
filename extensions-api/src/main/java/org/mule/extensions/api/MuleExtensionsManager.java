/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.api;

import org.mule.extensions.introspection.api.Extension;

import java.util.Iterator;

/**
 * Manages the {@link org.mule.extensions.introspection.api.Extension}s available in the current context.
 * TODO: Define scopes and hierarchies. The extensions manager available in APP1 should see the extensions in the
 * runtime, the domain and the app. The one in APP2 should see the ones in runtime, domain and APP2. At the same time,
 * the one at a domain level should only see runtime and domain, and so forth...
 *
 * @since 1.0
 */
public interface MuleExtensionsManager
{

    /**
     * Registers the given {@link org.mule.extensions.introspection.api.Extension}
     *
     * @param extension a not {@code null} {@link org.mule.extensions.introspection.api.Extension}
     */
    void register(Extension extension);

    /**
     * Returns a {@link java.util.Iterator} listing all the available
     * {@link org.mule.extensions.introspection.api.Extension}s.
     *
     * @return an {@link java.util.Iterator}. Will not be {@code null} but might be empty
     */
    Iterator<Extension> getExtensions();


}
