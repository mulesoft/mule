/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class ConnectionNode {

  private String classTerm;

  @SerializedName("mapping")
  private Map<String, ConnectivitySchemaParameter> mappings = new LinkedHashMap<>();

  public String getClassTerm() {
    return classTerm;
  }

  public Map<String, ConnectivitySchemaParameter> getMappings() {
    return mappings;
  }

  public void setClassTerm(String classTerm) {
    this.classTerm = classTerm;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionNode that = (ConnectionNode) o;
    return Objects.equals(classTerm, that.classTerm) && Objects.equals(mappings, that.mappings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classTerm, mappings);
  }
}
