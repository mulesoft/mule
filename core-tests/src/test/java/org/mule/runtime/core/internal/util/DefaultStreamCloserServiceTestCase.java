/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.streaming.iterator.Consumer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.transform.sax.SAXSource;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStreamCloserServiceTestCase {

  private DefaultStreamCloserService service;

  private MuleContextWithRegistries muleContext;

  @Before
  public void setUp() {
    this.muleContext = mock(MuleContextWithRegistries.class, Mockito.RETURNS_DEEP_STUBS);

    this.service = new DefaultStreamCloserService();
    this.service.setMuleContext(this.muleContext);
  }

  @Test
  public void closeCoreTypes() throws Exception {
    Collection<Object> closeableMocks = new ArrayList<>();
    InputStream in = mock(ByteArrayInputStream.class);
    closeableMocks.add(in);

    InputSource inputSource1 = mock(InputSource.class);
    InputStream byteStream = mock(InputStream.class);
    when(inputSource1.getByteStream()).thenReturn(byteStream);
    closeableMocks.add(inputSource1);

    InputSource inputSource2 = mock(InputSource.class);
    Reader reader = mock(Reader.class);
    when(inputSource2.getCharacterStream()).thenReturn(reader);
    closeableMocks.add(inputSource2);

    SAXSource sax1 = mock(SAXSource.class, Mockito.RETURNS_DEEP_STUBS);
    when(sax1.getInputSource()).thenReturn(inputSource1);
    closeableMocks.add(sax1);

    SAXSource sax2 = mock(SAXSource.class, Mockito.RETURNS_DEEP_STUBS);
    when(sax2.getInputSource()).thenReturn(inputSource2);
    closeableMocks.add(sax2);

    Closeable closeable = mock(Consumer.class);
    closeableMocks.add(closeable);

    java.io.Closeable javaClosable = mock(java.io.Closeable.class);
    closeableMocks.add(javaClosable);

    when(this.muleContext.getRegistry().lookupObjects(StreamCloser.class)).thenReturn(new ArrayList<>());

    closeableMocks.forEach(service::closeStream);

    verify(in).close();

    // expect twice. Once for the InputSource and another for the SAXSource
    verify(byteStream, times(2)).close();
    verify(reader, times(2)).close();

    verify(closeable).close();
    verify(javaClosable).close();
  }

  @Test
  public void customCloser() throws Exception {
    StreamCloser closer = mock(StreamCloser.class);
    when(closer.canClose(getClass())).thenReturn(true);
    when(muleContext.getRegistry().lookupObjects(StreamCloser.class)).thenReturn(asList(closer));

    this.service.closeStream(this);
    verify(closer).close(this);
  }
}
