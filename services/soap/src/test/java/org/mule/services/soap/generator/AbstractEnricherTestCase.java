/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.generator;

import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.services.soap.AbstractSoapServiceTestCase;
import org.mule.services.soap.introspection.WsdlIntrospecter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class AbstractEnricherTestCase extends AbstractSoapServiceTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  protected WsdlIntrospecter introspecter;
  protected XmlTypeLoader loader;

  @Before
  public void setup() {
    introspecter = new WsdlIntrospecter(server.getDefaultAddress() + "?wsdl", "TestService", "TestPort");
    loader = new XmlTypeLoader(introspecter.getSchemas());
  }
}
