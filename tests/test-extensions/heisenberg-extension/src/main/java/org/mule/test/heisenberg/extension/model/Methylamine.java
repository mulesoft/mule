/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

public class Methylamine {

  @Parameter
  private String importer;

  @Parameter
  private boolean stolen;

  public String getImporter() {
    return importer;
  }

  public void setImporter(String importer) {
    this.importer = importer;
  }

  public boolean isStolen() {
    return stolen;
  }

  public void setStolen(boolean stolen) {
    this.stolen = stolen;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Methylamine that = (Methylamine) o;
    return stolen == that.stolen &&
        Objects.equals(importer, that.importer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(importer, stolen);
  }
}
