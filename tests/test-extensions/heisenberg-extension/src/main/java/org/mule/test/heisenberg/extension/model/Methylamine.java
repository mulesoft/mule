/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class Methylamine {

  @Parameter
  private String importer;

  @Parameter
  private boolean isStolen;

  public String getImporter() {
    return importer;
  }

  public void setImporter(String importer) {
    this.importer = importer;
  }

  public boolean isStolen() {
    return isStolen;
  }

  public void setStolen(boolean stolen) {
    isStolen = stolen;
  }
}
