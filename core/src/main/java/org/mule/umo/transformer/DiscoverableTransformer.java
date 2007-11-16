/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.transformer;

/**
 * A interface to denote that a transformer is discoverable. A Transformer can implement this interface so that
 * when a transformation is being 'discovered' for a payload type the transformers implementing this interface
 * will be included in the search. A 'priorityWeighting property is introduced with this interface that can be used
 * to help select a transformer when there are two or more matches. The transformer with the highest priorityWeighting
 * will be selected.
 */
public interface DiscoverableTransformer
{
    public static final int MAX_PRIORITY_WEIGHTING = 10;
    public static final int MIN_PRIORITY_WEIGHTING = 1;
    public static final int DEFAULT_PRIORITY_WEIGHTING = MIN_PRIORITY_WEIGHTING;

    /**
     * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
     *
     * @return the priority weighting for this transformer. This is a value between
     *         {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    public int getPriorityWeighting();

    /**
     * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
     *
     * @param weighting the priority weighting for this transformer. This is a value between
     *                  {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    public void setPriorityWeighting(int weighting);
}
