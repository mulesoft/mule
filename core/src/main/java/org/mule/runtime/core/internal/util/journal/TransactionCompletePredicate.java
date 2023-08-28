/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
