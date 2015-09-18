/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api;

/**
 * Generic contract for a config of a connector which operates
 * over a {@link FileSystem}
 *
 * @since 4.0
 */
public interface FileConnectorConfig
{

    /**
     * The directory to be considered as the root of every
     * relative path used with this connector.
     */
    String getBaseDir();
}
