/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.compression;

import java.io.IOException;

public final class InvalidZipFileException extends IOException {

  public InvalidZipFileException(String message) {
    super(message);
  }
}
