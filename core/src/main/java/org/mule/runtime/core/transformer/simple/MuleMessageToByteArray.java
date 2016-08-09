/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

/** TODO */
public class MuleMessageToByteArray extends AbstractMessageTransformer {

  public MuleMessageToByteArray() {
    registerSourceType(DataType.MULE_MESSAGE);
    setReturnDataType(DataType.BYTE_ARRAY);
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) {
    return muleContext.getObjectSerializer().serialize(event.getMessage());
  }
}
