/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import org.mule.extension.http.internal.temp.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;

/**
 * 
 * @since 4.0
 */
public class HttpExtensionFuntionalTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class[] {SocketsExtension.class, HttpConnector.class};
  }

}
