/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.transaction;

/**
 * Indicates how a connector should behave about transactions
 */
public enum TransactionalAction
{
    /**
     * Always require an active transaction
     */
    ALWAYS_JOIN,

    /**
     * Does not requires an active transaction, but will use it if available
     */
    JOIN_IF_POSSIBLE,

    /**
     * Never uses a transaction
     */
    NOT_SUPPORTED
}
