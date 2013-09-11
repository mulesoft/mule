/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;

/**
 * A marker interface for a QueueInfoDelegate that needs to be rebuilt at startup
 */
public interface TransientQueueInfoDelegate extends QueueInfoDelegate
{
}
