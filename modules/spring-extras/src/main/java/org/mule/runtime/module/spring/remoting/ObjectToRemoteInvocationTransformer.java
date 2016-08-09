/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.remoting;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;

import org.springframework.remoting.support.RemoteInvocation;

/**
 * Transforms a byte[] into an ObjectInputStream and then into a Spring RemoteInvocation instance.
 */
public class ObjectToRemoteInvocationTransformer extends AbstractTransformer {

  public ObjectToRemoteInvocationTransformer() {
    super();
    this.registerSourceType(DataType.fromType(RemoteInvocation.class));
    this.registerSourceType(DataType.BYTE_ARRAY);
    this.registerSourceType(DataType.INPUT_STREAM);
    this.setReturnDataType(DataType.fromType(RemoteInvocation.class));
  }

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    if (src instanceof RemoteInvocation) {
      return src;
    }

    Object o = null;

    if (src instanceof InputStream) {
      try {
        o = new ObjectInputStream((InputStream) src).readObject();
      } catch (Exception e) {
        throw new TransformerException(this, e);
      }
    } else {
      byte[] data = (byte[]) src;
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      try {
        ObjectInputStream ois = new ObjectInputStream(bais);
        o = ois.readObject();
      } catch (Exception e) {
        throw new TransformerException(this, e);
      }
    }

    RemoteInvocation ri = (RemoteInvocation) o;
    if (logger.isDebugEnabled()) {
      logger.debug("request to execute " + ri.getMethodName());
      for (int i = 0; i < ri.getArguments().length; i++) {
        Object currentArgument = ri.getArguments()[i];

        StringBuilder buf = new StringBuilder(64);
        buf.append("with argument (");
        buf.append(currentArgument == null ? "<null>" : currentArgument.toString());
        buf.append(")");

        logger.debug(buf.toString());
      }
    }
    return ri;
  }

}
