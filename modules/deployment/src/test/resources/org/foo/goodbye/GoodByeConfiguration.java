/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.goodbye;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.client.ExtensionsClient;

import javax.inject.Inject;

@Configuration(name = "config")
public class GoodByeConfiguration {

  @Inject
  private ExtensionsClient extensionsClient;

}