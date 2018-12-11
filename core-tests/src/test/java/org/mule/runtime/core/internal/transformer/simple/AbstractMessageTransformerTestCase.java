/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.nio.charset.Charset;

import org.junit.Test;

public class AbstractMessageTransformerTestCase extends AbstractMuleContextTestCase {

  private static final byte[] BYTES = "transformed".getBytes();

  AbstractMessageTransformer transformer = new AbstractMessageTransformer() {

    @Override
    public Object transformMessage(CoreEvent event, Charset outputEncoding) throws MessageTransformerException {
      return BYTES;
    }

    @Override
    public DataType getReturnDataType() {
      return BYTE_ARRAY;
    }
  };

  @Test
  public void reusesDefaultFlowConstruct() throws MessageTransformerException {
    MuleContext muleContextSpy = spy(muleContext);
    transformer.setMuleContext(muleContextSpy);
    transform();
    transform();
    transform();
    verify(muleContextSpy, times(1)).getDefaultErrorHandler(any());
  }

  private void transform() throws MessageTransformerException {
    assertThat(transformer.transform(Message.of(new Object()), Charset.defaultCharset(), null), is(BYTES));
  }

}
