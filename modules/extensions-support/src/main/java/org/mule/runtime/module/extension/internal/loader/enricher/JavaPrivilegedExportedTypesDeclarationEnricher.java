/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.stream;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.PrivilegedExport;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

/**
 * Enriches the declaration with the types which are manually exported through {@link PrivilegedExport}
 *
 * @since 4.0
 */
public final class JavaPrivilegedExportedTypesDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    PrivilegedExport exportAnnotation =
        extractAnnotation(extensionLoadingContext.getExtensionDeclarer().getDeclaration(), PrivilegedExport.class);
    if (exportAnnotation != null) {
      ExtensionDeclarer declarer = extensionLoadingContext.getExtensionDeclarer();
      stream(exportAnnotation.packages()).forEach(declarer::withPrivilegedPackage);
      stream(exportAnnotation.artifacts()).forEach(declarer::withPrivilegedArtifact);
    }
  }
}
