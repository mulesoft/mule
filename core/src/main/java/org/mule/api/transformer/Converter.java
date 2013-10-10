/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transformer;

/**
 * Defines a {@link Transformer} that is a data type converters, ie: convert
 * data from a type to another without modifying the meaning of the data.
 */
public interface Converter extends Transformer
{

    int MAX_PRIORITY_WEIGHTING = 10;
    int MIN_PRIORITY_WEIGHTING = 1;
    int DEFAULT_PRIORITY_WEIGHTING = MIN_PRIORITY_WEIGHTING;

    /**
     * If two or more discoverable transformers are equal, this value can be
     * used to select the correct one
     *
     * @return the priority weighting for this transformer. This is a value between
     *         {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    int getPriorityWeighting();

    /**
     * If 2 or more discoverable transformers are equal, this value can be used
     * to select the correct one
     *
     * @param weighting the priority weighting for this transformer. This is a value between
     *                  {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    void setPriorityWeighting(int weighting);

}
