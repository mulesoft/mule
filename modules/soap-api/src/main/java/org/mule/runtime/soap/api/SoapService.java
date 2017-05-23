/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.SoapClientFactory;


/**
 * Contract for a service that provides a Soap client factory.
 *
 * @since 4.0
 */
public interface SoapService extends Service {

  /**
   * @return a {@link SoapClientFactory} instance capable of creating {@link SoapClient} instances.
   */
  SoapClientFactory getClientFactory();
}
