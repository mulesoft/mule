/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.issues;


import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import org.apache.ws.security.WSPasswordCallback;

public class ClientPasswordCallbackClientServer implements CallbackHandler
{
  public static final String CLIENT_PASSWORD = "client";
  public static final String SERVER_PASSWORD = "server";
  
  public ClientPasswordCallbackClientServer() {}
  
  public void handle(Callback[] callbacks) throws java.io.IOException, javax.security.auth.callback.UnsupportedCallbackException
  {
    for (int i = 0; i < callbacks.length; i++) {
      WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
      if ((pc.getUsage() == 3) || (pc.getUsage() == 1))
      {

        if (pc.getIdentifier().equals("client")) {
          pc.setPassword("client");
        } else if (pc.getIdentifier().equals("server")) {
          pc.setPassword("server");
        }
      }
    }
  }
}