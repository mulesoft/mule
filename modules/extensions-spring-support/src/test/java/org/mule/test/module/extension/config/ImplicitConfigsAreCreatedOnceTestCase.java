/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

import java.util.Collection;

public class ImplicitConfigsAreCreatedOnceTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "implicit-configs-are-created-once.xml";
  }

  @Test
  public void tooManyConfigsTestCase() throws Exception {
    Integer value = (Integer) flowRunner("implicitConfig").run().getMessage().getPayload().getValue();
    assertThat(value, is(5));
    Collection<ConfigurationProvider> configs = registry.lookupAllByType(ConfigurationProvider.class);
    assertThat(configs, hasSize(5));
    assertThat(configs.stream().filter(c -> c.getName().endsWith("implicit")).collect(toList()), hasSize(2));
  }
}
