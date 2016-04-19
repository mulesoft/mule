/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.transformer.types.MimeTypes;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;

import org.junit.Test;

@SmallTest
public class CompositeConverterTestCase
{
    private Converter mockConverterA = mock(Converter.class);
    private Converter mockConverterB = mock(Converter.class);

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyCompositeTransformer() throws Exception
    {
        new CompositeConverter();
    }

    @Test
    public void isSourceDataTypeSupported()
    {
        Converter converter = mock(Converter.class);
        when(converter.isSourceDataTypeSupported(DataType.STRING_DATA_TYPE)).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(converter);

        assertTrue(chain.isSourceDataTypeSupported(DataType.STRING_DATA_TYPE));
    }

    @Test
    public void getSourceDataTypes()
    {
        DataType<?>[] dataTypes = new DataType<?>[] {DataType.STRING_DATA_TYPE};
        Converter converter = mock(Converter.class);
        when(converter.getSourceDataTypes()).thenReturn(Arrays.asList(dataTypes));
        CompositeConverter chain = new CompositeConverter(converter);

        assertEquals(DataType.STRING_DATA_TYPE, chain.getSourceDataTypes().get(0));
    }

    @Test
    public void isAcceptNull()
    {
        Converter converter = mock(Converter.class);
        when(converter.isAcceptNull()).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(converter);

        assertTrue(chain.isAcceptNull());
    }

    @Test
    public void isIgnoreBadInput()
    {
        Converter converter = mock(Converter.class);
        when(converter.isIgnoreBadInput()).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(converter);

        assertTrue(chain.isIgnoreBadInput());
    }

    @Test
    public void setReturnDataType()
    {
        Converter converter = mock(Converter.class);
        CompositeConverter chain = new CompositeConverter(converter);
        chain.setReturnDataType(DataType.STRING_DATA_TYPE);

        verify(converter, atLeastOnce()).setReturnDataType(DataType.STRING_DATA_TYPE);
    }

    @Test
    public void getReturnDataType()
    {
        doReturn(DataType.STRING_DATA_TYPE).when(mockConverterB).getReturnDataType();
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(DataType.STRING_DATA_TYPE, compositeConverter.getReturnDataType());
    }

    @Test
    public void getMimeType()
    {
        doReturn(MimeTypes.APPLICATION_XML).when(mockConverterB).getMimeType();

        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(MimeTypes.APPLICATION_XML, compositeConverter.getMimeType());
    }

    @Test
    public void getEncoding()
    {
        doReturn("UTF-8").when(mockConverterB).getEncoding();

        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals("UTF-8", compositeConverter.getEncoding());
    }


    @Test
    public void getEndpoint()
    {
        ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
        doReturn(mockImmutableEndpoint).when(mockConverterA).getEndpoint();
        doReturn(mockImmutableEndpoint).when(mockConverterB).getEndpoint();
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(mockImmutableEndpoint, compositeConverter.getEndpoint());
    }

    @Test
    public void setEndpoint()
    {
        ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        compositeConverter.setEndpoint(mockImmutableEndpoint);

        verify(mockConverterA, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
        verify(mockConverterB, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
    }

    @Test
    public void priorityWeighting() throws Exception
    {
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);
        when(mockConverterA.getPriorityWeighting()).thenReturn(1);
        when(mockConverterB.getPriorityWeighting()).thenReturn(2);

        int priorityWeighting = compositeConverter.getPriorityWeighting();

        assertEquals(3, priorityWeighting);
    }

    @Test
    public void initialise() throws Exception
    {
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        compositeConverter.initialise();

        verify(mockConverterA, atLeastOnce()).initialise();
        verify(mockConverterB, atLeastOnce()).initialise();
    }

    @Test
    public void dispose() throws Exception
    {
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        compositeConverter.dispose();

        verify(mockConverterA, atLeastOnce()).dispose();
        verify(mockConverterB, atLeastOnce()).dispose();
    }

    @Test
    public void setMuleContext()
    {
        MuleContext mockMuleContext = mock(MuleContext.class);
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        compositeConverter.setMuleContext(mockMuleContext);

        verify(mockConverterA, atLeastOnce()).setMuleContext(mockMuleContext);
        verify(mockConverterB, atLeastOnce()).setMuleContext(mockMuleContext);
    }

    @Test
    public void transform() throws Exception
    {
        doReturn("MyOutput1").when(mockConverterA).transform(any());
        doReturn("UTF-8").when(mockConverterA).getEncoding();
        doReturn("MyOutput2").when(mockConverterB).transform(eq("MyOutput1"), eq("UTF-8"));
        doReturn("UTF-8").when(mockConverterB).getEncoding();
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        Object output = compositeConverter.transform("MyInput");

        verify(mockConverterA, times(1)).transform("MyInput");
        verify(mockConverterB, times(1)).transform("MyOutput1", "UTF-8");
        assertEquals("MyOutput2", output);
    }

    @Test
    public void appliesTransformerChainOnMessage() throws Exception
    {
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage message = mock(MuleMessage.class);
        MuleContext muleContext = mock(MuleContext.class);
        TransformationService transformationService = mock(TransformationService.class);
        doReturn(message).when(event).getMessage();
        doReturn(muleContext).when(event).getMuleContext();
        doReturn(transformationService).when(muleContext).getTransformationService();

        compositeConverter.process(event);

        verify(transformationService, times(1)).applyTransformers(event.getMessage(), event, compositeConverter);
    }
}
