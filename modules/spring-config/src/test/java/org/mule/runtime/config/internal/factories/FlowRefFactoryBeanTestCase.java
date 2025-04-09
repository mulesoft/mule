/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_EAGER_INIT;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_PROTOTYPE;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_SINGLETON;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.FlowReferenceStory.FLOW_REFERENCE;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.context.BaseConfigurationComponentLocator;
import org.mule.runtime.config.internal.context.MuleArtifactContext;
import org.mule.runtime.config.internal.context.ObjectProviderAwareBeanFactory;
import org.mule.runtime.config.internal.dsl.model.CoreComponentBuildingDefinitionProvider;
import org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.processor.chain.SubflowMessageProcessorChainBuilder;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.core.internal.routing.result.RoutePathNotFoundException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.MockSettings;
import org.mockito.stubbing.Answer;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;


@SmallTest
public class FlowRefFactoryBeanTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final Logger LOGGER = getLogger(FlowRefFactoryBeanTestCase.class);

  private static final MockSettings INITIALIZABLE_MESSAGE_PROCESSOR =
      withSettings().extraInterfaces(Component.class, Processor.class, Initialisable.class, Disposable.class,
                                     Startable.class, Stoppable.class);
  private static final String STATIC_REFERENCED_FLOW = "staticReferencedFlow";
  private static final String DYNAMIC_REFERENCED_FLOW = "dynamicReferencedFlow";
  private static final String PARSED_DYNAMIC_REFERENCED_FLOW = "parsedDynamicReferencedFlow";
  private static final String DYNAMIC_NON_EXISTANT = "#['nonExistant']";

  private CoreEvent result;
  private final ProcessingStrategy callerFlowProcessingStrategy = mock(ProcessingStrategy.class);
  private final Flow callerFlow = mock(Flow.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final Flow targetFlow = mock(Flow.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final LifecycleState flowLifeCycleState = mock(LifecycleState.class);
  private final MessageProcessorChain targetSubFlow = mock(MessageProcessorChain.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final Processor targetSubFlowProcessor = (Processor) mock(Object.class, INITIALIZABLE_MESSAGE_PROCESSOR);
  private final SubflowMessageProcessorChainBuilder targetSubFlowChainBuilder = spy(new SubflowMessageProcessorChainBuilder());
  private final ApplicationContext applicationContext = mock(ApplicationContext.class);
  private ExtendedExpressionManager expressionManager;
  private MuleContext mockMuleContext;

  @Inject
  private ConfigurationComponentLocator locator;

  public FlowRefFactoryBeanTestCase() throws MuleException {}

  @Before
  public void setup() throws MuleException {
    // Events mocking
    result = testEvent();
    // Mule context mocking
    mockMuleContext = mockContextWithServices();
    mockMuleContext.getInjector().inject(this);

    when(locator.find(any(Location.class))).thenReturn(of(mock(Flow.class)));
    when(locator.find(Location.builder().globalName("flow").build())).thenReturn(of(callerFlow));
    // Main flow mocking
    when(callerFlow.getProcessingStrategy()).thenReturn(callerFlowProcessingStrategy);
    when(callerFlowProcessingStrategy.onProcessor(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
    // Dynamic flowref mocking (this is reverted in static flowref tests)
    expressionManager = mockMuleContext.getExpressionManager();
    doReturn(true).when(expressionManager).isExpression(anyString());
    doReturn(new TypedValue<>(PARSED_DYNAMIC_REFERENCED_FLOW, STRING)).when(expressionManager)
        .evaluate(eq(FlowRefFactoryBeanTestCase.DYNAMIC_REFERENCED_FLOW), eq(DataType.STRING),
                  eq(NULL_BINDING_CONTEXT), any(CoreEvent.class),
                  any(ComponentLocation.class), eq(true));
    // Referenced flow mocking
    when(targetFlow.apply(any(Publisher.class))).thenReturn(just(result));
    when(targetFlow.apply(any())).thenAnswer(successAnswer());
    when(targetFlow.referenced()).thenReturn(targetFlow);
    // Referenced flow lifecycle checks mocking
    when(flowLifeCycleState.isStarted()).thenReturn(true);
    when(targetFlow.getLifecycleState()).thenReturn(flowLifeCycleState);
    // Referenced subflow mocking
    List<Processor> targetSubFlowProcessors = singletonList(targetSubFlowProcessor);
    when(targetSubFlow.getMessageProcessors()).thenReturn(targetSubFlowProcessors);
    targetSubFlowChainBuilder.chain(targetSubFlowProcessors);
    when(targetSubFlow.apply(any(Publisher.class))).thenReturn(just(result));
    when(targetSubFlowProcessor.apply(any())).thenAnswer(successAnswer());
  }

  @Test
  public void staticFlowRefFlow() throws Exception {
    // Flow is wrapped to prevent lifecycle propagation
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetFlow, null);

    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetFlow, applicationContext);
    verifyLifecycle(targetFlow, 0);
  }

  @Test
  public void dynamicFlowRefFlow() throws Exception {
    // Inner MessageProcessor is used to resolve MP in runtime
    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetFlow, null, applicationContext);

    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetFlow, applicationContext);
    verifyLifecycle(targetFlow, 0);
  }

  @Test
  public void staticFlowRefSubFlow() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetSubFlow, targetSubFlowChainBuilder);

    // Processor is wrapped by factory bean implementation
    assertThat(targetSubFlow, not(equalTo(getFlowRefProcessor(flowRefFactoryBean))));
    assertThat(targetSubFlow, not(equalTo(getFlowRefProcessor(flowRefFactoryBean))));

    verifyProcess(flowRefFactoryBean, targetSubFlowProcessor, applicationContext);
    verify(targetSubFlowChainBuilder).setProcessingStrategy(argThat(ps -> {
      ReactiveProcessor pipeline = mock(ReactiveProcessor.class);
      ReactiveProcessor processor = mock(ReactiveProcessor.class);

      ps.onProcessor(processor);
      verify(callerFlowProcessingStrategy).onProcessor(processor);
      assertThat(ps.onPipeline(pipeline), sameInstance(pipeline));
      return true;
    }));
  }

  @Test
  public void dynamicFlowRefSubFlow() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean =
        createDynamicFlowRefFactoryBean(targetSubFlow, targetSubFlowChainBuilder, applicationContext);

    // Inner MessageProcessor is used to resolve MP in runtime
    assertNotSame(targetSubFlow, getFlowRefProcessor(flowRefFactoryBean));
    assertNotSame(targetSubFlow, getFlowRefProcessor(flowRefFactoryBean));

    verifyProcess(flowRefFactoryBean, targetSubFlowProcessor, applicationContext);
    verify(targetSubFlowChainBuilder).setProcessingStrategy(argThat(ps -> {
      ReactiveProcessor pipeline = mock(ReactiveProcessor.class);
      ReactiveProcessor processor = mock(ReactiveProcessor.class);

      ps.onProcessor(processor);
      verify(callerFlowProcessingStrategy).onProcessor(processor);
      assertThat(ps.onPipeline(pipeline), sameInstance(pipeline));
      return true;
    }));
  }

  @Test
  @Issue("MULE-19272")
  public void tcclProperlySetWhenStartingStaticFlowRefSubFlow() throws Exception {
    AtomicReference<ClassLoader> startTcclRef = new AtomicReference<>();
    ((Startable) doAnswer(inv -> {
      startTcclRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(targetSubFlowProcessor)).start();

    ClassUtils.withContextClassLoader(mock(ClassLoader.class), () -> {
      FlowRefFactoryBean flowRefFactoryBean;
      try {
        flowRefFactoryBean = createStaticFlowRefFactoryBean(targetSubFlow, targetSubFlowChainBuilder);
        verifyProcess(flowRefFactoryBean, targetSubFlowProcessor, applicationContext);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });

    assertThat(startTcclRef.get(), sameInstance(mockMuleContext.getExecutionClassLoader()));
  }

  @Test
  @Issue("MULE-19272")
  public void tcclProperlySetWhenStartingDynamicFlowRefSubFlow() throws Exception {
    AtomicReference<ClassLoader> startTcclRef = new AtomicReference<>();
    ((Startable) doAnswer(inv -> {
      startTcclRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(targetSubFlowProcessor)).start();

    ClassUtils.withContextClassLoader(mock(ClassLoader.class), () -> {
      FlowRefFactoryBean flowRefFactoryBean;
      try {
        flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlow, targetSubFlowChainBuilder, applicationContext);
        verifyProcess(flowRefFactoryBean, targetSubFlowProcessor, applicationContext);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });

    assertThat(startTcclRef.get(), sameInstance(mockMuleContext.getExecutionClassLoader()));
  }

  @Test
  public void dynamicFlowRefSubContextAware() throws Exception {
    CoreEvent event = testEvent();
    MuleContextAware targetMuleContextAware = mock(MuleContextAware.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(((Processor) targetMuleContextAware).apply(any(Publisher.class))).thenReturn(just(result));

    FlowRefFactoryBean flowRefFactoryBean =
        createDynamicFlowRefFactoryBean((Processor) targetMuleContextAware, null, applicationContext);
    assertSame(result.getMessage(), getFlowRefProcessor(flowRefFactoryBean).process(event).getMessage());

    verify(targetMuleContextAware).setMuleContext(mockMuleContext);
  }

  @Test
  public void dynamicFlowRefSubFlowMessageProcessorChain() throws Exception {
    CoreEvent event = testEvent();

    Processor targetSubFlowConstructAware = (Processor) mock(Object.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(targetSubFlowConstructAware.apply(any(Publisher.class))).thenReturn(just(result));
    Processor targetMuleContextAwareAware =
        (Processor) mock(MuleContextAware.class, INITIALIZABLE_MESSAGE_PROCESSOR);

    when(targetMuleContextAwareAware.apply(any(Publisher.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
    when(targetSubFlowConstructAware.apply(any())).thenAnswer(successAnswer());

    MessageProcessorChain targetSubFlowChain = mock(MessageProcessorChain.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    when(targetSubFlowChain.apply(any(Publisher.class))).thenReturn(just(result));
    List<Processor> targetSubFlowProcessors = asList(targetSubFlowConstructAware, targetMuleContextAwareAware);
    when(targetSubFlowChain.getMessageProcessors()).thenReturn(targetSubFlowProcessors);
    SubflowMessageProcessorChainBuilder chainBuilder = new SubflowMessageProcessorChainBuilder();
    chainBuilder.chain(targetSubFlowProcessors);

    FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlowChain, chainBuilder, applicationContext);
    final Processor flowRefProcessor = getFlowRefProcessor(flowRefFactoryBean);
    just(event).transform(flowRefProcessor).block();

    verify((MuleContextAware) targetMuleContextAwareAware, atLeastOnce()).setMuleContext(mockMuleContext);
  }

  private Processor getFlowRefProcessor(FlowRefFactoryBean factoryBean) throws Exception {
    Processor processor = factoryBean.getObject();
    setMuleContextIfNeeded(processor, mockMuleContext);
    return processor;
  }

  @Test
  public void dynamicFlowRefDoesNotExist() throws Exception {
    doReturn(new TypedValue<>("other", STRING)).when(expressionManager).evaluate(eq(DYNAMIC_NON_EXISTANT), eq(DataType.STRING),
                                                                                 eq(NULL_BINDING_CONTEXT), any(CoreEvent.class),
                                                                                 any(ComponentLocation.class), eq(true));

    expectedException.expect(instanceOf(RoutePathNotFoundException.class));
    getFlowRefProcessor(createFlowRefFactoryBean(DYNAMIC_NON_EXISTANT, "flow", applicationContext)).process(testEvent());
  }

  @Test()
  @Feature(CORE_COMPONENTS)
  @Story(FLOW_REFERENCE)
  public void concurrentDynamicSubFlowInstantiation() throws Exception {
    // MuleArtifactContext stubbing
    DefaultListableBeanFactory beanFactory = new ObjectProviderAwareBeanFactory(null);
    MuleArtifactContext muleArtifactContext = spy(createMuleArtifactContextStub(beanFactory));
    // BeanFactory stubbing (subFlow and subFlow processor factories)
    List<Component> stubbedSubFlowProcessors = new ArrayList<>(2);
    BeanDefinition subFlowProcessorBeanDefinition = genericBeanDefinition(Component.class, () -> {
      Component stubbedProcessor = new StubbedProcessor(result);
      stubbedSubFlowProcessors.add(stubbedProcessor);
      return stubbedProcessor;
    }).getBeanDefinition();
    ComponentBuildingDefinition subFlowComponentBuildingDefinition = new CoreComponentBuildingDefinitionProvider()
        .getComponentBuildingDefinitions()
        .stream()
        .filter(componentBuildingDefinition -> componentBuildingDefinition.getComponentIdentifier().getName().equals("sub-flow"))
        .findFirst()
        .get();
    BeanDefinition subFlowBeanDefinition = genericBeanDefinition(new ObjectFactoryClassRepository()
        .getObjectFactoryClass(SubflowMessageProcessorChainFactoryBean.class, Object.class))
        .addPropertyValue("name", PARSED_DYNAMIC_REFERENCED_FLOW)
        .addPropertyValue("messageProcessors", subFlowProcessorBeanDefinition)
        .addPropertyValue(IS_SINGLETON, !subFlowComponentBuildingDefinition.isPrototype())
        .addPropertyValue(IS_PROTOTYPE, subFlowComponentBuildingDefinition.isPrototype())
        .addPropertyValue(IS_EAGER_INIT, new LazyValue<>(() -> true))
        .setScope(BeanDefinition.SCOPE_PROTOTYPE)
        .getBeanDefinition();
    beanFactory.registerSingleton(InitialSpanInfoProvider.class.getName(), new DummyComponentTracerFactory());
    beanFactory.registerBeanDefinition(PARSED_DYNAMIC_REFERENCED_FLOW, subFlowBeanDefinition);
    // Additional flow and processing strategy (needed to generate a concurrent subflow instantiation)
    Flow concurrentCallerFlow = mock(Flow.class, INITIALIZABLE_MESSAGE_PROCESSOR);
    ProcessingStrategy concurrentCallerFlowProcessingStrategy = mock(ProcessingStrategy.class);
    when(locator.find(Location.builder().globalName("concurrentFlow").build())).thenReturn(of(concurrentCallerFlow));
    when(concurrentCallerFlow.getProcessingStrategy()).thenReturn(concurrentCallerFlowProcessingStrategy);
    when(concurrentCallerFlowProcessingStrategy.onProcessor(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
    // Two flowRef dynamically pointing to the same subFlow
    FlowRefFactoryBean flowRefFactoryBean = createFlowRefFactoryBean(DYNAMIC_REFERENCED_FLOW, "flow", muleArtifactContext);
    FlowRefFactoryBean parallelFlowRefFactoryBean =
        createFlowRefFactoryBean(DYNAMIC_REFERENCED_FLOW, "concurrentFlow", muleArtifactContext);
    // Events are sent to both flowRefs in parallel in order to trigger a concurrent subflow instantiation
    CountDownLatch threadCountdown = new CountDownLatch(2);
    Callable<Void> flowRefEvents = sendEventsThroughFlowRefAsynchronously(threadCountdown, flowRefFactoryBean);
    Callable<Void> parallelFlowRefEvents = sendEventsThroughFlowRefAsynchronously(threadCountdown, parallelFlowRefFactoryBean);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try {
      executorService.invokeAll(asList(flowRefEvents, parallelFlowRefEvents));
    } finally {
      executorService.shutdown();
    }
    // Assertions over each parent flow processing strategies
    verify(callerFlowProcessingStrategy, times(2)).onProcessor(any());
    verify(concurrentCallerFlowProcessingStrategy, times(2)).onProcessor(any());
    // Asssertions over subflow components root container location
    assertThat(stubbedSubFlowProcessors.get(0).getRootContainerLocation().getGlobalName(),
               not(equalTo(stubbedSubFlowProcessors.get(1).getRootContainerLocation().getGlobalName())));
  }

  private Callable<Void> sendEventsThroughFlowRefAsynchronously(CountDownLatch countDownLatch,
                                                                FlowRefFactoryBean flowRefFactoryBean) {
    return () -> {
      try {
        countDownLatch.countDown();
        countDownLatch.await();
        sendEventsThroughFlowRef(flowRefFactoryBean);
      } catch (Exception e) {
        throw new RuntimeException("Error sending events to a flowRef", e);
      }
      return null;
    };
  }

  private void sendEventsThroughFlowRef(FlowRefFactoryBean flowRefFactoryBean) {
    try {
      Processor flowRefProcessor = getFlowRefProcessor(flowRefFactoryBean);
      initialiseIfNeeded(flowRefProcessor);
      startIfNeeded(flowRefProcessor);
      assertSame(result.getMessage(), just(newEvent()).cast(CoreEvent.class).transform(flowRefProcessor).block().getMessage());
      assertSame(result.getMessage(), just(newEvent()).cast(CoreEvent.class).transform(flowRefProcessor).block().getMessage());
      stopIfNeeded(flowRefProcessor);
      disposeIfNeeded(flowRefProcessor, null);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException("Error sending events to a flowRef", e);
    }
  }

  private static class StubbedProcessor extends AbstractComponent implements Processor {

    private final CoreEvent applyResult;

    public StubbedProcessor(CoreEvent applyResult) {
      this.applyResult = applyResult;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new RuntimeException(new IllegalAccessException("This method should never be called"));
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher)
          .doOnNext(event -> ((BaseEventContext) event.getContext())
              .success(CoreEvent.builder(event).message(applyResult.getMessage()).variables(applyResult.getVariables()).build()))
          .map(event -> CoreEvent.builder(event).message(applyResult.getMessage()).variables(applyResult.getVariables()).build());
    }

  }

  private MuleArtifactContext createMuleArtifactContextStub(DefaultListableBeanFactory mockedBeanFactory) {
    MuleArtifactContext muleArtifactContext =
        new MuleArtifactContext(mockMuleContext, emptyArtifact(), empty(),
                                new BaseConfigurationComponentLocator(),
                                new ContributedErrorTypeRepository(), new ContributedErrorTypeLocator(),
                                emptyMap(), false, APP,
                                new ComponentBuildingDefinitionRegistry(),
                                mock(MemoryManagementService.class),
                                mock(FeatureFlaggingService.class),
                                mock(ExpressionLanguageMetadataService.class)) {

          @Override
          protected DefaultListableBeanFactory createBeanFactory() {
            return mockedBeanFactory;
          }

          @Override
          protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean invocation here
          }

          @Override
          protected void registerListeners() {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void finishRefresh() {
            // Bean factory is mocked, so no nothing to do here
          }
        };
    muleArtifactContext.refresh();
    return muleArtifactContext;
  }

  private FlowRefFactoryBean createFlowRefFactoryBean(String referencedFlowName, String flowRefLocation,
                                                      ApplicationContext applicationContext)
      throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
    flowRefFactoryBean.setName(referencedFlowName);
    flowRefFactoryBean.setAnnotations(singletonMap(LOCATION_KEY, from(flowRefLocation)));
    flowRefFactoryBean.setApplicationContext(applicationContext);
    mockMuleContext.getInjector().inject(flowRefFactoryBean);
    return flowRefFactoryBean;
  }

  private FlowRefFactoryBean createStaticFlowRefFactoryBean(Processor target, Object targetBuilder)
      throws Exception {
    doReturn(false).when(expressionManager).isExpression(anyString());
    if (targetBuilder != null) {
      when(applicationContext.getBean(eq(STATIC_REFERENCED_FLOW))).thenReturn(targetBuilder);
    } else {
      when(applicationContext.getBean(eq(STATIC_REFERENCED_FLOW))).thenReturn(target);
    }
    return createFlowRefFactoryBean(STATIC_REFERENCED_FLOW, "flow", applicationContext);
  }

  private FlowRefFactoryBean createDynamicFlowRefFactoryBean(Processor target, Object targetBuilder,
                                                             ApplicationContext applicationContext)
      throws Exception {
    if (targetBuilder != null) {
      doReturn(targetBuilder).when(applicationContext).getBean(eq(PARSED_DYNAMIC_REFERENCED_FLOW));
    } else {
      doReturn(target).when(applicationContext).getBean(eq(PARSED_DYNAMIC_REFERENCED_FLOW));
    }
    return createFlowRefFactoryBean(DYNAMIC_REFERENCED_FLOW, "flow", applicationContext);
  }

  private Answer<?> successAnswer() {
    return invocation -> Mono.from(invocation.getArgument(0))
        .cast(CoreEvent.class)
        .doOnNext(event -> ((BaseEventContext) event.getContext())
            .success(CoreEvent.builder(event).message(result.getMessage()).variables(result.getVariables()).build()))
        .map(event -> CoreEvent.builder(event).message(result.getMessage()).variables(result.getVariables()).build());
  }

  private void verifyProcess(FlowRefFactoryBean flowRefFactoryBean, Processor target, ApplicationContext applicationContext)
      throws Exception {
    sendEventsThroughFlowRef(flowRefFactoryBean);
    verify(applicationContext).getBean(anyString());
    verify(target, times(2)).apply(any(Publisher.class));
  }

  private void verifyLifecycle(Processor target, int lifecycleRounds)
      throws Exception {
    verify((Initialisable) target, times(lifecycleRounds)).initialise();

    verify(targetSubFlow, times(lifecycleRounds)).initialise();
    verify(targetSubFlow, times(lifecycleRounds)).start();
    verify(targetSubFlow, times(lifecycleRounds)).stop();
    verify(targetSubFlow, times(lifecycleRounds)).dispose();
  }

  @Test
  public void referencedSubFlowIsStartedWhenCallerFlowIsStartedAfterStop() throws Exception {
    FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetSubFlow, null);
    Processor flowRefProcessor = flowRefFactoryBean.doGetObject();

    flowRefProcessor.process(testEvent());

    verify(targetSubFlow, times(1)).initialise();
    verify(targetSubFlow, times(1)).start();
    verify(targetSubFlow, times(0)).stop();
    verify(targetSubFlow, times(0)).dispose();

    stopIfNeeded(flowRefProcessor);

    verify(targetSubFlow, times(1)).initialise();
    verify(targetSubFlow, times(1)).start();
    verify(targetSubFlow, times(1)).stop();
    verify(targetSubFlow, times(0)).dispose();

    startIfNeeded(flowRefProcessor);

    verify(targetSubFlow, times(1)).initialise();
    verify(targetSubFlow, times(2)).start();
    verify(targetSubFlow, times(1)).stop();
    verify(targetSubFlow, times(0)).dispose();
  }

}
