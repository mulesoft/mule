/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.api.Closeable;
import org.mule.api.MuleContext;
import org.mule.api.util.StreamCloser;
import org.mule.streaming.Consumer;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.transform.sax.SAXSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.InputSource;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStreamCloserServiceTestCase
{

    private DefaultStreamCloserService service;

    private MuleContext muleContext;

    @Before
    public void setUp()
    {
        this.muleContext = Mockito.mock(MuleContext.class, Mockito.RETURNS_DEEP_STUBS);

        this.service = new DefaultStreamCloserService();
        this.service.setMuleContext(this.muleContext);
    }

    @Test
    public void closeCoreTypes() throws Exception
    {
        Collection<Object> closeableMocks = new ArrayList<Object>();
        InputStream in = Mockito.mock(ByteArrayInputStream.class);
        closeableMocks.add(in);

        InputSource inputSource1 = Mockito.mock(InputSource.class);
        InputStream byteStream = Mockito.mock(InputStream.class);
        Mockito.when(inputSource1.getByteStream()).thenReturn(byteStream);
        closeableMocks.add(inputSource1);

        InputSource inputSource2 = Mockito.mock(InputSource.class);
        Reader reader = Mockito.mock(Reader.class);
        Mockito.when(inputSource2.getCharacterStream()).thenReturn(reader);
        closeableMocks.add(inputSource2);

        SAXSource sax1 = Mockito.mock(SAXSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(sax1.getInputSource()).thenReturn(inputSource1);
        closeableMocks.add(sax1);

        SAXSource sax2 = Mockito.mock(SAXSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(sax2.getInputSource()).thenReturn(inputSource2);
        closeableMocks.add(sax2);

        Closeable closeable = Mockito.mock(Consumer.class);
        closeableMocks.add(closeable);

        java.io.Closeable doNotClose = Mockito.mock(java.io.Closeable.class);
        closeableMocks.add(doNotClose);

        Mockito.when(this.muleContext.getRegistry().lookupObjects(StreamCloser.class)).thenReturn(
            new ArrayList<StreamCloser>());

        for (Object stream : closeableMocks)
        {
            this.service.closeStream(stream);
        }

        Mockito.verify(in).close();

        // expect twice. Once for the InputSource and another for the SAXSource
        Mockito.verify(byteStream, Mockito.times(2)).close();
        Mockito.verify(reader, Mockito.times(2)).close();

        Mockito.verify(closeable).close();
        Mockito.verify(doNotClose, Mockito.never()).close();
    }

    @Test
    public void customCloser() throws Exception
    {
        java.io.Closeable closeable = Mockito.mock(java.io.Closeable.class);

        StreamCloser closer = Mockito.mock(StreamCloser.class);
        Mockito.when(closer.canClose(closeable.getClass())).thenReturn(true);
        Mockito.when(this.muleContext.getRegistry().lookupObjects(StreamCloser.class)).thenReturn(
            Arrays.asList(closer));

        this.service.closeStream(closeable);

        Mockito.verify(closer).close(closeable);
    }
}
