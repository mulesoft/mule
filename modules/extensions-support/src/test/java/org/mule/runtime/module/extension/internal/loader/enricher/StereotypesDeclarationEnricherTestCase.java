/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.test.marvel.MarvelExtension.MARVEL_EXTENSION;
import static org.mule.test.marvel.drstrange.DrStrangeStereotypeDefinition.DR_STRANGE_STEREOTYPE_NAME;
import static org.mule.test.marvel.ironman.IronMan.CONFIG_NAME;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.marvel.drstrange.DrStrange;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class StereotypesDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private final ExtensionModel extension = loadExtension(MarvelExtension.class);
  private final ExtensionModel heisenbergExtension = loadExtension(HeisenbergExtension.class);
  private final ConfigurationModel configuration = extension.getConfigurationModel(DrStrange.CONFIG_NAME).get();

  @Test
  public void connectionProviderWithMultipleConfigReferenceParameter() {
    ParameterModel paramWithReferences = configuration.getConnectionProviders().get(0).getAllParameterModels().get(0);
    List<StereotypeModel> allowedStereotypes = paramWithReferences.getAllowedStereotypes();
    assertThat(allowedStereotypes, hasSize(2));
    assertStereotype(allowedStereotypes.get(0), HeisenbergExtension.HEISENBERG, "config", CONFIG);
    assertStereotype(allowedStereotypes.get(1), MARVEL_EXTENSION, CONFIG_NAME, CONFIG);
  }

  @Test
  public void configWithObjectStoreReference() {
    ParameterModel osParam = configuration.getAllParameterModels().stream()
        .filter(p -> p.getName().equals("spellStore"))
        .findFirst().get();

    assertThat(osParam.getAllowedStereotypes(), hasSize(1));
    StereotypeModel stereotypeModel = osParam.getAllowedStereotypes().get(0);
    assertThat(stereotypeModel, is(OBJECT_STORE));
  }

  @Test
  public void exportedTypesWithStereotypes() {
    Optional<ObjectType> withStereoType = extension.getTypes().stream()
        .filter(type -> type.getAnnotation(StereotypeTypeAnnotation.class).isPresent())
        .findFirst();
    assertThat(withStereoType.isPresent(), is(true));
    Optional<StereotypeTypeAnnotation> stereotype = withStereoType.get().getAnnotation(StereotypeTypeAnnotation.class);
    List<StereotypeModel> allowedStereotypes = stereotype.get().getAllowedStereotypes();
    assertThat(allowedStereotypes, hasSize(1));
    assertStereotype(allowedStereotypes.get(0), MARVEL_EXTENSION, DR_STRANGE_STEREOTYPE_NAME, null);
  }

  @Test
  public void sourceParameterWithCustomReference() {
    SourceModel source = configuration.getSourceModel("bytes-caster").get();

    ParameterModel param = source.getAllParameterModels().stream()
        .filter(p -> p.getName().equals("nextOperationReference")).findFirst().get();

    List<StereotypeModel> stereotypes = param.getAllowedStereotypes();
    assertThat(stereotypes, hasSize(1));
    assertThat(stereotypes.get(0).getType(), is("REFERABLE_OPERATION"));
    assertThat(stereotypes.get(0).getNamespace(), is("MARVEL"));
  }

  @Test
  public void operationParameterWithFlowReferenceParameter() {
    OperationModel operation = configuration.getOperationModel("withFlowReference").get();
    assertThat(operation.getAllParameterModels(), hasSize(2));
    ParameterModel param = operation.getAllParameterModels().get(0);
    List<StereotypeModel> stereotypes = param.getAllowedStereotypes();
    assertThat(stereotypes, hasSize(1));
    assertThat(stereotypes.get(0), is(FLOW));
  }

  @Test
  public void configurationWithConfigReferenceParameter() {
    List<ParameterModel> params = configuration.getAllParameterModels();
    assertThat(params, hasSize(3));
    ParameterModel param = params.get(0);
    List<StereotypeModel> allowedStereotypes = param.getAllowedStereotypes();
    assertThat(allowedStereotypes, hasSize(1));
    assertStereotype(allowedStereotypes.get(0), MARVEL_EXTENSION, CONFIG_NAME, CONFIG);
  }

  @Test
  public void allowedStereotypeOnScopeChain() {
    OperationModel operation = heisenbergExtension.getOperationModel("getChain").get();

    NestedChainModel nestedChain = (NestedChainModel) operation.getNestedComponents().get(0);
    assertThat(nestedChain.getAllowedStereotypes().size(), is(1));

    StereotypeModel stereotypeModel = nestedChain.getAllowedStereotypes().iterator().next();
    assertThat(stereotypeModel.toString(), stereotypeModel, is(MuleStereotypes.VALIDATOR));
  }

  private void assertStereotype(StereotypeModel stereotypeModel, String ns, String name, StereotypeModel parent) {
    StereotypeModel expected = newStereotype(name, ns).withParent(parent).build();
    assertThat(stereotypeModel.getNamespace(), is(expected.getNamespace()));
    assertThat(stereotypeModel.getType(), is(expected.getType()));
    if (stereotypeModel.getParent().isPresent()) {
      assertThat(expected.getParent().isPresent(), is(true));
      assertStereotype(stereotypeModel.getParent().get(), parent.getNamespace(), parent.getType(),
                       parent.getParent().orElse(null));
    } else {
      assertThat(expected.getParent().isPresent(), is(false));
    }
  }
}
