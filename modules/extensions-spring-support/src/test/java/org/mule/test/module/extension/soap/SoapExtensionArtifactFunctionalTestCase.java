/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.services.http.impl.service.HttpServiceImplementation;
import org.mule.services.soap.SoapServiceImplementation;
import org.mule.services.soap.TestHttpSoapServer;
import org.mule.services.soap.api.SoapService;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.soap.services.FootballService;
import org.mule.test.module.extension.soap.services.LaLigaService;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.List;

import org.junit.Rule;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public abstract class SoapExtensionArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  private static final String SOAP_CONFIG = "soap-football-extension-config.xml";

  private final SchedulerService schedulerService = new SimpleUnitTestSupportSchedulerService();
  private final HttpService httpService = new HttpServiceImplementation(schedulerService);
  private final SoapService soapService = new SoapServiceImplementation(httpService);

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
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    try {
      startIfNeeded(httpService);
    } catch (MuleException e) {
      // do nothing
    }
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getRegistry().registerObject(httpService.getName(), httpService);
        muleContext.getRegistry().registerObject(soapService.getName(), soapService);
      }
    });
  }

  @Override
  protected void doSetUp() throws Exception {
    footballService.init();
    laLigaService.init();
  }

  @Override
  protected String getConfigFile() {
    return SOAP_CONFIG;
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    stopIfNeeded(soapService);
    stopIfNeeded(httpService);
    stopIfNeeded(schedulerService);
    footballService.stop();
    laLigaService.stop();
  }

}
