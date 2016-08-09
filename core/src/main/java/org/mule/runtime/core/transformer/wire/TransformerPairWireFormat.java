/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.wire;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.wire.WireFormat;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pairing of an outbound transformer and an inbound transformer that can be used to serialize and deserialize data. THis is
 * used when marshalling requests over the wire. IN Mule the MuleClient RemoteDispatcher uses wire formats to communicate with the
 * server.
 */
public class TransformerPairWireFormat implements WireFormat {

  /**
   * logger used by this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected Transformer inboundTransformer;
  protected Transformer outboundTransformer;
  protected MuleContext muleContext;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    inboundTransformer.setMuleContext(muleContext);
    outboundTransformer.setMuleContext(muleContext);
  }

  @Override
  public Object read(InputStream in) throws MuleException {
    if (inboundTransformer == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("inboundTransformer").getMessage());
    }
    if (inboundTransformer.isSourceDataTypeSupported(DataType.INPUT_STREAM)) {
      return inboundTransformer.transform(in);
    } else {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(in, baos);
        return inboundTransformer.transform(baos.toByteArray());
      } catch (IOException e) {
        throw new DefaultMuleException(CoreMessages.failedToReadPayload(), e);
      }
    }
  }

  @Override
  public void write(OutputStream out, Object o, Charset encoding) throws MuleException {
    if (outboundTransformer == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("outboundTransformer").getMessage());
    }
    try {
      Class returnClass = outboundTransformer.getReturnDataType().getType();
      if (returnClass.equals(Object.class)) {
        logger.warn("No return class was set on transformer: " + outboundTransformer
            + ". Attempting to work out how to treat the result transformation");

        Object result = outboundTransformer.transform(o);

        byte[] bytes;
        if (result instanceof byte[]) {
          bytes = (byte[]) result;
        } else {
          bytes = result.toString().getBytes(encoding);
        }

        out.write(bytes);
      } else if (returnClass.equals(byte[].class)) {
        byte[] b = (byte[]) outboundTransformer.transform(o);
        out.write(b);
      } else if (returnClass.equals(String.class)) {
        String s = (String) outboundTransformer.transform(o);
        out.write(s.getBytes(encoding));
      } else {
        throw new TransformerException(CoreMessages.transformFailedFrom(o.getClass()));
      }
    } catch (IOException e) {
      throw new TransformerException(CoreMessages.transformFailedFrom(o.getClass()), e);
    }
  }

  public Transformer getInboundTransformer() {
    return inboundTransformer;
  }

  public void setInboundTransformer(Transformer inboundTransformer) {
    this.inboundTransformer = inboundTransformer;
  }

  public Transformer getOutboundTransformer() {
    return outboundTransformer;
  }

  public void setOutboundTransformer(Transformer outboundTransformer) {
    this.outboundTransformer = outboundTransformer;
  }
}
