/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.interception.InterceptionHandlerChain;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Execution mediator for {@link Processor} that intercepts the processor execution with an {@link InterceptionHandlerChain
 * interceptor callback chain}.
 *
 * @since 4.0
 */
public class InterceptorMessageProcessorExecutionStrategy {
  //  implements MessageProcessorExecutionMediator, MuleContextAware,
  //  FlowConstructAware {
  //
  //public static final QName SOURCE_FILE_NAME_ANNOTATION =
  //    new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName");
  //public static final QName SOURCE_FILE_LINE_ANNOTATION =
  //    new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");
  //
  //private transient Logger logger = LoggerFactory.getLogger(InterceptorMessageProcessorExecutionStrategy.class);
  //
  //private MuleContext muleContext;
  //private FlowConstruct flowConstruct;
  //
  //@Override
  //public void setMuleContext(MuleContext muleContext) {
  //  this.muleContext = muleContext;
  //}
  //
  //@Override
  //public void setFlowConstruct(FlowConstruct flowConstruct) {
  //  this.flowConstruct = flowConstruct;
  //}
  //
  ///**
  // * {@inheritDoc}
  // */
  //@Override
  //public Publisher<Event> apply(Publisher<Event> publisher, Processor processor) {
  //  if (isInterceptable(processor)) {
  //    logger.debug("Applying interceptor for Processor: '{}'", processor.getClass());
  //
  //    ComponentIdentifier componentIdentifier = ((AnnotatedObject) processor).getIdentifier();
  //    ComponentLocation componentLocation =
  //        ((AnnotatedObject) processor).getLocation(((MessageProcessorPathResolver) flowConstruct).getProcessorPath(processor));
  //
  //    InterceptionHandlerChain interceptionHandlerChain =
  //        new InterceptionHandlerChain(muleContext.getMessageProcessorInterceptorManager().retrieveInterceptionHandlerChain()
  //            .stream().filter(h -> h.intercept(componentIdentifier, componentLocation))
  //            .collect(toList()));
  //
  //    return configureInterception(publisher, interceptionHandlerChain,
  //                                 (Map<String, String>) ((AnnotatedObject) processor).getAnnotation(ANNOTATION_PARAMETERS),
  //                                 processor);
  //  }
  //
  //  return processor.apply(publisher);
  //}
  //
  //private Boolean isInterceptable(Processor processor) {
  //  if (processor instanceof AnnotatedObject) {
  //    ComponentLocation componentLocation =
  //        ((AnnotatedObject) processor).getLocation(((MessageProcessorPathResolver) flowConstruct).getProcessorPath(processor));
  //    if (componentLocation != null) {
  //      return true;
  //    } else {
  //      logger.warn("Processor '{}' is an '{}' but doesn't have a componentIdentifier", processor.getClass(),
  //                  AnnotatedObject.class);
  //    }
  //  } else {
  //    logger.debug("Processor '{}' is not an '{}'", processor.getClass(), AnnotatedObject.class);
  //  }
  //  return false;
  //}
  //
  ///**
  // * {@inheritDoc}
  // */
  //private Publisher<Event> configureInterception(Publisher<Event> publisher, InterceptionHandlerChain interceptionHandlerChain,
  //                                               Map<String, String> parameters, Processor processor) {
  //
  //  final InterceptionMappersProvider mappers =
  //      new InterceptionMappersProvider(interceptionHandlerChain, processor,
  //                                      checkedFunction(event -> resolveParameters(event, processor, parameters)));
  //
  //  return from(publisher).map(mappers.before()).transform(processor).map(mappers.after())
  //      .onErrorResumeWith(mappers.afterError());
  //}
  //
  //private static class InterceptionMappersProvider {
  //
  //  private final ListIterator<InterceptionHandler> listIterator;
  //  private final Processor processor;
  //  private final Function<Event, Map<String, Object>> parameters;
  //
  //  private DefaultInterceptionEvent eventInterception;
  //
  //  public InterceptionMappersProvider(InterceptionHandlerChain interceptionHandlerChain, Processor processor,
  //                                     Function<Event, Map<String, Object>> parameters) {
  //    this.listIterator = interceptionHandlerChain.listIterator();
  //    this.processor = processor;
  //    this.parameters = parameters;
  //  }
  //
  //  public Function<Event, Event> before() {
  //    return input -> {
  //      eventInterception = new DefaultInterceptionEvent(input);
  //      while (listIterator.hasNext()) {
  //        InterceptionHandler interceptionHandler = listIterator.next();
  //        final Map<String, Object> resolvedParameters = parameters.apply(eventInterception.getInterceptionResult());
  //
  //        try {
  //          AtomicBoolean actionCalled = new AtomicBoolean(false);
  //
  //          interceptionHandler.before(resolvedParameters, eventInterception, new InterceptionAction() {
  //
  //            private boolean isSkippable() {
  //              return !(processor instanceof InterceptingMessageProcessor || processor instanceof SelectiveRouter);
  //            }
  //
  //            @Override
  //            public void skip() {
  //              if (!isSkippable()) {
  //                throw new IllegalArgumentException("skip() may not be called for implementations of "
  //                    + InterceptingMessageProcessor.class.getName() + " or " + SelectiveRouter.class.getName());
  //              }
  //              actionCalled.set(true);
  //              throw new InterceptionSkippedException();
  //            }
  //
  //            @Override
  //            public void proceed() {
  //              actionCalled.set(true);
  //            }
  //          });
  //
  //          if (!actionCalled.get()) {
  //            throw new NoActionCalledException(interceptionHandler);
  //          }
  //        } finally {
  //          eventInterception.resolve();
  //        }
  //      }
  //      return eventInterception.getInterceptionResult();
  //    };
  //  }
  //
  //  public Function<Event, Event> after() {
  //    return output -> {
  //      final DefaultInterceptionEvent outputInterception = new DefaultInterceptionEvent(output);
  //      while (listIterator.hasPrevious()) {
  //        InterceptionHandler interceptionHandler = listIterator.previous();
  //        try {
  //          interceptionHandler.after(outputInterception);
  //        } catch (Throwable t) {
  //          throw new FailedInAfterException(t);
  //        }
  //      }
  //      return output;
  //    };
  //  }
  //
  //  public Function<? super Throwable, ? extends Publisher<? extends Event>> afterError() {
  //    return error -> {
  //      if (error.getCause() instanceof FailedInAfterException) {
  //        return error(error);
  //      }
  //
  //      while (listIterator.hasPrevious()) {
  //        InterceptionHandler interceptionHandler = listIterator.previous();
  //        try {
  //          interceptionHandler.after(eventInterception);
  //        } catch (Throwable t) {
  //          return error(new FailedInAfterException(t));
  //        }
  //      }
  //      if (getRootCause(error) instanceof InterceptionSkippedException) {
  //        return just(eventInterception.getInterceptionResult());
  //      } else {
  //        return error(error);
  //      }
  //    };
  //  }
  //
  //  private class InterceptionSkippedException extends RuntimeException {
  //
  //    private static final long serialVersionUID = 2465037950856680323L;
  //
  //  }
  //
  //  private class NoActionCalledException extends RuntimeException {
  //
  //    public NoActionCalledException(InterceptionHandler interceptionHandler) {
  //      super("No method called on 'action' parameter in before method of " + interceptionHandler.toString());
  //    }
  //
  //    private static final long serialVersionUID = 4896210944694903668L;
  //
  //  }
  //
  //  private class FailedInAfterException extends RuntimeException {
  //
  //    private static final long serialVersionUID = -4550278432332329624L;
  //
  //    public FailedInAfterException(Throwable cause) {
  //      super(cause);
  //    }
  //  }
  //
  //}
  //
  //private Map<String, Object> resolveParameters(Event event, Processor processor, Map<String, String> parameters)
  //    throws MuleException {
  //
  //  if (processor instanceof ProcessorParameterResolver) {
  //    return ((ProcessorParameterResolver) processor).resolve(event);
  //  }
  //
  //  Map<String, Object> resolvedParameters = new HashMap<>();
  //  for (Map.Entry<String, String> entry : parameters.entrySet()) {
  //    Object value;
  //    String paramValue = entry.getValue();
  //    if (muleContext.getExpressionManager().isExpression(paramValue)) {
  //      value = muleContext.getExpressionManager().evaluate(paramValue, event, flowConstruct).getValue();
  //    } else {
  //      value = valueOf(paramValue);
  //    }
  //    resolvedParameters.put(entry.getKey(), value);
  //  }
  //  return resolvedParameters;
  //}
}
