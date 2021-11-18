/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;
import static org.mule.runtime.config.internal.BaseSpringMuleContextServiceConfigurator.DISABLE_TRANSFORMERS_SUPPORT;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import static java.util.Collections.emptyMap;
import static java.util.Optional.of;

import static org.apache.commons.io.FileUtils.copyURLToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import io.qameta.allure.Issue;

public class SpringXmlConfigurationBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  public static final String SCHEMA_VALIDATION_ERROR =
      "Can't resolve http://www.mulesoft.org/schema/mule/invalid-namespace/current/invalid-schema.xsd, A dependency or plugin might be missing";
  @Inject
  private FeatureFlaggingService featureFlaggingService;

  private SpringXmlConfigurationBuilder configurationBuilderWithUsedInvalidSchema;
  private SpringXmlConfigurationBuilder configurationBuilderWitUnusedInvalidSchema;
  private MuleContextWithRegistry muleContext;

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public SystemProperty disableExpressionsSupport = new SystemProperty(DISABLE_TRANSFORMERS_SUPPORT, "true");

  @Before
  public void setUp() throws Exception {
    muleContext = mockContextWithServices();
    muleContext.getInjector().inject(this);
    configurationBuilderWithUsedInvalidSchema =
        new SpringXmlConfigurationBuilder(new String[] {"invalid-schema.xml"}, new HashMap<>(), APP, false, false);
    configurationBuilderWitUnusedInvalidSchema =
        new SpringXmlConfigurationBuilder(new String[] {"invalid-schema-not-used.xml"}, new HashMap<>(), APP, false,
                                          false);
  }

  @Test
  @Issue("MULE-19534")
  public void configureWithFailOnFirstError() throws ConfigurationException {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString(SCHEMA_VALIDATION_ERROR));
    when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(true);

    configurationBuilderWithUsedInvalidSchema.configure(muleContext);
  }

  @Test
  @Issue("MULE-19534")
  public void configureWithFailAfterTenErrors() throws ConfigurationException {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString(SCHEMA_VALIDATION_ERROR));
    when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(false);

    configurationBuilderWithUsedInvalidSchema.configure(muleContext);
  }

  @Test
  @Issue("MULE-19534")
  public void configureWithFailAfterTenErrorsWillSucceedIfSchemaNotUsed() throws ConfigurationException {
    when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(false);

    configurationBuilderWitUnusedInvalidSchema.configure(muleContext);
  }

  @Test
  @Issue("MULE-19791")
  public void configureWithResourceOutsideClasspathPreservesResourceName() throws ConfigurationException, IOException {
    copyResourceToTemp("simple.xml");
    final SpringXmlConfigurationBuilder configurationBuilder =
        xmlConfigurationBuilderRelativeToPath(tempFolder.getRoot(), new String[] {"simple.xml"});

    configurationBuilder.configure(muleContext);
    final ArtifactContext artifactContext = configurationBuilder.createArtifactContext();
    final ArtifactAst artifactAst = artifactContext.getArtifactAst();
    final ComponentAst componentAst = artifactAst.topLevelComponents().get(0);
    assertThat(componentAst.getMetadata().getFileName(), is(of("simple.xml")));
  }

  @Test
  public void memoryManagementCanBeInjectedInBean() throws MuleException, IOException {
    copyResourceToTemp("simple.xml");
    final SpringXmlConfigurationBuilder configurationBuilder =
        xmlConfigurationBuilderRelativeToPath(tempFolder.getRoot(), new String[] {"simple.xml"});

    configurationBuilder.configure(muleContext);
    final ArtifactContext artifactContext = configurationBuilder.createArtifactContext();
    MemoryManagementInjected memoryManagementInjected = new MemoryManagementInjected();
    artifactContext.getMuleContext().getInjector().inject(memoryManagementInjected);

    assertThat(memoryManagementInjected.getMemoryManagementService(), is(notNullValue()));
  }

  private void copyResourceToTemp(String resourceName) throws IOException {
    final URL originalResource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
    final File simpleAppFileOutsideClassPath = new File(tempFolder.getRoot(), resourceName);
    assertThat(originalResource, is(not(nullValue())));
    copyURLToFile(originalResource, simpleAppFileOutsideClassPath);
  }

  private SpringXmlConfigurationBuilder xmlConfigurationBuilderRelativeToPath(File basePath, String[] resources)
      throws IOException {
    return withContextClassLoader(new URLClassLoader(new URL[] {basePath.toURI().toURL()}, null),
                                  () -> new SpringXmlConfigurationBuilder(resources, emptyMap(), APP, false,
                                                                          false));
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
