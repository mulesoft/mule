/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever.imap;

import org.mule.extension.email.internal.retriever.RetrieverConfiguration;
import org.mule.extension.email.internal.retriever.RetrieverOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * Configuration for operations that are performed through the IMAP (Internet Message Access Protocol) protocol.
 *
 * @since 4.0
 */
@Operations({IMAPOperations.class, RetrieverOperations.class})
@ConnectionProviders({IMAPProvider.class, IMAPSProvider.class})
@Configuration(name = "imap")
@DisplayName("IMAP")
public class IMAPConfiguration implements RetrieverConfiguration {

  /**
   * Indicates whether the retrieved emails should be opened and read. The default value is "true".
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean eagerlyFetchContent;

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEagerlyFetchContent() {
    return eagerlyFetchContent;
  }

}
