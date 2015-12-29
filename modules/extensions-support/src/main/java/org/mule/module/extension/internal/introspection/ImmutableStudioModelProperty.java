/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import org.mule.extension.api.introspection.property.StudioModelProperty;

/**
 * Immutable implementation of {@link StudioModelProperty}
 *
 * @since 4.0
 */
public final class ImmutableStudioModelProperty implements StudioModelProperty
{

    private final String editorFileName;
    private final boolean generated;
    public ImmutableStudioModelProperty(String editorFileName, boolean generated)
    {
        this.editorFileName = editorFileName;
        this.generated = generated;
     }

    @Override
    public String getEditorFileName()
    {
        return editorFileName;
    }

    @Override
    public boolean isGenerated()
    {
        return generated;
    }
}
