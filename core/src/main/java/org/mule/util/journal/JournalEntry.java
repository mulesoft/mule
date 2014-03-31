/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.journal;

/**
 * Contract for a journal file entry
 *
 * @param <T> type of the transaction identifier.
 */
public interface JournalEntry<T>
{

    /**
     * @return the transaction identifier
     */
    public T getTxId();

}

