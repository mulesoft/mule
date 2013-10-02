/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.MuleEvent;

import java.io.NotSerializableException;
import java.io.Serializable;

/**
 * Defines a way to generate cache keys for {@link MuleEvent} instances.
 */
public interface MuleEventKeyGenerator
{

    /**
     * Generates a key for an event.
     * <p/>
     * The generation algorithm should return the same key value for all the
     * events that are considered equals.
     *
     * @param event the event to generate the key for
     * @return the generated key
     * @throws NotSerializableException if the generated key is not {@link Serializable}
     */
    Serializable generateKey(MuleEvent event) throws NotSerializableException;
}
