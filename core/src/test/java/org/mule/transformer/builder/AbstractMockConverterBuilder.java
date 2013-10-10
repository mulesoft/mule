/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    public Converter build()
    {
        Transformer converter = super.build();
        doReturn(weight).when((Converter) converter).getPriorityWeighting();

        return (Converter) converter;
    }

    @Override
    protected Class<? extends Transformer> getClassToMock()
    {
        return Converter.class;
    }
}
