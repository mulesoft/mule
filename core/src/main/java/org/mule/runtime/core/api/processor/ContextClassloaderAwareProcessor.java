/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.api.annotation.NoImplement;

/**
 * Marker interface for {@link Processor} implementations that either:
 * <ul>
 * <li>Do not require to have the threadContextClassLoader set to the {@link ClassLoader} of its application before being
 * invoked.</li>
 * <li>Internally fetch and set the threadContextClassLoader of the application themselves.</li>
 * </ul>
 *
 * @since 4.2
 */
@NoImplement
public interface ContextClassloaderAwareProcessor extends Processor {

}
