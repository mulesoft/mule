/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.spi;

import org.mule.extensions.api.MuleExtensionsManager;
import org.mule.extensions.introspection.api.Extension;

import java.util.List;

/**
 * This component is responsible from discovering the available extensions in the context classpath.
 * When an extension is found, it will parse the Annotations in it to generate an instance of
 * {@link org.mule.extensions.introspection.api.Extension} that describes it.
 * <p/>
 * Optionally, it can also register those extensions in a {@link org.mule.extensions.api.MuleExtensionsManager}
 * <p/>
 * The scanning process works as follows:
 * <ul>
 * <li>It scans the classpath looking for files in the path META-INF/extensions/mule.extensions</li>
 * <li>The found files are assumed to be text files in which one canonical name is found per line</li>
 * <li>Those classes are loaded and scanned for annotation which are parsed in turn</li>
 * </ul>
 * <p/>
 * Exceptions will result from listing extensions classes not properly annotated.
 *
 * @since 1.0
 */
public interface MuleExtensionScanner
{

    /**
     * Scans the classpath for extensions and describes them as {@link org.mule.extensions.introspection.api.Extension}s.
     * Found extensions are then returned in a {@link java.util.List}
     *
     * @return a {@link java.util.List} of {@link org.mule.extensions.introspection.api.Extension}
     */
    List<Extension> scan();

    /**
     * Calls {@link #scan()} but also registers the found exceptions in the given {@code muleExtensionsManager}
     * by invoking {@link org.mule.extensions.api.MuleExtensionsManager#register(org.mule.extensions.introspection.api.Extension)}
     *
     * @param muleExtensionsManager a {@link org.mule.extensions.api.MuleExtensionsManager}
     * @return a {@link java.util.List} of {@link org.mule.extensions.introspection.api.Extension}
     */
    List<Extension> scanAndRegister(MuleExtensionsManager muleExtensionsManager);

}
