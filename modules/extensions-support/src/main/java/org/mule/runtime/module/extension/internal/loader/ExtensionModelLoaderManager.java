/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;

/**
 * Manages the lifecycle of the {@link org.mule.runtime.extension.api.loader.ExtensionModelLoader} available in the {@link ExtensionModelLoaderRepository}.
 *
 * @since 4.0
 */
public interface ExtensionModelLoaderManager extends Startable, Stoppable, ExtensionModelLoaderRepository {

}
