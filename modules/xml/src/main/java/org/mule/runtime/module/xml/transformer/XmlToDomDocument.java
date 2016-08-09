/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.module.xml.util.XMLUtils;

import java.nio.charset.Charset;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.dom4j.io.DocumentResult;
import org.dom4j.io.SAXContentHandler;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;

/**
 * <code>XmlToDomDocument</code> transforms a XML String to org.w3c.dom.Document.
 */
public class XmlToDomDocument extends AbstractXmlTransformer implements DiscoverableTransformer {

  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

  @Override
  public Object transformMessage(MuleEvent event, Charset encoding) throws TransformerException {
    Object src = event.getMessage().getPayload();
    try {
      Source sourceDoc = XMLUtils.toXmlSource(getXMLInputFactory(), isUseStaxSource(), src);
      if (sourceDoc == null) {
        return null;
      }

      if (XMLStreamReader.class.isAssignableFrom(getReturnDataType().getType())) {
        return getXMLInputFactory().createXMLStreamReader(sourceDoc);
      } else if (getReturnDataType().getType().isAssignableFrom(sourceDoc.getClass())) {
        return sourceDoc;
      }

      // If returnClass is not set, assume W3C DOM
      // This is the original behaviour
      ResultHolder holder = getResultHolder(getReturnDataType().getType());
      if (holder == null) {
        holder = getResultHolder(Document.class);
      }

      Result result = holder.getResult();

      if (result instanceof DocumentResult) {
        DocumentResult dr = (DocumentResult) holder.getResult();
        ContentHandler contentHandler = dr.getHandler();
        if (contentHandler instanceof SAXContentHandler) {
          // The following code is used to avoid the splitting
          // of text inside DOM elements.
          ((SAXContentHandler) contentHandler).setMergeAdjacentText(true);
        }
      }

      Transformer idTransformer = XMLUtils.getTransformer();
      idTransformer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
      idTransformer.transform(sourceDoc, holder.getResult());

      return holder.getResultObject();
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
