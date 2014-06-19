/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

/**
 * @since 3.5.0
 */
public class NamedStageNameSource implements StageNameSource
{

    private final String ownerName;
    private final String stageName;

    public NamedStageNameSource(String ownerName, String stageName)
    {
        this.ownerName = ownerName;
        this.stageName = stageName;
    }

    @Override
    public String getName()
    {
        return String.format("%s.%s", this.ownerName, this.stageName);
    }
}
