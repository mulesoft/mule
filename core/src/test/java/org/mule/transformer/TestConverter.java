/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import org.mule.api.transformer.DiscoverableTransformer;

public class TestConverter extends org.mule.transformer.TestTransformer implements DiscoverableTransformer
{

    private int weight;

    @Override
    public int getPriorityWeighting()
    {
        return weight;
    }

    @Override
    public void setPriorityWeighting(int weighting)
    {
        this.weight = weighting;
    }
}
