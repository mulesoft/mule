/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.services.http.impl.service.HttpServiceImplementation;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;

import org.junit.Rule;

/**
 * Declares a dependency on HTTP extension for integration tests cases.
 * 
 * TODO MULE-11341 change to extend MuleArtifactFunctionalTestCase
 * 
 * @since 4.0
 */
public abstract class AbstractCxfOverHttpExtensionTestCase extends ExtensionFunctionalTestCase {

  @Rule
  public SystemProperty melDefault = new SystemProperty("mule.test.mel.default", "true");

  // TODO - MULE-11119: Remove once the service is injected higher up on the hierarchy
  private SchedulerService schedulerService = new SimpleUnitTestSupportSchedulerService();
  private HttpService httpService = new HttpServiceImplementation(schedulerService);

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
      }
    });
  }

  @Override
  protected boolean mockHttpService() {
    return false;
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class[] {SocketsExtension.class, HttpConnector.class};
  }

  @Override
  protected void doTearDown() throws Exception {
    stopIfNeeded(httpService);
    stopIfNeeded(schedulerService);
    super.doTearDown();
  }
}
