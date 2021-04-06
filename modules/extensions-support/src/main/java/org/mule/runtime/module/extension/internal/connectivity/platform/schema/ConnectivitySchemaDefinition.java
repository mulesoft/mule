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

public class ConnectivitySchemaDefinition {

  @SerializedName("documents")
  private ConnectivitySchemaBody document = new ConnectivitySchemaBody();

  private Map<String, String> uses = new LinkedHashMap<>();
  private Map<String, String> external = new LinkedHashMap<>();
  private Map<String, ConnectivitySchemaNode> nodeMappings = new LinkedHashMap<>();

  public ConnectivitySchemaBody getDocument() {
    return document;
  }

  public Map<String, String> getUses() {
    return uses;
  }

  public Map<String, String> getExternal() {
    return external;
  }

  public Map<String, ConnectivitySchemaNode> getNodeMappings() {
    return nodeMappings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectivitySchemaDefinition that = (ConnectivitySchemaDefinition) o;
    return Objects.equals(document, that.document)
        && Objects.equals(uses, that.uses)
        && Objects.equals(external, that.external)
        && Objects.equals(nodeMappings, that.nodeMappings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(document, uses, external, nodeMappings);
  }
}
