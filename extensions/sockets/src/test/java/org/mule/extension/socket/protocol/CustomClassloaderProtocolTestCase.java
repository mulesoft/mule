/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.protocol;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.extension.socket.SocketExtensionTestCase;
import org.mule.extension.socket.api.connection.tcp.protocol.CustomClassLoadingLengthProtocol;

import org.junit.Test;

public class CustomClassloaderProtocolTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "custom-classloader-protocol-config.xml";
  }

  @Test
  public void testClassLoader() throws Exception {

    CustomClassLoadingLengthProtocol protocol = muleContext.getRegistry().lookupObject("customClassLoaderProtocol");
    assertThat(protocol, is(not((nullValue()))));
    assertThat(protocol.getClassLoader(), sameInstance(muleContext.getRegistry().get("classLoader")));
  }

  public static class FakeClassLoader extends ClassLoader {
  }
}
