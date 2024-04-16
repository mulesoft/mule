/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static java.util.stream.Collectors.toList;

import io.qameta.allure.Issue;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelDeclarer;
import org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurer;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Issue("W-15261626")
public class CoreExtensionDeclarerTestCase {

  private List<ParameterizedDeclarer> routersOneOf = new ArrayList<>();
  private List<ParameterizedDeclarer> routersAllOf = new ArrayList<>();
  private List<ParameterizedDeclarer> scopesPassThrough = new ArrayList<>();

  @Test
  public void configurationOfExtensionDeclarer() {
    MuleExtensionModelDeclarer declarer = new MuleExtensionModelDeclarer(MockConfigurer::new);
    declarer.createExtensionModel();
    assertThat(routersAllOf, hasSize(1));
    assertThat(getNameOfDeclarer(routersAllOf.get(0)), is("scatterGather"));
    assertThat(routersOneOf, hasSize(3));
    assertThat(routersOneOf.stream().map(this::getNameOfDeclarer).collect(toList()),
               containsInAnyOrder("choice", "firstSuccessful", "roundRobin"));
    assertThat(scopesPassThrough, hasSize(3));
    assertThat(scopesPassThrough.stream().map(this::getNameOfDeclarer).collect(toList()),
               containsInAnyOrder("untilSuccessful", "parallelForeach", "try"));
  }

  private String getNameOfDeclarer(ParameterizedDeclarer declarer) {
    return ((NamedDeclaration) declarer.getDeclaration()).getName();
  }


  private class MockConfigurer implements ComponentMetadataConfigurer {

    private List<ParameterizedDeclarer> selected;

    @Override
    public <T extends ParameterizedDeclaration> void configureNullMetadata(ParameterizedDeclaration<T> declaration) {

    }

    @Override
    public <T extends ParameterizedDeclarer, D extends ParameterizedDeclaration> void configureNullMetadata(ParameterizedDeclarer<T, D> declarer) {

    }

    @Override
    public ComponentMetadataConfigurer setOutputTypeResolver(OutputTypeResolver outputTypeResolver) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer setAttributesTypeResolver(AttributesTypeResolver attributesTypeResolver) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer setKeysResolver(TypeKeysResolver keysResolver, String keyParameterName,
                                                       MetadataType keyParameterType, boolean isPartialKeyResolver) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer setChainInputTypeResolver(ChainInputTypeResolver chainInputTypeResolver) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer addInputResolver(String parameterName, InputTypeResolver resolver) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer addInputResolvers(Map<String, InputTypeResolver> resolvers) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer addRouteChainInputResolver(String routeName, ChainInputTypeResolver resolver) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer addRoutesChainInputResolvers(Map<String, ChainInputTypeResolver> resolvers) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer setConnected(boolean connected) {
      return this;
    }

    @Override
    public ComponentMetadataConfigurer asOneOfRouter() {
      selected = routersOneOf;
      return this;
    }

    @Override
    public ComponentMetadataConfigurer asPassthroughScope() {
      selected = scopesPassThrough;
      return this;
    }

    @Override
    public ComponentMetadataConfigurer asAllOfRouter() {
      selected = routersAllOf;
      return this;
    }

    @Override
    public <T extends ParameterizedDeclarer, D extends ParameterizedDeclaration> void configure(ParameterizedDeclarer<T, D> declarer) {
      selected.add(declarer);
    }

    @Override
    public <T extends ComponentDeclaration> void configure(ParameterizedDeclaration<T> declaration) {

    }
  }

}
