/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal;

/**
 * Determines if the operation denotes a transaction complete log entry.
 */
public interface TransactionCompletePredicate {

  /**
   * @param journalEntry journal entry
   * @return true if the journal entry completes a transaction, false otherwise.
   */
  boolean isTransactionComplete(JournalEntry journalEntry);

}
