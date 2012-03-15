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

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.MimeTypes;

import java.util.Arrays;

import org.junit.Test;

@SmallTest
public class CompositeConverterTestCase
{
    private MockConverter mockConverterA = mock(MockConverter.class);
    private MockConverter mockConverterB = mock(MockConverter.class);

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyCompositeTransformer() throws Exception
    {
        new CompositeConverter();
    }

    @Test
    public void isSourceTypeSupported()
    {
        Transformer mockTransformer = mock(MockConverter.class);
        when(mockTransformer.isSourceTypeSupported(String.class)).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(mockTransformer);

        assertTrue(chain.isSourceTypeSupported(String.class));
    }

    @Test
    public void isSourceDataTypeSupported()
    {
        Transformer mockTransformer = mock(MockConverter.class);
        when(mockTransformer.isSourceDataTypeSupported(DataType.STRING_DATA_TYPE)).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(mockTransformer);

        assertTrue(chain.isSourceDataTypeSupported(DataType.STRING_DATA_TYPE));
    }

    @Test
    public void getSourceTypes()
    {
        Class<?>[] dataTypes = new Class<?>[] {String.class};
        Transformer mockTransformer = mock(MockConverter.class);
        when(mockTransformer.getSourceTypes()).thenReturn(Arrays.asList(dataTypes));
        CompositeConverter chain = new CompositeConverter(mockTransformer);

        assertEquals(String.class, chain.getSourceTypes().get(0));
    }

    @Test
    public void getSourceDataTypes()
    {
        DataType<?>[] dataTypes = new DataType<?>[] {DataType.STRING_DATA_TYPE};
        Transformer mockTransformer = mock(MockConverter.class);
        when(mockTransformer.getSourceDataTypes()).thenReturn(Arrays.asList(dataTypes));
        CompositeConverter chain = new CompositeConverter(mockTransformer);

        assertEquals(DataType.STRING_DATA_TYPE, chain.getSourceDataTypes().get(0));
    }

    @Test
    public void isAcceptNull()
    {
        Transformer mockTransformer = mock(MockConverter.class);
        when(mockTransformer.isAcceptNull()).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(mockTransformer);

        assertTrue(chain.isAcceptNull());
    }

    @Test
    public void isIgnoreBadInput()
    {
        Transformer mockTransformer = mock(MockConverter.class);
        when(mockTransformer.isIgnoreBadInput()).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(mockTransformer);

        assertTrue(chain.isIgnoreBadInput());
    }

    @Test
    public void setReturnClass()
    {
        Transformer mockTransformer = mock(MockConverter.class);
        CompositeConverter chain = new CompositeConverter(mockTransformer);
        chain.setReturnClass(String.class);

        verify(mockTransformer, atLeastOnce()).setReturnClass(String.class);
    }

    @Test
    public void setReturnDataType()
    {
        Transformer mockTransformer = mock(MockConverter.class);
        CompositeConverter chain = new CompositeConverter(mockTransformer);
        chain.setReturnDataType(DataType.STRING_DATA_TYPE);

        verify(mockTransformer, atLeastOnce()).setReturnDataType(DataType.STRING_DATA_TYPE);
    }

    @Test
    public void getReturnClass()
    {
        doReturn(String.class).when(mockConverterB).getReturnClass();
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(String.class, chain.getReturnClass());
    }

    @Test
    public void getReturnDataType()
    {
        doReturn(DataType.STRING_DATA_TYPE).when(mockConverterB).getReturnDataType();
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(DataType.STRING_DATA_TYPE, chain.getReturnDataType());
    }

    @Test
    public void getMimeType()
    {
        doReturn(MimeTypes.APPLICATION_XML).when(mockConverterB).getMimeType();

        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(MimeTypes.APPLICATION_XML, chain.getMimeType());
    }

    @Test
    public void getEncoding()
    {
        doReturn("UTF-8").when(mockConverterB).getEncoding();

        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals("UTF-8", chain.getEncoding());
    }


    @Test
    public void getEndpoint()
    {
        ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
        doReturn(mockImmutableEndpoint).when(mockConverterA).getEndpoint();
        doReturn(mockImmutableEndpoint).when(mockConverterB).getEndpoint();
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(mockImmutableEndpoint, chain.getEndpoint());
    }

    @Test
    public void setEndpoint()
    {
        ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        chain.setEndpoint(mockImmutableEndpoint);

        verify(mockConverterA, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
        verify(mockConverterB, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
    }

    @Test
    public void priorityWeighting() throws Exception
    {
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);
        when(mockConverterA.getPriorityWeighting()).thenReturn(1);
        when(mockConverterB.getPriorityWeighting()).thenReturn(2);

        int priorityWeighting = chain.getPriorityWeighting();

        assertEquals(3, priorityWeighting);
    }

    @Test
    public void initialise() throws Exception
    {
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        chain.initialise();

        verify(mockConverterA, atLeastOnce()).initialise();
        verify(mockConverterB, atLeastOnce()).initialise();
    }

    @Test
    public void dispose() throws Exception
    {
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        chain.dispose();

        verify(mockConverterA, atLeastOnce()).dispose();
        verify(mockConverterB, atLeastOnce()).dispose();
    }

    @Test
    public void setMuleContext()
    {
        MuleContext mockMuleContext = mock(MuleContext.class);
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        chain.setMuleContext(mockMuleContext);

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
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);

        Object output = chain.transform("MyInput");

        verify(mockConverterA, times(1)).transform("MyInput");
        verify(mockConverterB, times(1)).transform("MyOutput1", "UTF-8");
        assertEquals("MyOutput2", output);
    }

    @Test
    public void appliesTransformerChainOnMessage() throws Exception
    {
        CompositeConverter chain = new CompositeConverter(mockConverterA, mockConverterB);
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage message = mock(MuleMessage.class);
        doReturn(message).when(event).getMessage();

        chain.process(event);

        verify(message, times(1)).applyTransformers(event, chain);
    }

    private interface MockConverter extends Transformer, Converter
    {

    }
}
