/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.nio.charset.Charset;

/** TODO */
public class TransformerTemplate extends AbstractMessageTransformer {

  private TransformerCallback callback;

  public TransformerTemplate(TransformerCallback callback) {
    this.callback = callback;
  }

  @Override
  public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
    try {
      return callback.doTransform(event.getMessage());
    } catch (TransformerException e) {
      throw new TransformerException(e.getI18nMessage(), this, e);
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }

  public interface TransformerCallback {

    Object doTransform(Message message) throws Exception;
  }

  public static class OverwitePayloadCallback implements TransformerCallback {

    private Object payload;

    public OverwitePayloadCallback(Object payload) {
      this.payload = payload;
    }

    @Override
    public Object doTransform(Message message) throws Exception {
      return payload;
    }
  }
}
