/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.store;

import java.io.Serializable;

/**
 * Defines a <{@link ListableObjectStore} to store data for event queues.
 */
public interface QueueStore<T extends Serializable> extends ListableObjectStore<T>
{
}
