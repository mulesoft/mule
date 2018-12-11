/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.PRIMARY_NODE_ONLY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.DEFAULT_ALL_NODES;
import static org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.DEFAULT_PRIMARY_NODE_ONLY;
import static org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.NOT_SUPPORTED;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.annotation.source.ClusterSupport;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClusterSupportEnricherTestCase extends AbstractMuleTestCase {


  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionLoadingContext extensionLoadingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceDeclaration sourceDeclaration;

  @Mock
  private ParameterGroupDeclaration parameterGroup;

  private ClusterSupportEnricher enricher = new ClusterSupportEnricher();
  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Before
  public void before() {
    when(extensionLoadingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getMessageSources()).thenReturn(asList(sourceDeclaration));
    when(sourceDeclaration.getParameterGroup(DEFAULT_GROUP_NAME)).thenReturn(parameterGroup);
    when(sourceDeclaration.getSuccessCallback()).thenReturn(empty());
    when(sourceDeclaration.getErrorCallback()).thenReturn(empty());
  }

  @Test
  public void defaultBehavior() {
    setSourceClass(TestSource.class);
    assertEnrichment(false, true, false);
  }

  @Test
  public void clusterNotSupported() {
    setSourceClass(NonClusteredSource.class);
    assertEnrichment(true, false, false);
  }

  @Test
  public void defaultAllNodes() {
    setSourceClass(AllNodesSource.class);
    assertEnrichment(false, true, false);
  }

  @Test
  public void defaultPrimaryNodeOnly() {
    setSourceClass(PrimaryNodeSource.class);
    assertEnrichment(false, true, true);
  }

  private void assertEnrichment(boolean runsOnPrimaryNodeOnly, boolean parameterAdded, boolean defaultParameterValue) {
    enricher.enrich(extensionLoadingContext);
    verify(sourceDeclaration).setRunsOnPrimaryNodeOnly(runsOnPrimaryNodeOnly);
    if (parameterAdded) {
      ArgumentCaptor<ParameterDeclaration> captor = ArgumentCaptor.forClass(ParameterDeclaration.class);
      verify(parameterGroup).addParameter(captor.capture());

      ParameterDeclaration parameter = captor.getValue();
      assertThat(parameter.getName(), equalTo(PRIMARY_NODE_ONLY_PARAMETER_NAME));
      assertThat(parameter.getDefaultValue(), is(defaultParameterValue));
    } else {
      verify(parameterGroup, never()).addParameter(any());
    }
  }

  private void setSourceClass(Class<? extends Source> sourceClass) {
    ExtensionTypeDescriptorModelProperty property =
        new ExtensionTypeDescriptorModelProperty(new TypeWrapper(sourceClass, typeLoader));
    when(sourceDeclaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class)).thenReturn(Optional.of(property));
  }

  public static class TestSource extends Source {

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }


  @ClusterSupport(NOT_SUPPORTED)
  public static class NonClusteredSource extends TestSource {

  }


  @ClusterSupport(DEFAULT_ALL_NODES)
  public static class AllNodesSource extends TestSource {

  }

  @ClusterSupport(DEFAULT_PRIMARY_NODE_ONLY)
  public static class PrimaryNodeSource extends TestSource {

  }
}
