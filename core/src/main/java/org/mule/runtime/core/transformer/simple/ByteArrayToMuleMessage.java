/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.serialization.SerializationProtocol;

/**
 * Deserializes a {@link org.mule.runtime.api.message.Message} stored on a byte array.
 */
public class ByteArrayToMuleMessage extends ByteArrayToSerializable {

  public ByteArrayToMuleMessage() {
    super();
    setReturnDataType(DataType.MULE_MESSAGE);
  }

  @Override
  protected SerializationProtocol getSerializationProtocol() {
    return muleContext.getObjectSerializer().getInternalProtocol();
  }
}
