/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.inject;

import static org.mule.test.allure.AllureConstants.JavaSdk.JAVAX_INJECT_COMPATIBILITY;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;

import static java.nio.charset.Charset.defaultCharset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(JAVAX_INJECT_COMPATIBILITY)
public class JavaxInjectCompatibilityTestCase extends AbstractExtensionFunctionalTestCase {

  private static String storedData;

  @Inject
  @Named("source")
  private Flow sourceFlow;

  @Inject
  @Named("sourceConfig")
  private Flow sourceConfigFlow;

  @Override
  protected String getConfigFile() {
    return "inject/javax-inject-compatibility-config.xml";
  }

  @Test
  public void sourceConfig() throws MuleException {
    sourceConfigFlow.start();

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      assertThat(storedData, is(defaultCharset().name()));
      return true;
    }));
  }

  @Test
  public void config() throws Exception {
    final Message flowOutput = flowRunner("operationConfig").run().getMessage();
    assertThat(flowOutput.getPayload().getValue(), is(defaultCharset().name()));
  }

  @Test
  public void connectionProvider() throws Exception {
    final Message flowOutput = flowRunner("operationConnection").run().getMessage();
    assertThat(flowOutput.getPayload().getValue(), is(defaultCharset().name()));
  }

  @Test
  public void source() throws MuleException {
    sourceFlow.start();

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      assertThat(storedData, is(defaultCharset().name()));
      return true;
    }));
  }

  @Test
  public void operation() throws Exception {
    final Message flowOutput = flowRunner("operation").run().getMessage();
    assertThat(flowOutput.getPayload().getValue(), is(defaultCharset().name()));
  }

  @Test
  public void pojo() throws Exception {
    final Message flowOutput = flowRunner("operationPojo").run().getMessage();
    assertThat(flowOutput.getPayload().getValue(), is(defaultCharset().name()));
  }

  @Test
  public void function() throws Exception {
    final Message flowOutput = flowRunner("function").run().getMessage();
    assertThat(flowOutput.getPayload().getValue(), is(defaultCharset().name()));
  }

  @Test
  public void pojoAttribute() {

  }

  @Test
  public void pojoParam() {

  }

  public static String storeData(String data) {
    storedData = data;
    return data;
  }
}
