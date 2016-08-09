/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.nio.charset.Charset;

/**
 * <code>DomDocumentToXml</code> Transform a org.w3c.dom.Document to XML String
 */
public class DomDocumentToXml extends AbstractXmlTransformer implements DiscoverableTransformer {

  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

  public DomDocumentToXml() {
    setReturnDataType(DataType.XML_STRING);
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset encoding) throws TransformerException {
    Object src = event.getMessage().getPayload();
    try {
      // We now offer XML in byte OR String form.
      // String remains the default like before.
      if (byte[].class.equals(getReturnDataType().getType())) {
        return convertToBytes(src, encoding);
      } else {
        return convertToText(src, encoding);
      }
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int priorityWeighting) {
    this.priorityWeighting = priorityWeighting;
  }
}
