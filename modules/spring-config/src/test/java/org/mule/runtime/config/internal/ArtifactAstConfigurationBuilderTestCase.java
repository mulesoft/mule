/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.internal.context.BaseSpringMuleContextServiceConfigurator.DISABLE_TRANSFORMERS_SUPPORT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import static java.util.Collections.emptyMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.lazy.LazyExpressionLanguageAdaptor;
import org.mule.runtime.config.internal.registry.BaseSpringRegistry;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.ExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.mockito.ArgumentCaptor;

import io.qameta.allure.Issue;

public class ArtifactAstConfigurationBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  public static final String SCHEMA_VALIDATION_ERROR =
      "Can't resolve http://www.mulesoft.org/schema/mule/invalid-namespace/current/invalid-schema.xsd, A dependency or plugin might be missing";

  private MuleContextWithRegistry muleContext;

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public SystemProperty disableExpressionsSupport = new SystemProperty(DISABLE_TRANSFORMERS_SUPPORT, "true");

  @Before
  public void setUp() throws Exception {
    muleContext = mockContextWithServices();
    muleContext.getInjector().inject(this);
  }

  @Test
  @Issue("W-11745207")
  public void baseRegistryWithLazyInitialisation() throws Exception {
    doTestBaseRegistryExpressionLanguageAdapter(true, LazyExpressionLanguageAdaptor.class);
  }

  @Test
  @Issue("W-11745207")
  public void baseRegistryWithEagerInitialisation() throws Exception {
    doTestBaseRegistryExpressionLanguageAdapter(false, DataWeaveExpressionLanguageAdaptor.class);
  }

  private void doTestBaseRegistryExpressionLanguageAdapter(boolean lazyInit, Class expectedClass) throws IOException,
      ConfigurationException {
    final ArtifactAstConfigurationBuilder configurationBuilder =
        astConfigurationBuilderRelativeToPath(tempFolder.getRoot(), emptyArtifact(), lazyInit);
    ArgumentCaptor<Registry> registryCaptor = ArgumentCaptor.forClass(Registry.class);
    configurationBuilder.configure(muleContext);

    verify(muleContext, atLeastOnce()).setRegistry(registryCaptor.capture());

    List<Registry> registries = registryCaptor.getAllValues();

    assertThat(registries.get(0), instanceOf(BaseSpringRegistry.class));

    BaseSpringRegistry baseSpringRegistry = (BaseSpringRegistry) registries.get(0);
    ExpressionLanguageAdaptor dataWeaveExpressionLanguageAdaptor =
        baseSpringRegistry.get(OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER);

    assertThat(dataWeaveExpressionLanguageAdaptor, is(notNullValue()));
    assertThat(dataWeaveExpressionLanguageAdaptor, instanceOf(expectedClass));
  }

  @Test
  public void memoryManagementCanBeInjectedInBean() throws MuleException, IOException {
    final ArtifactAstConfigurationBuilder configurationBuilder =
        astConfigurationBuilderRelativeToPath(tempFolder.getRoot(), emptyArtifact(), false);

    configurationBuilder.configure(muleContext);
    final ArtifactContext artifactContext = configurationBuilder.createArtifactContext();
    MemoryManagementInjected memoryManagementInjected = new MemoryManagementInjected();
    artifactContext.getMuleContext().getInjector().inject(memoryManagementInjected);

    assertThat(memoryManagementInjected.getMemoryManagementService(), is(notNullValue()));
  }

  @Test
  @Issue("W-13969259")
  public void baseRegistryDisposedOnDeploymentError() throws IOException {
    final ArtifactAstConfigurationBuilder configurationBuilder =
        astConfigurationBuilderRelativeToPath(tempFolder.getRoot(), emptyArtifact(), false);

    final ArgumentCaptor<Registry> registryCaptor = forClass(Registry.class);
    doNothing()
        .when((DefaultMuleContext) muleContext)
        .setRegistry(registryCaptor.capture());

    doThrow(IllegalStateException.class)
        .when((DefaultMuleContext) muleContext)
        // some method called within the configuration that uses the baseRegistry
        .getExtensionManager();

    try {
      configurationBuilder.configure(muleContext);
      fail("Expected IllegalStateException");
    } catch (ConfigurationException e) {
      assertThat(e.getCause(), instanceOf(IllegalStateException.class));
      final BaseSpringRegistry baseRegistry = (BaseSpringRegistry) (registryCaptor.getValue());
      assertThat(baseRegistry.getLifecycleManager().getLastExecutedPhase(), is(Disposable.PHASE_NAME));
    }
  }

  private ArtifactAstConfigurationBuilder astConfigurationBuilderRelativeToPath(File basePath, ArtifactAst artifactAst,
                                                                                boolean lazyInit)
      throws IOException {
    return withContextClassLoader(new URLClassLoader(new URL[] {basePath.toURI().toURL()}, null),
                                  () -> new ArtifactAstConfigurationBuilder(artifactAst, emptyMap(), APP, lazyInit, false));
  }

  public static final class TestExtensionSchemagenerator implements ExtensionSchemaGenerator {

    @Override
    public String generate(ExtensionModel extensionModel, DslResolvingContext context) {
      return "";
    }
  }


  /**
   * Class to test the injection of a {@link MemoryManagementService}
   */
  private static class MemoryManagementInjected {

    @Inject
    private MemoryManagementService memoryManagementService;

    public MemoryManagementService getMemoryManagementService() {
      return memoryManagementService;
    }
  }
}
