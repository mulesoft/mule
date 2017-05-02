/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

/**
 * Marker, private, <b>NON API</b> interface to indicate that the implementing component
 * does not support connectivity testing.
 * <p>
 * Only the runtime is allowed to use this interface
 *
 * @since 4.0
 */
public interface NoConnectivityTest {

}
