/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.builder;

import static org.mockito.Mockito.doReturn;

import org.mule.api.transformer.Converter;
import org.mule.api.transformer.Transformer;

public abstract class AbstractMockConverterBuilder<T extends AbstractMockConverterBuilder<T>> extends AbstractMockTransformerBuilder<T>
{

    private int weight;

    public T weighting(int weight)
    {
        this.weight = weight;
        return getThis();
    }

    @Override
    public Transformer build()
    {
        Transformer converter = super.build();
        doReturn(weight).when((MockConverter) converter).getPriorityWeighting();

        return converter;
    }

    @Override
    protected Class<? extends Transformer> getClassToMock()
    {
        return MockConverter.class;
    }

    private interface MockConverter extends Transformer, Converter
    {

    }
}
