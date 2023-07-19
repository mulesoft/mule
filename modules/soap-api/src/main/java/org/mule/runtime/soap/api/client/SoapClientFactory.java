/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
