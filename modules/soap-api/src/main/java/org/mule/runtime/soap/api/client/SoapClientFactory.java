/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.client;

import org.mule.runtime.api.connection.ConnectionException;

/**
 * A Contract for Factory Objects that creates {@link SoapClient instances}
 *
 * @since 4.0
 */
public interface SoapClientFactory {

  /**
   * Creates a new SoapClient instance.
   *
   * @param configuration the {@link SoapClientConfiguration} specifying the desired client configuration.
   * @return a newly built {@link SoapClient} based on the {@code configuration}.
   */
  SoapClient create(SoapClientConfiguration configuration) throws ConnectionException;
}
