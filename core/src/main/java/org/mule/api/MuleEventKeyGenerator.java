/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
