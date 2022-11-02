/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

/**
 * Loads the {@link ClassLoaderModel} for Mule artifacts
 * <p/>
 * Explicitly defined to enable definition of implementations using SPI.
 *
 * @deprecated since 4.5 use {@link ClassLoaderConfigurationLoader}.
 */
// TODO - W-11098291: remove this class
@Deprecated
public interface ClassLoaderModelLoader extends ClassLoaderConfigurationLoader {



}
