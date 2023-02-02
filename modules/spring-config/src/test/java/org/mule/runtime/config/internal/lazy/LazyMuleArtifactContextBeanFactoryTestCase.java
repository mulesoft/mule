/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.lazy;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.forExtension;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ComponentLifeCycle.COMPONENT_LIFE_CYCLE;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.config.internal.context.ObjectProviderAwareBeanFactory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.ResolvableType;

/**
 * Test case for the methods of {@link org.mule.runtime.config.internal.context.lazy.LazyMuleArtifactContext} that implement
 * {@link org.springframework.beans.factory.BeanFactory}
 */
@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(COMPONENT_LIFE_CYCLE)
@RunWith(Parameterized.class)
public class LazyMuleArtifactContextBeanFactoryTestCase extends AbstractLazyMuleArtifactContextTestCase {

  @Parameterized.Parameters(name = "BeanFactory throws NoSuchBeanDefinition: {0}")
  public static Object[] parameters() {
    return new Object[] {false, true};
  }

  @Parameterized.Parameter
  public boolean beanFactoryMustThrow;

  @Rule
  public final ExpectedException expectedException = none();

  private static final String ANOTHER_FLOW = "anotherFlow";

  private final AtomicInteger initializations = new AtomicInteger(0);
  private final FakeBeanFactoryDefaultAnswer beanFactoryDefaultAnswer = new FakeBeanFactoryDefaultAnswer();

  @Override
  public void setup() throws Exception {
    super.setup();
    // Enables the Bean Factory to throw only after initial setup
    beanFactoryDefaultAnswer.setBeanFactoryMustThrow(beanFactoryMustThrow);
  }

  @Test
  public void whenBeanFactoryThrowsThenThrows() {
    assumeThat(beanFactoryMustThrow, is(true));
    doThrow(NoSuchBeanDefinitionException.class).when(beanFactory).getBean("non-existent");
    expectedException.expect(NoSuchBeanDefinitionException.class);
    lazyMuleArtifactContext.getBean("non-existent");
    verify(beanFactory, never()).registerBeanDefinition(eq("non-existent"), any());
  }

  @Test
  public void whenBeanFactoryDoesNotThrowThenDontThrow() {
    doReturn(null).when(beanFactory).getBean("non-existent");
    assertThat(lazyMuleArtifactContext.getBean("non-existent"), is(nullValue()));
    verify(beanFactory, never()).registerBeanDefinition(eq("non-existent"), any());
  }

  @Test
  public void whenInitializationIsNotDoneYetThenShouldNotCreateBeansOnLookup() {
    if (beanFactoryMustThrow) {
      verifyException(() -> lazyMuleArtifactContext.getBean(MY_FLOW), NoSuchBeanDefinitionException.class);
    } else {
      lazyMuleArtifactContext.getBean(MY_FLOW);
    }
    assertThat(initializations.get(), is(0));
    verify(beanFactory, never()).registerBeanDefinition(eq(MY_FLOW), any());
  }

  @Test
  public void whenInitializationIsDoneThenShouldCreateBeansOnLookup() {
    lazyMuleArtifactContext.initializeComponent(builderFromStringRepresentation(ANOTHER_FLOW).build());

    lazyMuleArtifactContext.getBean(MY_FLOW);
    assertThat(initializations.get(), is(1));
    verify(beanFactory).registerBeanDefinition(eq(MY_FLOW), any());
  }

  @Test
  public void whenBeanAlreadyInitializedThenShouldNotDoItAgain() {
    Location location = builderFromStringRepresentation(MY_FLOW).build();

    lazyMuleArtifactContext.initializeComponent(location);

    lazyMuleArtifactContext.getBean(MY_FLOW);
    lazyMuleArtifactContext.getBean(MY_FLOW);

    assertThat(initializations.get(), is(1));
    verify(beanFactory).registerBeanDefinition(eq(MY_FLOW), any());
  }

  @Test
  public void whenCallingUnsupportedOperationsBeforeInitializationThenDelegates() {
    if (beanFactoryMustThrow) {
      verifyException(() -> lazyMuleArtifactContext.getBean(MY_FLOW, 1), NoSuchBeanDefinitionException.class);
      verifyException(() -> lazyMuleArtifactContext.getBean(Object.class), NoSuchBeanDefinitionException.class);
      verifyException(() -> lazyMuleArtifactContext.getBean(Object.class, 1), NoSuchBeanDefinitionException.class);
      verifyException(() -> lazyMuleArtifactContext.getBeanProvider(Object.class), NoSuchBeanDefinitionException.class);
      verifyException(() -> lazyMuleArtifactContext.getBeanProvider(ResolvableType.forClass(Object.class)),
                      NoSuchBeanDefinitionException.class);
    } else {
      lazyMuleArtifactContext.getBean(MY_FLOW, 1);
      lazyMuleArtifactContext.getBean(Object.class);
      lazyMuleArtifactContext.getBean(Object.class, 1);
      lazyMuleArtifactContext.getBeanProvider(Object.class);
      lazyMuleArtifactContext.getBeanProvider(ResolvableType.forClass(Object.class));
    }
  }

  @Test
  public void whenCallingUnsupportedOperationsAfterInitializationThenThrowsException() {
    lazyMuleArtifactContext.initializeComponent(builderFromStringRepresentation(ANOTHER_FLOW).build());
    verifyUnsupportedOperationException(() -> lazyMuleArtifactContext.getBean(MY_FLOW, 1));
    verifyUnsupportedOperationException(() -> lazyMuleArtifactContext.getBean(Object.class));
    verifyUnsupportedOperationException(() -> lazyMuleArtifactContext.getBean(Object.class, 1));
    verifyUnsupportedOperationException(() -> lazyMuleArtifactContext.getBeanProvider(Object.class));
    verifyUnsupportedOperationException(() -> lazyMuleArtifactContext.getBeanProvider(ResolvableType.forClass(Object.class)));
  }

  @Test
  public void whenCallingGetBeanTypeSafeThenBeanIsInitialized() {
    verifyInitialized(() -> lazyMuleArtifactContext.getBean(MY_FLOW, Object.class));
  }

  @Test
  public void whenCallingContainsBeanThenBeanIsInitialized() {
    assumeThat(beanFactoryMustThrow, is(true));
    verifyInitialized(() -> lazyMuleArtifactContext.containsBean(MY_FLOW));
  }

  @Test
  public void whenCallingIsSingletonThenBeanIsInitialized() {
    assumeThat(beanFactoryMustThrow, is(true));
    verifyInitialized(() -> lazyMuleArtifactContext.isSingleton(MY_FLOW));
  }

  @Test
  public void whenCallingIsPrototypeThenBeanIsInitialized() {
    assumeThat(beanFactoryMustThrow, is(true));
    verifyInitialized(() -> lazyMuleArtifactContext.isPrototype(MY_FLOW));
  }

  @Test
  public void whenCallingIsTypeMatchThenBeanIsInitialized() {
    assumeThat(beanFactoryMustThrow, is(true));
    verifyInitialized(() -> lazyMuleArtifactContext.isTypeMatch(MY_FLOW, Object.class));
  }

  @Test
  public void whenCallingIsTypeMatchResolvableTypeThenBeanIsInitialized() {
    assumeThat(beanFactoryMustThrow, is(true));
    verifyInitialized(() -> lazyMuleArtifactContext.isTypeMatch(MY_FLOW, ResolvableType.forClass(Object.class)));
  }

  @Test
  public void whenCallingGetTypeThenBeanIsInitialized() {
    verifyInitialized(() -> lazyMuleArtifactContext.getType(MY_FLOW));
  }

  @Test
  public void whenCallingGetTypeAllowFactoryBeanInitThenBeanIsInitialized() {
    verifyInitialized(() -> lazyMuleArtifactContext.getType(MY_FLOW, true));
  }

  @Test
  public void whenCallingGetAliasesThenBeanIsInitialized() {
    verifyInitialized(() -> lazyMuleArtifactContext.getAliases(MY_FLOW));
  }

  private void verifyUnsupportedOperationException(Runnable runnable) {
    verifyException(runnable, UnsupportedOperationException.class);
  }

  private void verifyException(Runnable runnable, Class<? extends Throwable> throwableCls) {
    Matcher<? super Throwable> matcher = instanceOf(throwableCls);
    try {
      runnable.run();
      fail(format("Expected test to throw: %s", matcher));
    } catch (Throwable t) {
      assertThat(t, matcher);
    }
  }

  private void verifyInitialized(Runnable runnable) {
    lazyMuleArtifactContext.initializeComponent(builderFromStringRepresentation(ANOTHER_FLOW).build());
    runnable.run();
    assertThat(initializations.get(), is(1));
    verify(beanFactory).registerBeanDefinition(eq(MY_FLOW), any());
  }

  protected ArtifactDeclaration getArtifactDeclaration() {
    return newArtifact()
        .withGlobalElement(forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(MY_FLOW)
            .getDeclaration())
        .withGlobalElement(forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(ANOTHER_FLOW)
            .getDeclaration())
        .getDeclaration();
  }


  @Override
  protected void onProcessorInitialization() {
    initializations.incrementAndGet();
  }

  @Override
  protected DefaultListableBeanFactory doCreateBeanFactoryMock() {
    // Mocks the Bean Factory in a way that simulates not finding beans until the bean definition is registered
    DefaultListableBeanFactory beanFactory = mock(ObjectProviderAwareBeanFactory.class, beanFactoryDefaultAnswer);
    doAnswer(beanFactoryDefaultAnswer.getAnswerForRegisterBeanDefinition())
        .when(beanFactory)
        .registerBeanDefinition(any(), any());
    return beanFactory;
  }

  /**
   * An {@link Answer} meant to be used for {@link BeanFactory} that will simulate not finding beans until the corresponding bean
   * definitions are registered.
   */
  private static class FakeBeanFactoryDefaultAnswer implements Answer<Object> {

    private boolean beanFactoryMustThrow = false;
    private final Set<String> registeredBeans = new HashSet<>();

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
      if (!beanFactoryMustThrow || !isMethodFromBeanFactoryInterface(invocation.getMethod())) {
        return RETURNS_DEFAULTS.answer(invocation);
      }

      if (invocation.getArgument(0) instanceof Class) {
        throw new NoSuchBeanDefinitionException(invocation.getArgument(0, Class.class));
      } else if (invocation.getArgument(0) instanceof ResolvableType) {
        throw new NoSuchBeanDefinitionException(invocation.getArgument(0, ResolvableType.class));
      }

      if (registeredBeans.contains(invocation.getArgument(0, String.class))) {
        return RETURNS_DEFAULTS.answer(invocation);
      } else {
        throw new NoSuchBeanDefinitionException(invocation.getArgument(0, String.class));
      }
    }

    public Answer<?> getAnswerForRegisterBeanDefinition() {
      return invocation -> {
        registeredBeans.add(invocation.getArgument(0, String.class));
        return null;
      };
    }

    public void setBeanFactoryMustThrow(boolean beanFactoryMustThrow) {
      this.beanFactoryMustThrow = beanFactoryMustThrow;
    }

    private static boolean isMethodFromBeanFactoryInterface(Method method) {
      try {
        BeanFactory.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
        return true;
      } catch (NoSuchMethodException e) {
        return false;
      }
    }
  }
}
