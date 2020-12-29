/*
 * (c) 2003-2019 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractMessageTransformer;

public class FlowTransformer extends AbstractMessageTransformer {

  public static boolean crash;

  @Override
  public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
    if (crash) {
      throw new TransformerException(MessageFactory.createStaticMessage("FlowTransformer failed"));
    } else {
      return message;
    }
  }
}
