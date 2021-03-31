/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import com.google.gson.annotations.SerializedName;

public class Definition {

  @SerializedName("documents")
  private DocumentNode document = new DocumentNode();

  public DocumentNode getDocument() {
    return document;
  }
}
