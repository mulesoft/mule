/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.api.message.Message.of;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.registry.ResolverException;
import org.mule.runtime.core.api.routing.ResponseTimeoutException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessageRedeliveredException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.test.AbstractIntegrationTestCase;

import java.sql.SQLDataException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Error Handling")
@Stories("Error Handler")
public class ErrorHandlerTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private I18nMessage mockMessage = mock(I18nMessage.class);
  private Processor mockMP = mock(Processor.class);

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handler.xml";
  }

  @Test
  public void testMatchesCorrectExceptionStrategy() throws Exception {
    callAndThrowException(new IllegalStateException(), "0 catch-2");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingWrapper() throws Exception {
    callAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new IllegalStateException()), "0 catch-2");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingWrapperAndCause() throws Exception {
    callAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""),
                                                new RuntimeException(new IllegalStateException())),
                          "0 catch-2");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingBaseClass() throws Exception {
    callAndThrowException(new BaseException(), "0 catch-3");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingSubtypeClass() throws Exception {
    callAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new SubtypeException()), "0 catch-4");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingSubtypeSubtypeClass() throws Exception {
    callAndThrowException(new SubtypeSubtypeException(), "0 catch-4");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingRegex() throws Exception {
    callAndThrowException(new AnotherTypeMyException(), "0 catch-5");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingGroovyExpressionEvaluator() throws Exception {
    callAndThrowException("groovy", new SQLDataException(), "groovy catch-6");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingStartsWithWildcard() throws Exception {
    callAndThrowException(new StartsWithException(), "0 catch-7");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingFinishesWithWildcard() throws Exception {
    callAndThrowException(new ThisExceptionFinishesWithException(), "0 catch-8");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingMatchesAll() throws Exception {
    callAndThrowException(new AnotherTotallyDifferentKindOfException(), "0 catch-9");
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingFinishesWithSomethingElse() throws Exception {
    callAndThrowException(new ThisExceptionFinishesWithSomethingElse(), "0 groovified");
  }

  @Test
  public void testMatchesCorrectExceptionUsingNoCause() throws Exception {
    expectedException.expectCause(is(instanceOf(ComponentException.class)));
    expectedException.expectCause(hasCause(instanceOf(ResolverException.class)));
    callAndThrowException(new ResolverException(CoreMessages.createStaticMessage("")), null);
  }

  @Test
  public void transformation() throws Exception {
    String expectedMessage = "0 transformation";
    Transformer mockTransformer = mock(Transformer.class);
    callTypeAndThrowException(new MessageTransformerException(mockMessage, mockTransformer), expectedMessage);
    callTypeAndThrowException(new TransformerException(mockMessage, mockTransformer), expectedMessage);
  }

  @Test
  public void expression() throws Exception {
    callTypeAndThrowException(new ExpressionRuntimeException(mockMessage, new Exception()), "0 expression");
  }

  @Test
  public void connectivity() throws Exception {
    callTypeAndThrowException(new RetryPolicyExhaustedException(mockMessage, new Object()), "0 connectivity");
  }

  @Test
  public void security() throws Exception {
    callTypeAndThrowException(new UnauthorisedException(mockMessage), "0 security");
  }

  @Test
  public void routing() throws Exception {
    String expectedMessage = "0 routing";
    callTypeAndThrowException(new RoutingException(mockMP), expectedMessage);
    callTypeAndThrowException(new ResponseTimeoutException(mockMessage, mockMP), expectedMessage);
  }

  @Test
  public void redelivery() throws Exception {
    MessageRedeliveredException exception =
        new MessageRedeliveredException("3", 1, 1, eventBuilder().message(of("0")).build(), mockMP);
    callTypeAndThrowException(exception, "0 redelivery");
  }

  @Test
  public void any() throws Exception {
    String expectedMessage = "0 any";
    callTypeAndThrowException(new RuntimeException(), expectedMessage);
    callTypeAndThrowException(new DefaultMuleException(mockMessage), expectedMessage);
  }

  @Test
  public void criticalNotHandled() throws Exception {
    MessagingException exception = flowRunner("propagatesCriticalErrors").runExpectingException();
    assertThat(exception.getEvent().getError().isPresent(), is(true));
    assertThat(exception.getEvent().getError().get().getErrorType().getIdentifier(), is("CRITICAL"));
  }

  private void callTypeAndThrowException(Exception exception, String expectedMessage) throws Exception {
    Message response = flowRunner("matchesHandlerUsingType")
        .withPayload("0")
        .withVariable("exception", exception)
        .run()
        .getMessage();
    assertThat(getPayloadAsString(response), is(expectedMessage));
  }

  private void callAndThrowException(final Exception exceptionToThrow, final String expectedMessage) throws Exception {
    callAndThrowException("0", exceptionToThrow, expectedMessage);
  }

  private void callAndThrowException(Object payload, final Exception exceptionToThrow, final String expectedMessage)
      throws Exception {
    FunctionalTestComponent ftc = getFunctionalTestComponent("matchesHandlerUsingWhen");
    ftc.setEventCallback((context, component, muleContext) -> {
      throw exceptionToThrow;
    });
    Message response =
        flowRunner("matchesHandlerUsingWhen").withPayload(payload).run().getMessage();
    assertThat(getPayloadAsString(response), is(expectedMessage));
  }

  public static class BaseException extends Exception {
  }

  public static class SubtypeException extends BaseException {
  }

  public static class SubtypeSubtypeException extends SubtypeException {
  }

  public static class AnotherTypeMyException extends Exception {
  }

  public static class StartsWithException extends Exception {
  }

  public static class ThisExceptionFinishesWithException extends Exception {
  }

  public static class ThisExceptionFinishesWithSomethingElse extends Exception {
  }

  public static class AnotherTotallyDifferentKindOfException extends Exception {
  }

  public static class ThrowExceptionProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      Throwable exception = (Throwable) event.getVariable("exception").getValue();
      if (exception instanceof MuleException) {
        if (exception instanceof MessagingException) {
          exception = new MessagingException(event, exception);
        }
        throw (MuleException) exception;
      } else if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      }
      return event;
    }

  }

  public static class ThrowErrorProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new AssertionError("validation failed");
    }

  }
}
