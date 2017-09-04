/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.xmlsecurity;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;

public class XMLSecureFactoriesTestCase {

  @Test
  public void createsTheCorrectInstances() {
    XMLSecureFactories xmlSecureFactories = XMLSecureFactories.createDefault();

    // As there are casts inside, creation would fail if the appropriate type is not returned
    assertThat(xmlSecureFactories.getDocumentBuilderFactory(), notNullValue());
    assertThat(xmlSecureFactories.getSAXParserFactory(), notNullValue());
    assertThat(xmlSecureFactories.getXMLInputFactory(), notNullValue());
    assertThat(xmlSecureFactories.getTransformerFactory(), notNullValue());
  }

  @Test
  public void cachesXmlFactory() {
    DocumentBuilderFactory documentBuilderFactoryOne = XMLSecureFactories.createDefault().getDocumentBuilderFactory();
    DocumentBuilderFactory documentBuilderFactoryTwo = XMLSecureFactories.createDefault().getDocumentBuilderFactory();

    assertThat(documentBuilderFactoryOne, sameInstance(documentBuilderFactoryTwo));
  }

  @Test
  public void handlesDifferentConfigurations() {
    DocumentBuilderFactory insecureFactoryOne = XMLSecureFactories.createWithConfig(true, true).getDocumentBuilderFactory();
    DocumentBuilderFactory secureFactoryOne = XMLSecureFactories.createWithConfig(true, false).getDocumentBuilderFactory();
    DocumentBuilderFactory insecureFactoryTwo = XMLSecureFactories.createWithConfig(true, true).getDocumentBuilderFactory();
    DocumentBuilderFactory secureFactoryTwo = XMLSecureFactories.createWithConfig(true, false).getDocumentBuilderFactory();

    assertThat(insecureFactoryOne, sameInstance(insecureFactoryTwo));
    assertThat(secureFactoryOne, sameInstance(secureFactoryTwo));
    assertThat(insecureFactoryOne, not(sameInstance(secureFactoryOne)));
  }
}
