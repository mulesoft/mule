/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.info;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;

import java.util.List;

/**
 * Simple bean which maps the info defined in the {@link org.mule.runtime.extension.api.annotation.Export} or
 * {@link org.mule.sdk.api.annotation.Export} annotations, so that consumers can decouple from which was used.
 *
 * @since 4.5.0
 */
public class ExportInfo {

  private final List<Type> types;
  private final List<String> resources;

  public static ExportInfo fromLegacy(AnnotationValueFetcher<Export> annotation) {
    return new ExportInfo(annotation.getClassArrayValue(Export::classes), annotation.getArrayValue(Export::resources));
  }

  public static ExportInfo fromSdkApi(AnnotationValueFetcher<org.mule.sdk.api.annotation.Export> annotation) {
    return new ExportInfo(annotation.getClassArrayValue(org.mule.sdk.api.annotation.Export::classes),
                          annotation.getArrayValue(org.mule.sdk.api.annotation.Export::resources));
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
