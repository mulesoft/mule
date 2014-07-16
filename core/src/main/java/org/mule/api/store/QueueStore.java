/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.store;

import java.io.Serializable;

/**
 * Defines a <{@link ListableObjectStore} to store data for event queues.
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public interface QueueStore<T extends Serializable> extends ListableObjectStore<T>
{
}
