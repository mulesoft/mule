/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.info;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ClassBasedAnnotationValueFetcher;

import java.util.List;

public class ExportInfo {

  private final List<Type> types;
  private final List<String> resources;

  public static ExportInfo from(Export annotation, ClassTypeLoader typeLoader) {
    AnnotationValueFetcher<Export> fetcher = new ClassBasedAnnotationValueFetcher<>(annotation, typeLoader);
    return new ExportInfo(fetcher.getClassArrayValue(Export::classes), fetcher.getArrayValue(Export::resources));
  }

  public static ExportInfo from(org.mule.sdk.api.annotation.Export annotation, ClassTypeLoader typeLoader) {
    AnnotationValueFetcher<org.mule.sdk.api.annotation.Export> fetcher = new ClassBasedAnnotationValueFetcher<>(annotation, typeLoader);
    return new ExportInfo(fetcher.getClassArrayValue(org.mule.sdk.api.annotation.Export::classes), fetcher.getArrayValue(org.mule.sdk.api.annotation.Export::resources));
  }

  public ExportInfo(List<Type> types, List<String> resources) {
    this.types = types;
    this.resources = resources;
  }

  public List<Type> getExportedTypes() {
    return types;
  }

  public List<String> getExportedResources() {
    return resources;
  }
}
