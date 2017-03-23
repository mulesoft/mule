/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;

import org.mule.extension.ws.api.exception.WscErrors;
import org.mule.extension.ws.internal.security.SecurityStrategyAdapter;
import org.mule.extension.ws.internal.security.WssDecryptSecurityStrategy;
import org.mule.extension.ws.internal.security.WssEncryptSecurityStrategy;
import org.mule.extension.ws.internal.security.WssSignSecurityStrategy;
import org.mule.extension.ws.internal.security.WssTimestampSecurityStrategy;
import org.mule.extension.ws.internal.security.WssUsernameTokenSecurityStrategy;
import org.mule.extension.ws.internal.security.WssVerifySignatureSecurityStrategy;
import org.mule.extension.ws.internal.connection.WscConnectionProvider;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Web Service Consumer extension used to consume SOAP web services.
 *
 * @since 4.0
 */
@ErrorTypes(WscErrors.class)
@Operations(ConsumeOperation.class)
@ConnectionProviders(WscConnectionProvider.class)
@SubTypeMapping(baseType = SecurityStrategyAdapter.class,
    subTypes = {WssDecryptSecurityStrategy.class, WssEncryptSecurityStrategy.class, WssSignSecurityStrategy.class,
        WssUsernameTokenSecurityStrategy.class, WssTimestampSecurityStrategy.class, WssVerifySignatureSecurityStrategy.class})
@Extension(name = "Web Service Consumer")
@Xml(prefix = "wsc")
public class WebServiceConsumer {

  /**
   * Default character encoding to be used in all the messages. If not specified, the default charset in the mule configuration
   * will be used
   */
  @Parameter
  @DefaultEncoding
  private String encoding;

  public String getEncoding() {
    return encoding;
  }
}
