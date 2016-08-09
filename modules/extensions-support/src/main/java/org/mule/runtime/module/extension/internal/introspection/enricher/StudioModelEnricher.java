/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import org.mule.runtime.extension.api.annotation.capability.CustomStudioEditor;
import org.mule.runtime.extension.api.annotation.capability.StudioProvidedEditor;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.property.StudioModelProperty;

/**
 * Enriches the {@link ExtensionDeclarer} with a {@link StudioModelProperty}
 *
 * @since 4.0
 */
public final class StudioModelEnricher extends AbstractAnnotatedModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    StudioProvidedEditor studioProvidedEditor =
        extractAnnotation(describingContext.getExtensionDeclarer().getDeclaration(), StudioProvidedEditor.class);
    CustomStudioEditor customStudioEditor =
        extractAnnotation(describingContext.getExtensionDeclarer().getDeclaration(), CustomStudioEditor.class);

    ExtensionDeclarer descriptor = describingContext.getExtensionDeclarer();
    descriptor.withModelProperty(createStudioEditorModelProperty(studioProvidedEditor, customStudioEditor));
  }

  private StudioModelProperty createStudioEditorModelProperty(StudioProvidedEditor studioProvidedEditor,
                                                              CustomStudioEditor customStudioEditor) {
    String editorFileName = "";
    boolean isDerived = false;

    // No annotations found on extension
    if (customStudioEditor == null && studioProvidedEditor == null) {
      isDerived = true;
    } else {
      if (customStudioEditor != null) {
        editorFileName = customStudioEditor.fileName();
      }
    }
    return new StudioModelProperty(editorFileName, isDerived);
  }
}
