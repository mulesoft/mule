/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.journal;

/**
 * Contract for a journal file entry
 *
 * @param <T> type of the transaction identifier.
 */
public interface JournalEntry<T> {

  /**
   * @return the transaction identifier
   */
  public T getTxId();

}

