/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.goodbye;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.client.ExtensionsClient;

import jakarta.inject.Inject;

@Configuration(name = "config")
public class GoodByeConfiguration {

  @Inject
  private ExtensionsClient extensionsClient;

}