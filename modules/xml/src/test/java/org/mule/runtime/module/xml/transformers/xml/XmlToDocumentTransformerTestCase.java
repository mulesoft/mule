/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.xml.transformers.xml;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.module.xml.transformer.DomDocumentToXml;
import org.mule.runtime.module.xml.transformer.XmlToDomDocument;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;

public class XmlToDocumentTransformerTestCase extends AbstractXmlTransformerTestCase {

  private static final String TEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>TEST_MESSAGE</test>";

  @Override
  public Transformer getTransformer() throws Exception {
    return createObject(XmlToDomDocument.class);
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return createObject(DomDocumentToXml.class);
  }

  @Override
  public Object getTestData() {
    return TEST_XML;
  }

  @Override
  public Object getResultData() {
    try {
      return new DOMWriter().write(DocumentHelper.parseText(TEST_XML));
    } catch (DocumentException e) {
      throw new RuntimeException(e);
    }
  }

}
