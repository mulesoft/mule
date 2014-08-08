/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.serialization;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

import javax.inject.Inject;

/**
 * Objects who need an {@link ObjectSerializer} can implement this
 * interface to allow it been set.
 * <p/>
 * Unlike similar interfaces like {@link MuleContextAware}, the platform
 * will not automatically inject the default {@link ObjectSerializer} into
 * instances of this interface. Recommended ways of using this interface
 * is by either annotating the method with {@link Inject} or manually invoking it
 * if the instance is not part of the registry and you don't
 * want to use {@link MuleContext#getInjector()}
 *
 * @since 3.7.0
 */
public interface ObjectSerializerAware
{

    /**
     * Receives a {@link ObjectSerializer}
     * @param objectSerializer a {@link ObjectSerializer}
     */
    void setObjectSerializer(ObjectSerializer objectSerializer);
}
