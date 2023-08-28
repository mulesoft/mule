/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.dsl.xml.TypeDsl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@TypeDsl(allowTopLevelDefinition = true)
public class RecursivePojo {

  @Parameter
  @Optional
  private RecursivePojo next;

  @Parameter
  @Optional
  @NullSafe
  private List<RecursivePojo> childs;

  @Parameter
  @Optional
  @NullSafe
  private Map<String, RecursivePojo> mappedChilds;

  public RecursivePojo getNext() {
    return next;
  }

  public List<RecursivePojo> getChilds() {
    return childs;
  }

  public Map<String, RecursivePojo> getMappedChilds() {
    return mappedChilds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RecursivePojo that = (RecursivePojo) o;
    return Objects.equals(next, that.next) &&
        Objects.equals(childs, that.childs) &&
        Objects.equals(mappedChilds, that.mappedChilds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(next, childs, mappedChilds);
  }
}
