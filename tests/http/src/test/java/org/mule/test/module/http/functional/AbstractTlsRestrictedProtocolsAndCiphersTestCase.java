/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.service.http.api.HttpService;
import org.mule.services.http.impl.service.HttpServiceImplementation;

import java.util.List;

/**
 * Needs to access resources for validating different TLS scenarios and it cannot be done with
 * {@link org.mule.functional.junit4.MuleArtifactFunctionalTestCase}. Therefore this one has to be a
 * {@link ExtensionFunctionalTestCase}.
 * <p/>
 * Mostly these scenarios are about changing the {@code tls.properties} so it is modified by tests and that cannot be done with an
 * isolated class loader.
 */
public abstract class AbstractTlsRestrictedProtocolsAndCiphersTestCase extends ExtensionFunctionalTestCase {

  //TODO - MULE-11119: Remove once the service is injected higher up on the hierarchy
  private HttpService httpService = new HttpServiceImplementation();

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    try {
      startIfNeeded(httpService);
    } catch (MuleException e) {
      //do nothing
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

  /**
   * The test cannot run with isolation due to http ext doesn't have anymore the mule-module.properties. This test needs to have
   * the complete access to all the classes and resources therefore it just returns the class loader that loaded the test class.
   *
   * @return the {@link ClassLoader} that loaded the test.
   */
  @Override
  protected ClassLoader getExecutionClassLoader() {
    return this.getClass().getClassLoader();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    stopIfNeeded(httpService);
  }
}
