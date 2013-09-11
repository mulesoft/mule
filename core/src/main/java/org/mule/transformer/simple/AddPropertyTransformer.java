/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transport.PropertyScope;

public class AddPropertyTransformer extends AbstractAddVariablePropertyTransformer
{

    @Override
    protected PropertyScope getScope()
    {
        return PropertyScope.OUTBOUND;
    }

    public void setPropertyName(String propertyName)
    {
        this.setIdentifier(propertyName);
    }
}
