/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 3.5.0
 */
public class SequentialStageNameSource implements StageNameSource
{

    private final String ownerName;
    private final AtomicInteger asyncCount = new AtomicInteger(0);

    public SequentialStageNameSource(String ownerName)
    {
        this.ownerName = ownerName;
    }

    @Override
    public String getName()
    {
        return String.format("%s.%s", this.ownerName, this.asyncCount.addAndGet(1));
    }
}
