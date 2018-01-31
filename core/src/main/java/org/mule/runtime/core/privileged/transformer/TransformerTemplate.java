/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.nio.charset.Charset;

public final class TransformerTemplate extends AbstractMessageTransformer {

  private TransformerCallback callback;

  public TransformerTemplate(TransformerCallback callback) {
    this.callback = callback;
  }

  @Override
  public Object transformMessage(CoreEvent event, Charset outputEncoding) throws MessageTransformerException {
    try {
      return callback.doTransform(event.getMessage());
    } catch (MessageTransformerException e) {
      throw e;
    } catch (TransformerException e) {
      throw new MessageTransformerException(e.getI18nMessage(), this, e, event.getMessage());
    } catch (Exception e) {
      throw new MessageTransformerException(this, e, event.getMessage());
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
