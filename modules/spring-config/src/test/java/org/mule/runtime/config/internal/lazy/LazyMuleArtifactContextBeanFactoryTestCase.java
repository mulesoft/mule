/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.lazy;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.forExtension;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ComponentLifeCycle.COMPONENT_LIFE_CYCLE;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.hamcrest.Matcher;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
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

  @Override
  public void setup() throws Exception {
    super.setup();
    // Enables the Bean Factory to throw only after initial setup
    beanFactory.setBeanFactoryMustThrow(beanFactoryMustThrow);
  }

  @Test
  public void whenBeanFactoryThrowsThenThrows() {
    assumeThat(beanFactoryMustThrow, is(true));
    expectedException.expect(NoSuchBeanDefinitionException.class);
    lazyMuleArtifactContext.getBean("non-existent");
    assertThat(beanFactory.isRegisteredBeanDefiniion("non-existent"), equalTo(FALSE));
  }

  @Test
  public void whenBeanFactoryDoesNotThrowThenDontThrow() {
    assumeThat(beanFactoryMustThrow, is(false));
    assertThat(lazyMuleArtifactContext.getBean("non-existent"), is(nullValue()));
    assertThat(beanFactory.isRegisteredBeanDefiniion("non-existent"), equalTo(FALSE));
  }

  @Test
  public void whenInitializationIsNotDoneYetThenShouldNotCreateBeansOnLookup() {
    if (beanFactoryMustThrow) {
      verifyException(() -> lazyMuleArtifactContext.getBean(MY_FLOW), NoSuchBeanDefinitionException.class);
    } else {
      lazyMuleArtifactContext.getBean(MY_FLOW);
    }
    assertThat(initializations.get(), is(0));
    assertThat(beanFactory.isRegisteredBeanDefiniion(MY_FLOW), equalTo(FALSE));
  }

  @Test
  public void whenInitializationIsDoneThenShouldCreateBeansOnLookup() {
    lazyMuleArtifactContext.initializeComponent(builderFromStringRepresentation(ANOTHER_FLOW).build());

    lazyMuleArtifactContext.getBean(MY_FLOW);
    assertThat(initializations.get(), is(1));
    assertThat(beanFactory.isRegisteredBeanDefiniion(MY_FLOW), equalTo(TRUE));
  }

  @Test
  public void whenBeanAlreadyInitializedThenShouldNotDoItAgain() {
    Location location = builderFromStringRepresentation(MY_FLOW).build();

    lazyMuleArtifactContext.initializeComponent(location);

    lazyMuleArtifactContext.getBean(MY_FLOW);
    lazyMuleArtifactContext.getBean(MY_FLOW);

    assertThat(initializations.get(), is(1));
    assertThat(beanFactory.isRegisteredBeanDefiniion(MY_FLOW), equalTo(TRUE));
  }

  @Test
  public void whenCallingOperationsNotSupportingAutomaticInitializationBeforeInitializationThenDelegates() {
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
  public void whenCallingOperationsNotSupportingAutomaticInitializationAfterInitializationThenDelegates() {
    lazyMuleArtifactContext.initializeComponent(builderFromStringRepresentation(ANOTHER_FLOW).build());
    whenCallingOperationsNotSupportingAutomaticInitializationBeforeInitializationThenDelegates();
  }

  @Test
  public void whenCallingGetBeanTypeSafeThenBeanIsInitialized() {
    verifyInitialized(() -> lazyMuleArtifactContext.getBean(MY_FLOW, Object.class));
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
    assertThat(beanFactory.isRegisteredBeanDefiniion(MY_FLOW), equalTo(TRUE));
  }

  @Override
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

}
