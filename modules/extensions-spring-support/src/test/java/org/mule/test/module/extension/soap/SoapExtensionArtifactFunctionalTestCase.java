/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.services.soap.TestHttpSoapServer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.soap.services.FootballService;
import org.mule.test.module.extension.soap.services.LaLigaService;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public abstract class SoapExtensionArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  private static final String SOAP_CONFIG = "soap-football-extension-config.xml";

  @Rule
  public DynamicPort footballPort = new DynamicPort("footballPort");

  @Rule
  public DynamicPort laLigaPort = new DynamicPort("laLigaPort");

  private final TestHttpSoapServer footballService = new TestHttpSoapServer(footballPort.getNumber(), new FootballService());
  private final TestHttpSoapServer laLigaService = new TestHttpSoapServer(laLigaPort.getNumber(), new LaLigaService());

  @Rule
  public SystemProperty systemProperty = new SystemProperty("footballAddress", footballService.getDefaultAddress());

  @Rule
  public SystemProperty laLigaProp = new SystemProperty("laLigaAddress", laLigaService.getDefaultAddress());

  @Override
  protected void doSetUp() throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    footballService.init();
    laLigaService.init();
  }

  @Override
  protected String getConfigFile() {
    return SOAP_CONFIG;
  }

  String getBodyXml(String tagName, String content) {
    String ns = "http://services.soap.extension.module.test.mule.org/";
    return String.format("<con:%s xmlns:con=\"%s\">%s</con:%s>", tagName, ns, content, tagName);
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    footballService.stop();
    laLigaService.stop();
  }

}
