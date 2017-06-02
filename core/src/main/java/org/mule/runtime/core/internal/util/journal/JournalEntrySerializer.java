/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Allows to serialize / deserialize log entries from an {@link java.io.OutputStream}
 */
public interface JournalEntrySerializer<T, K extends JournalEntry<T>> {

  /**
   * @param inputStream input stream with the serialized log entry.
   * @return a JournalEntry instance from the ouput stream
   */
  K deserialize(DataInputStream inputStream) throws IOException;

  /**
   * @param journalEntry journal entry to serialize
   * @param dataOutputStream destination for the serialization
   */
  void serialize(K journalEntry, DataOutputStream dataOutputStream);

}
