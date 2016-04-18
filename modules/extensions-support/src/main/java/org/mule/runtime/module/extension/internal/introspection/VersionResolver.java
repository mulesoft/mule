/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.introspection.ExtensionModel;

/**
 * Component that resolves an extension's version.
 *
 * @since 4.0
 */
public interface VersionResolver
{

    /**
     * Resolves the version of a given {@link ExtensionModel}
     *
     * @param extension the {@link Extension} for which the version will be resolved
     * @return a {@link String} representing the version
     */
    String resolveVersion(Extension extension);
}
