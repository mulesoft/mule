/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static java.util.Collections.unmodifiableSet;

import org.mule.runtime.api.meta.model.ModelProperty;

import java.util.Set;

public class ExportedClassNamesModelProperty implements ModelProperty {

  private final Set<String> exportedClassNames;

  public ExportedClassNamesModelProperty(Set<String> exportedClassNames) {
    this.exportedClassNames = unmodifiableSet(exportedClassNames);
  }

  public Set<String> getExportedClassNames() {
    return exportedClassNames;
  }

  @Override
  public String getName() {
    return "exportedClassNames";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
