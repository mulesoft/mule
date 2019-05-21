/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import static org.mule.api.config.MuleProperties.MULE_MAX_ATTRIBUTE_SIZE;
import static org.mule.module.xml.util.XMLUtils.toXMLStreamReader;
import static org.mule.util.IOUtils.getResourceAsStream;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transformer.types.DataTypeFactory;

import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;

import org.junit.Rule;
import org.junit.Test;

public class DomToXmlTransformerTestCase extends AbstractMuleContextTestCase
{

  @Rule
  public final SystemProperty maxAttributeSizeProperty = new SystemProperty(MULE_MAX_ATTRIBUTE_SIZE, "2");

  @Test(expected = TransformerException.class)
  public void domToXmlWithSmallInput() throws Exception
  {
    XmlToDomDocument transformer = createObject(XmlToDomDocument.class);
    transformer.setReturnDataType(DataTypeFactory.create(org.w3c.dom.Document.class));

    InputStream is = getResourceAsStream("small.xml", XMLTestUtils.class);
    XMLStreamReader sr = toXMLStreamReader(transformer.getXMLInputFactory(), is);

    transformer.transform(sr);
  }

}
