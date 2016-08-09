/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport.service;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;

import java.nio.charset.Charset;

public class DefaultEndpointAwareMessageTransformer extends DefaultEndpointAwareTransformer implements MessageTransformer {

  public DefaultEndpointAwareMessageTransformer(MessageTransformer transformer, Charset defaultEncoding) {
    super(transformer, defaultEncoding);
  }

  @Override
  public Object transform(Object src, MuleEvent event) throws TransformerMessagingException {
    return ((MessageTransformer) transformer).transform(src, event);
  }

  @Override
  public Object transform(Object src, Charset encoding, MuleEvent event) throws TransformerMessagingException {
    return ((MessageTransformer) transformer).transform(src, encoding, event);
  }

}
