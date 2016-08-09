/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import org.mule.extension.http.internal.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;

/**
 * Needs to access resources for validating different TLS scenarios and it cannot be done with
 * {@link org.mule.functional.junit4.MuleArtifactFunctionalTestCase}. Therefore this one has to be a
 * {@link ExtensionFunctionalTestCase}.
 * <p/>
 * Mostly the scenarios are about changing the {@code tls.properties} so it is modified by tests and that cannot be done with an
 * isolated class loader.
 */
public abstract class AbstractTlsRestrictedProtocolsAndCiphersTestCase extends ExtensionFunctionalTestCase {

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

}
