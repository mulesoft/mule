/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore;

import org.mule.runtime.core.util.queue.QueueStoreDelegate;

/**
 * A marker interface for a QueueStoreDelegate that, along with its store, is transactional
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public interface TransactionalQueueStoreDelegate extends QueueStoreDelegate {
}
