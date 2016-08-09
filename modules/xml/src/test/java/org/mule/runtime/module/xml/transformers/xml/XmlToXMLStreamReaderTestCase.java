/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.module.xml.transformer.XmlToDomDocument;
import org.mule.runtime.module.xml.transformer.XmlToXMLStreamReader;
import org.mule.runtime.module.xml.util.XMLUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.Test;

public class XmlToXMLStreamReaderTestCase extends AbstractXmlTransformerTestCase {

  private static final String TEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>TEST_MESSAGE</test>";

  @Override
  public Transformer getTransformer() throws Exception {
    return createObject(XmlToXMLStreamReader.class);
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    Transformer t = createObject(XmlToDomDocument.class);
    t.setReturnDataType(DataType.STRING);
    return t;
  }

  @Override
  public Object getTestData() {
    return TEST_XML;
  }

  @Override
  public Object getResultData() {
    try {
      return XMLUtils.toXMLStreamReader(XMLInputFactory.newFactory(), TEST_XML);
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void supportsOutputHandlerAsSourceType() throws Exception {
    OutputHandler outputHandler = (event, out) -> out.write(TEST_XML.getBytes());
    XMLStreamReader result = (XMLStreamReader) getTransformer().transform(outputHandler);
    compareResults(getResultData(), result);
  }
}
