/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.message;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.services.soap.api.message.dispatcher.HttpConfigBasedMessageDispatcher;

@Alias("http-transport-configuration")
public class CustomHttpTransportConfiguration implements CustomTransportConfiguration {

  @Parameter
  private String configName;

  @Override
  public MessageDispatcher buildDispatcher(ExtensionsClient client) {
    return new HttpConfigBasedMessageDispatcher(configName, client);
  }
}
