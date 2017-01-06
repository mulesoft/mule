/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.interception;

import static org.mule.functional.functional.FlowAssert.verify;
import static org.mule.runtime.dsl.api.xml.DslConstants.CORE_NAMESPACE;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.interception.ProcessorInterceptorCallback;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InterceptionFlowTestCase extends AbstractIntegrationTestCase {

  public static final String INTERCEPTED = "intercepted";
  public static final String EXPECTED_INTERCEPTED_MESSAGE = TEST_MESSAGE + " " + INTERCEPTED;

  private ComponentIdentifier loggerComponentIdentifier =
      new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE).withName("logger").build();
  private ComponentIdentifier fileReadComponentIdentifier =
      new ComponentIdentifier.Builder().withNamespace("file").withName("read").build();
  private ComponentIdentifier customInterceptorComponentIdentifier =
      new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE).withName("custom-interceptor").build();

  private ProcessorInterceptorCallbackHolder loggerCallbackHolder;
  private ProcessorInterceptorCallbackHolder fileReadCallbackHolder;
  private ProcessorInterceptorCallbackHolder customInterceptorCallbackHolder;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/interception-flow.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new ConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        loggerCallbackHolder = new ProcessorInterceptorCallbackHolder();
        fileReadCallbackHolder = new ProcessorInterceptorCallbackHolder();
        customInterceptorCallbackHolder = new ProcessorInterceptorCallbackHolder();

        muleContext.getProcessorInterceptorManager().registerInterceptionCallback(loggerComponentIdentifier,
                                                                                  loggerCallbackHolder);
        muleContext.getProcessorInterceptorManager().registerInterceptionCallback(fileReadComponentIdentifier,
                                                                                  fileReadCallbackHolder);
        muleContext.getProcessorInterceptorManager().registerInterceptionCallback(customInterceptorComponentIdentifier,
                                                                                  customInterceptorCallbackHolder);
      }

      @Override
      public boolean isConfigured() {
        return true;
      }
    });
    super.addBuilders(builders);
  }

  @Test
  public void interceptLoggerMessageProcessor() throws Exception {
    loggerCallbackHolder.setDelegate(new DoProcessorInterceptorCallback());
    String flow = "loggerProcessorFlow";
    flowRunner(flow).withVariable("expectedMessage", EXPECTED_INTERCEPTED_MESSAGE).withPayload(TEST_MESSAGE).run().getMessage();
    verify(flow);
  }

  @Test
  public void interceptOperationMessageProcessor() throws Exception {
    fileReadCallbackHolder.setDelegate(new DoProcessorInterceptorCallback());
    String flow = "operationProcessorFlow";
    flowRunner(flow).withVariable("expectedMessage", EXPECTED_INTERCEPTED_MESSAGE)
        .withVariable("source", temporaryFolder.getRoot()).withPayload(TEST_MESSAGE).run().getMessage();
    verify(flow);
  }

  @Test
  public void interceptCustomInterceptorMessageProcessor() throws Exception {
    customInterceptorCallbackHolder.setDelegate(new DoNotProcessorInterceptorCallback());
    String flow = "customInterceptorProcessorFlow";
    flowRunner(flow).withVariable("expectedMessage", TEST_MESSAGE + "!").withPayload(TEST_MESSAGE).run().getMessage();
    verify(flow);
  }

  @Test
  public void interceptCustomInterceptorMessageProcessorNotInvoked() throws Exception {
    customInterceptorCallbackHolder.setDelegate(new DoProcessorInterceptorCallback());
    String flow = "customInterceptorNotInvokedProcessorFlow";
    flowRunner(flow).withVariable("expectedMessage", EXPECTED_INTERCEPTED_MESSAGE).withPayload(TEST_MESSAGE).run().getMessage();
    verify(flow);
  }

  class ProcessorInterceptorCallbackHolder implements ProcessorInterceptorCallback {

    private ProcessorInterceptorCallback delegate;

    public void setDelegate(ProcessorInterceptorCallback delegate) {
      this.delegate = delegate;
    }

    @Override
    public void before(Event event, Map<String, Object> parameters) {
      delegate.before(event, parameters);
    }

    @Override
    public boolean shouldInterceptExecution(Event event, Map<String, Object> parameters) {
      return delegate.shouldInterceptExecution(event, parameters);
    }

    @Override
    public Event getResult(Event event) throws MuleException {
      return delegate.getResult(event);
    }

    @Override
    public void after(Event resultEvent, Map<String, Object> parameters) {
      delegate.after(resultEvent, parameters);
    }
  }

  class DoProcessorInterceptorCallback implements ProcessorInterceptorCallback {

    @Override
    public boolean shouldInterceptExecution(Event event, Map<String, Object> parameters) {
      return true;
    }

    @Override
    public Event getResult(Event event) throws MuleException {
      return Event.builder(event)
          .message(InternalMessage.builder(event.getMessage())
              .payload(event.getMessage().getPayload().getValue() + " " + INTERCEPTED)
              .build())
          .build();
    }

  }

  class DoNotProcessorInterceptorCallback implements ProcessorInterceptorCallback {

    @Override
    public Event getResult(Event event) throws MuleException {
      throw new IllegalStateException("Should not be invoked as the intercepted processor should be called");
    }
  }

}
