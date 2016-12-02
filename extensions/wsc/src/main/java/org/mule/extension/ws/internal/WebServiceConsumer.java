/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;

import org.mule.extension.ws.api.exception.WscErrors;
import org.mule.extension.ws.api.security.SecurityStrategy;
import org.mule.extension.ws.api.security.WssDecryptSecurityStrategy;
import org.mule.extension.ws.api.security.WssEncryptSecurityStrategy;
import org.mule.extension.ws.api.security.WssSignSecurityStrategy;
import org.mule.extension.ws.api.security.WssTimestampSecurityStrategy;
import org.mule.extension.ws.api.security.WssUsernameTokenSecurityStrategy;
import org.mule.extension.ws.api.security.WssVerifySignatureSecurityStrategy;
import org.mule.extension.ws.internal.connection.WscConnectionProvider;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.inject.Inject;

/**
 * Web Service Consumer extension used to consume SOAP web services.
 *
 * @since 4.0
 */
@ErrorTypes(WscErrors.class)
@Operations(ConsumeOperation.class)
@ConnectionProviders(WscConnectionProvider.class)
@SubTypeMapping(baseType = SecurityStrategy.class,
    subTypes = {WssDecryptSecurityStrategy.class, WssEncryptSecurityStrategy.class, WssSignSecurityStrategy.class,
        WssUsernameTokenSecurityStrategy.class, WssTimestampSecurityStrategy.class, WssVerifySignatureSecurityStrategy.class})
@Extension(name = "Web Service Consumer")
@Xml(namespace = "wsc")
public class WebServiceConsumer implements Initialisable {

  @Inject
  private MuleContext muleContext;

  /**
   * Default character encoding to be used in all the messages. If not specified, the default charset in the mule configuration
   * will be used
   */
  @Parameter
  @Optional
  private String encoding;

  public String getEncoding() {
    return encoding;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (encoding == null) {
      encoding = muleContext.getConfiguration().getDefaultEncoding();
    }
  }
}
