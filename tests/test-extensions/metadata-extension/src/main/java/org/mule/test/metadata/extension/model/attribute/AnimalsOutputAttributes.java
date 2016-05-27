/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.model.attribute;

public class AnimalsOutputAttributes implements AbstractOutputAttributes
{

    private String outputId = "AnimalsOutputAttributes";
    private String kind;

    public String getKind()
    {
        return kind;
    }

    public void setKind(String kind)
    {
        this.kind = kind;
    }

    public String getOutputId()
    {
        return outputId;
    }
}
