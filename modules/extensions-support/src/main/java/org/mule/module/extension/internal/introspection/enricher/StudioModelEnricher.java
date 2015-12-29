/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import org.mule.extension.annotation.api.capability.CustomStudioEditor;
import org.mule.extension.annotation.api.capability.StudioProvidedEditor;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.property.StudioModelProperty;
import org.mule.module.extension.internal.introspection.ImmutableStudioModelProperty;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;

/**
 * Enriches the {@link DeclarationDescriptor} with a model property which key is {@link StudioModelProperty#KEY} and the value an instance of {@link StudioModelProperty}.
 *
 * @since 4.0
 */
public final class StudioModelEnricher extends AbstractAnnotatedModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        StudioProvidedEditor studioProvidedEditor = extractAnnotation(describingContext.getDeclarationDescriptor().getDeclaration(), StudioProvidedEditor.class);
        CustomStudioEditor customStudioEditor = extractAnnotation(describingContext.getDeclarationDescriptor().getDeclaration(), CustomStudioEditor.class);

        DeclarationDescriptor descriptor = describingContext.getDeclarationDescriptor();
        descriptor.withModelProperty(StudioModelProperty.KEY, createStudioEditorModelProperty(studioProvidedEditor, customStudioEditor));
    }

    private StudioModelProperty createStudioEditorModelProperty(StudioProvidedEditor studioProvidedEditor, CustomStudioEditor customStudioEditor)
    {
        String editorFileName = "";
        boolean isDerived = false;

        //No annotations found on extension
        if (customStudioEditor == null && studioProvidedEditor == null)
        {
            isDerived = true;
        }
        else
        {
            if (customStudioEditor != null)
            {
                editorFileName = customStudioEditor.fileName();
            }
        }
        return new ImmutableStudioModelProperty(editorFileName, isDerived);
    }
}
