/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
