/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved;

import static org.junit.Assert.fail;
import org.mule.runtime.deployment.model.api.plugin.moved.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.moved.PluginDescriptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class FailureAbstractPluginTestCase {

  @Parameterized.Parameter(value = 0)
  public String pluginFolder;

  @Parameterized.Parameters(name = "{index}:{0}")
  public static Collection<Object[]> data() {

    return Arrays.asList("plugin-descriptor-corrupted",
                         "plugin-descriptor-minMuleVersion-missing",
                         "plugin-descriptor-minMuleVersion-wrong",
                         "plugin-descriptor-name-missing",
                         "plugin-descriptor-wrong-filename",
                         "plugin-descriptor-wrong-type",
                         "plugin-no-descriptor",
                         "plugin-wrong-meta-inf")
        .stream()
        .map(ele -> new Object[] {ele})
        .collect(Collectors.toList());
  }

  @Test(expected = MalformedPluginException.class)
  public void testFailure() throws MalformedPluginException {
    assertFailurePlugin(pluginFolder);
  }

  private void assertFailurePlugin(String pluginFolder) throws MalformedPluginException {
    getPluginDescriptor("plugin-failure-structures", pluginFolder);
    fail("Should not have reach here");
  }

  protected abstract PluginDescriptor getPluginDescriptor(String parentFolder, String pluginFolder)
      throws MalformedPluginException;
}
