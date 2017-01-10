/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.CoreMatchers.sameInstance;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreSourceRetryPolicyProviderTestCase extends ExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 1000;
  public static final int POLL_DELAY_MILLIS = 50;
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static ConnectionException connectionException = new ConnectionException("ERROR");

  private static ExecutorService executor;

  @Override
  protected String getConfigFile() {
    return "petstore-source-retry-policy.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnectorWithSource.class};
  }

  @After
  public void tearDown() {
    PetStoreConnectorWithSource.timesStarted = 0;
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  @Test
  public void retryPolicySourceFailOnStart() throws Exception {
    exception.expect(RetryPolicyExhaustedException.class);
    exception.expectCause(sameInstance(connectionException));
    try {
      startFlow("source-fail-on-start");
    } catch (Exception e) {
      new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS)
          .check(new JUnitLambdaProbe(() -> PetStoreConnectorWithSource.timesStarted == 2));
      throw e;
    }
  }

  @Test
  public void retryPolicySourceFailOnException() throws Exception {
    startFlow("source-fail-on-exception");
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS)
        .check(new JUnitLambdaProbe(() -> PetStoreConnectorWithSource.timesStarted == 3));
  }

  @Extension(name = "petstore", description = "PetStore Test connector")
  @Xml(namespaceLocation = "http://www.mulesoft.org/schema/mule/petstore", namespace = "petstore")
  @Sources(PetStoreSource.class)
  public static class PetStoreConnectorWithSource extends PetStoreConnector {

    public static int timesStarted;
  }

  @Alias("source")
  public static class PetStoreSource extends Source<String, Attributes> {

    @UseConfig
    PetStoreConnectorWithSource config;

    @Parameter
    @Optional(defaultValue = "false")
    boolean failOnStart;

    @Parameter
    @Optional(defaultValue = "false")
    boolean failOnException;

    public static boolean failedDueOnException = false;

    @Override
    public void onStart(SourceCallback<String, Attributes> sourceCallback) throws MuleException {
      PetStoreConnectorWithSource.timesStarted++;

      if (failOnStart || failedDueOnException) {
        throw new RuntimeException(connectionException);
      }

      if (failOnException) {
        failedDueOnException = true;
        executor = newSingleThreadExecutor();
        executor.execute(() -> sourceCallback.onSourceException(connectionException));
      }
    }

    @Override
    public void onStop() {}
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }
}
