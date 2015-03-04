/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

/**
 * A contact for objects capable of giving access
 * to a {@link ObjectLimbo}
 *
 * @since 3.7.0
 */
public interface ObjectLimboLocator
{

    /**
     * Returns a {@link ObjectLimbo}
     *
     * @return a {@link ObjectLimbo}
     */
    ObjectLimbo getLimbo();
}
