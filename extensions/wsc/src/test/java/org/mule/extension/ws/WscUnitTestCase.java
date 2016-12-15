/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import org.mule.extension.ws.service.Simple11Service;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.tck.junit4.rule.DynamicPort;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class WscUnitTestCase {

  @ClassRule
  public static DynamicPort operationsPort = new DynamicPort("operationsPort");

  @ClassRule
  public static WebServiceRule service = new WebServiceRule(operationsPort.getValue(), "/test", new Simple11Service());

  @Rule
  public ExpectedException exception = ExpectedException.none();

  protected WsdlIntrospecter introspecter;
  protected XmlTypeLoader loader;

  @Before
  public void setup() {
    introspecter = new WsdlIntrospecter(service.getAddress() + "?wsdl", "TestService", "TestPort");
    loader = new XmlTypeLoader(introspecter.getSchemas());
  }
}
