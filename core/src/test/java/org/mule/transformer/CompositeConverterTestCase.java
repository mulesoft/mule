/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
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
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.MimeTypes;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.simple.ObjectToString;

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
    public void isSourceTypeSupported()
    {
        Converter converter = mock(Converter.class);
        when(converter.isSourceTypeSupported(String.class)).thenReturn(true);
        CompositeConverter chain = new CompositeConverter(converter);

        assertTrue(chain.isSourceTypeSupported(String.class));
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
    public void getSourceTypes()
    {
        Class<?>[] dataTypes = new Class<?>[] {String.class};
        Converter converter = mock(Converter.class);
        when(converter.getSourceTypes()).thenReturn(Arrays.asList(dataTypes));
        CompositeConverter chain = new CompositeConverter(converter);

        assertEquals(String.class, chain.getSourceTypes().get(0));
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
    public void setReturnClass()
    {
        Converter converter = mock(Converter.class);
        CompositeConverter chain = new CompositeConverter(converter);
        chain.setReturnClass(String.class);

        verify(converter, atLeastOnce()).setReturnClass(String.class);
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
    public void getReturnClass()
    {
        doReturn(String.class).when(mockConverterB).getReturnClass();
        CompositeConverter compositeConverter = new CompositeConverter(mockConverterA, mockConverterB);

        assertEquals(String.class, compositeConverter.getReturnClass());
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
        doReturn(message).when(event).getMessage();

        compositeConverter.process(event);

        verify(message, times(1)).applyTransformers(event, compositeConverter);
    }

    @Test
    public void equalsReturnsTrueOnCompositeConvertersWithSameNameAndSameTransformationChain() {
        Converter byteArrayToObjectConverter = new ByteArrayToObject();
        Converter byteArrayToInputStreamConverter = new ObjectToString();
        CompositeConverter compositeConverterA = new CompositeConverter(byteArrayToObjectConverter, byteArrayToInputStreamConverter);
        CompositeConverter compositeConverterB = new CompositeConverter(byteArrayToObjectConverter, byteArrayToInputStreamConverter);

        assertThat(compositeConverterA, equalTo(compositeConverterB));
    }


    @Test
    public void equalsReturnsFalseOnCompositeConvertersWithDifferentTransformationChain() {
        Converter byteArrayToObjectConverter = new ByteArrayToObject();
        Converter byteArrayToInputStreamConverter = new ObjectToString();
        CompositeConverter compositeConverterA = new CompositeConverter(byteArrayToObjectConverter, byteArrayToInputStreamConverter);
        CompositeConverter compositeConverterB = new CompositeConverter(byteArrayToInputStreamConverter, byteArrayToObjectConverter);

        assertThat(compositeConverterA, not(equalTo(compositeConverterB)));
    }

    @Test
    public void hashCodeForCompositeConvertersChangesWithDifferentTransformationChain() {
        Converter byteArrayToObjectConverter = new ByteArrayToObject();
        Converter byteArrayToInputStreamConverter = new ObjectToString();

        Converter byteArrayToObjectConverter2 = new ByteArrayToObject();
        Converter byteArrayToInputStreamConverter2 = new ObjectToString();
        Converter byteArrayToSerializableConverter = new ByteArrayToSerializable();

        int hashCodeConverterA = new CompositeConverter(byteArrayToObjectConverter, byteArrayToInputStreamConverter).hashCode();

        int hashCodeConverterAClone = new CompositeConverter(byteArrayToObjectConverter, byteArrayToInputStreamConverter).hashCode();
        int hashCodeConverterAnotherClone =
                new CompositeConverter(byteArrayToObjectConverter2, byteArrayToInputStreamConverter2).hashCode();

        int hashCodeConverterReverseClone =
                new CompositeConverter(byteArrayToInputStreamConverter, byteArrayToObjectConverter).hashCode();
        int hashCodeConverterNotAClone =
                new CompositeConverter(byteArrayToInputStreamConverter, byteArrayToSerializableConverter).hashCode();

        assertThat(Arrays.asList(hashCodeConverterAClone), everyItem(equalTo(hashCodeConverterA)));
        assertThat(Arrays.asList(hashCodeConverterReverseClone, hashCodeConverterNotAClone, hashCodeConverterAnotherClone),
                everyItem(not(equalTo(hashCodeConverterA))));
    }
}
