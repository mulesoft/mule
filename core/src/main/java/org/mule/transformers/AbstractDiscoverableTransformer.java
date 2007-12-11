/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers;

import org.mule.umo.transformer.DiscoverableTransformer;

public abstract class AbstractDiscoverableTransformer
        extends AbstractTransformer implements DiscoverableTransformer
{

    private int priorityWeighting = DEFAULT_PRIORITY_WEIGHTING;

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int weighting)
    {
        priorityWeighting = weighting;
    }

}
