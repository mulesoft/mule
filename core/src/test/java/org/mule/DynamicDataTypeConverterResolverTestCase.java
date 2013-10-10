/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.types.DataTypeFactory;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class DynamicDataTypeConverterResolverTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext = mock(MuleContext.class);
    private MuleRegistry muleRegistry = mock(MuleRegistry.class);

    @Test
    public void doesNotFailWhenThereIsNoDataTypeResolution() throws TransformerException
    {
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(muleRegistry.lookupTransformer(Mockito.any(DataType.class), Mockito.any(DataType.class))).thenReturn(null);

        DynamicDataTypeConversionResolver resolver = new DynamicDataTypeConversionResolver(muleContext);

        List<DataType<?>> targetTypes = new ArrayList<DataType<?>>();
        targetTypes.add(DataTypeFactory.STRING);
        Transformer resolvedConverter = resolver.resolve(DataTypeFactory.INPUT_STREAM, targetTypes);

        assertEquals(null, resolvedConverter);
    }

    @Test
    public void findsExpectedConverter() throws TransformerException
    {
        Transformer expectedConverter = new MockConverterBuilder().from(DataTypeFactory.BYTE_ARRAY).to(DataTypeFactory.STRING).build();

        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(muleRegistry.lookupTransformer(DataTypeFactory.BYTE_ARRAY, DataTypeFactory.STRING)).thenReturn(expectedConverter);

        DynamicDataTypeConversionResolver resolver = new DynamicDataTypeConversionResolver(muleContext);

        List<DataType<?>> targetTypes = new ArrayList<DataType<?>>();
        targetTypes.add(DataTypeFactory.INPUT_STREAM);
        targetTypes.add(DataTypeFactory.STRING);
        Transformer resolvedConverter = resolver.resolve(DataTypeFactory.BYTE_ARRAY, targetTypes);

        assertEquals(expectedConverter, resolvedConverter);
    }
}
