/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import org.mule.runtime.extension.api.introspection.ModelProperty;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;

/**
 * Marks that the enriched {@link ParameterModel} does not accepts
 * references to objects in the mule registry. All static String values
 * should be mapped to the actual String value
 *
 * @since 4.0
 */
public final class NoReferencesModelProperty implements ModelProperty
{

    @Override
    public String getName()
    {
        return "noRef";
    }

    @Override
    public boolean isExternalizable()
    {
        return false;
    }
}
