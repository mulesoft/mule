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
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.MimeTypes;

import java.util.Arrays;

import org.junit.Test;

@SmallTest
public class CompositeTransformerTestCase
{
    private Transformer mockTransformerA = mock(Transformer.class);
    private Transformer mockTransformerB = mock(Transformer.class);
    private CompositeTransformer chain;

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyCompositeTransformer() throws Exception
    {
        new CompositeTransformer();
    }

    @Test
    public void isSourceTypeSupported()
    {
        Transformer mockTransformer = mock(Transformer.class);
        when(mockTransformer.isSourceTypeSupported(String.class)).thenReturn(true);
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);

        assertTrue(chain.isSourceTypeSupported(String.class));
    }

    @Test
    public void isSourceDataTypeSupported()
    {
        Transformer mockTransformer = mock(Transformer.class);
        when(mockTransformer.isSourceDataTypeSupported(DataType.STRING_DATA_TYPE)).thenReturn(true);
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);

        assertTrue(chain.isSourceDataTypeSupported(DataType.STRING_DATA_TYPE));
    }

    @Test
    public void getSourceTypes()
    {
        Class<?>[] dataTypes = new Class<?>[] {String.class};
        Transformer mockTransformer = mock(Transformer.class);
        when(mockTransformer.getSourceTypes()).thenReturn(Arrays.asList(dataTypes));
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);

        assertEquals(String.class, chain.getSourceTypes().get(0));
    }

    @Test
    public void getSourceDataTypes()
    {
        DataType<?>[] dataTypes = new DataType<?>[] {DataType.STRING_DATA_TYPE};
        Transformer mockTransformer = mock(Transformer.class);
        when(mockTransformer.getSourceDataTypes()).thenReturn(Arrays.asList(dataTypes));
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);

        assertEquals(DataType.STRING_DATA_TYPE, chain.getSourceDataTypes().get(0));
    }

    @Test
    public void isAcceptNull()
    {
        Transformer mockTransformer = mock(Transformer.class);
        when(mockTransformer.isAcceptNull()).thenReturn(true);
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);

        assertTrue(chain.isAcceptNull());
    }

    @Test
    public void isIgnoreBadInput()
    {
        Transformer mockTransformer = mock(Transformer.class);
        when(mockTransformer.isIgnoreBadInput()).thenReturn(true);
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);

        assertTrue(chain.isIgnoreBadInput());
    }

    @Test
    public void setReturnClass()
    {
        Transformer mockTransformer = mock(Transformer.class);
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);
        chain.setReturnClass(String.class);

        verify(mockTransformer, atLeastOnce()).setReturnClass(String.class);
    }

    @Test
    public void setReturnDataType()
    {
        Transformer mockTransformer = mock(Transformer.class);
        CompositeTransformer chain = new CompositeTransformer(mockTransformer);
        chain.setReturnDataType(DataType.STRING_DATA_TYPE);

        verify(mockTransformer, atLeastOnce()).setReturnDataType(DataType.STRING_DATA_TYPE);
    }

    @Test
    public void getReturnClass()
    {
        doReturn(String.class).when(mockTransformerB).getReturnClass();
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        assertEquals(String.class, chain.getReturnClass());
    }

    @Test
    public void getReturnDataType()
    {
        doReturn(DataType.STRING_DATA_TYPE).when(mockTransformerB).getReturnDataType();
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        assertEquals(DataType.STRING_DATA_TYPE, chain.getReturnDataType());
    }

    @Test
    public void getMimeType()
    {
        doReturn(MimeTypes.APPLICATION_XML).when(mockTransformerB).getMimeType();

        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        assertEquals(MimeTypes.APPLICATION_XML, chain.getMimeType());
    }

    @Test
    public void getEncoding()
    {
        doReturn("UTF-8").when(mockTransformerB).getEncoding();

        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        assertEquals("UTF-8", chain.getEncoding());
    }

    @Test
    public void allowsNullReturn()
    {
        doReturn(true).when(mockTransformerA).isAllowNullReturn();

        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        assertEquals(true, chain.isAllowNullReturn());
    }

    @Test
    public void doesNotAllowsNullReturn()
    {
        doReturn(false).when(mockTransformerA).isAllowNullReturn();

        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        assertEquals(false, chain.isAllowNullReturn());
    }

    @Test
    public void getEndpoint()
    {
        ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
        doReturn(mockImmutableEndpoint).when(mockTransformerA).getEndpoint();
        doReturn(mockImmutableEndpoint).when(mockTransformerB).getEndpoint();
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        assertEquals(mockImmutableEndpoint, chain.getEndpoint());
    }

    @Test
    public void setEndpoint()
    {
        ImmutableEndpoint mockImmutableEndpoint = mock(ImmutableEndpoint.class);
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        chain.setEndpoint(mockImmutableEndpoint);

        verify(mockTransformerA, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
        verify(mockTransformerB, atLeastOnce()).setEndpoint(mockImmutableEndpoint);
    }

    @Test
    public void initialise() throws Exception
    {
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        chain.initialise();

        verify(mockTransformerA, atLeastOnce()).initialise();
        verify(mockTransformerB, atLeastOnce()).initialise();
    }

    @Test
    public void dispose() throws Exception
    {
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        chain.dispose();

        verify(mockTransformerA, atLeastOnce()).dispose();
        verify(mockTransformerB, atLeastOnce()).dispose();
    }

    @Test
    public void setMuleContext()
    {
        MuleContext mockMuleContext = mock(MuleContext.class);
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        chain.setMuleContext(mockMuleContext);

        verify(mockTransformerA, atLeastOnce()).setMuleContext(mockMuleContext);
        verify(mockTransformerB, atLeastOnce()).setMuleContext(mockMuleContext);
    }

    @Test
    public void transform() throws Exception
    {
        doReturn("MyOutput1").when(mockTransformerA).transform(any());
        doReturn("UTF-8").when(mockTransformerA).getEncoding();
        doReturn("MyOutput2").when(mockTransformerB).transform(eq("MyOutput1"), eq("UTF-8"));
        doReturn("UTF-8").when(mockTransformerB).getEncoding();
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);

        Object output = chain.transform("MyInput");

        verify(mockTransformerA, times(1)).transform("MyInput");
        verify(mockTransformerB, times(1)).transform("MyOutput1", "UTF-8");
        assertEquals("MyOutput2", output);
    }

    @Test
    public void appliesTransformerChainOnMessage() throws Exception
    {
        CompositeTransformer chain = new CompositeTransformer(mockTransformerA, mockTransformerB);
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage message = mock(MuleMessage.class);
        doReturn(message).when(event).getMessage();

        chain.process(event);

        verify(message, times(1)).applyTransformers(event, chain);
    }
}
