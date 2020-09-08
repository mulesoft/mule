/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import org.junit.Test;

public class OperationSampleDataTestCase extends AbstractSampleDataTestCase {

  private static final String EXPECTED_PAYLOAD = "my payload";
  private static final String EXPECTED_ATTRIBUTES = "my attributes";
  private static final String CONF_PREFIX = "from-conf-";
  private static final String NULL_VALUE = "<<null>>";

  @Override
  protected String getConfigFile() {
    return "data/operation-sample-data.xml";
  }

  @Test
  public void connectionLess() throws Exception {
    assertMessage(getSample("connectionLess"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnection() throws Exception {
    assertMessage(getSample("useConnection"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void nonBlocking() throws Exception {
    assertMessage(getSample("nonBlocking"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConfig() throws Exception {
    assertMessage(getSample("useConfig"), CONF_PREFIX + EXPECTED_PAYLOAD, CONF_PREFIX + EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroup() throws Exception {
    assertMessage(getSample("parameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroupWithOptional() throws Exception {
    assertMessage(getSample("parameterGroupWithOptional"), EXPECTED_PAYLOAD, NULL_VALUE);
  }

  @Test
  public void showInDslParameterGroup() throws Exception {
    assertMessage(getSample("showInDslParameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void aliasedGroup() throws Exception {
    assertMessage(getSample("aliasedGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingActingParameter() throws Exception {
    assertMessage(getSample("missingActingParameter"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingActingParameterInGroup() throws Exception {
    assertMessage(getSample("missingActingParameterInGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }


  @Test
  public void muleContextAwareSampleData() throws Exception {
    assertMessage(getSample("muleContextAwareSampleData"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }
}
