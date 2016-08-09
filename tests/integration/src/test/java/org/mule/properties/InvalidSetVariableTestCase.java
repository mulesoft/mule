/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InvalidSetVariableTestCase extends AbstractMuleTestCase {

  private String muleConfigPath;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"org/mule/properties/invalid-set-property.xml"},
        {"org/mule/properties/invalid-set-variable.xml"}});
  }

  public InvalidSetVariableTestCase(String muleConfigPath) {
    this.muleConfigPath = muleConfigPath;
  }

  @Test(expected = InitialisationException.class)
  public void emptyVariableNameValidatedBySchema() throws Exception {
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    new DefaultMuleContextFactory().createMuleContext(new SpringXmlConfigurationBuilder(muleConfigPath));
  }
}
