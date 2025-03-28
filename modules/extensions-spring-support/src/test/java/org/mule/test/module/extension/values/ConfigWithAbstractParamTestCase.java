/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ConnectionProviderValueResolver;

import org.junit.Test;

public class ConfigWithAbstractParamTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/abstract-param-values.xml";
  }

  @Test
  public void testAbstractParam() throws Exception {
    ConnectionProviderValueResolver<Object> connectionProviderResolver =
        createConnectionProviderResolver("abstract-param-config", "abstract-param-provider");
    connectionProviderResolver.getResolverSet();
    connectionProviderResolver.isDynamic();
  }

}
