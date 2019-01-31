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
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
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
import org.mule.runtime.module.extension.internal.loader.enricher.stereotypes.CustomStereotypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionDeclarationTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.stereotypes.EmpireStereotype;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.marvel.drstrange.DrStrange;
import org.mule.test.marvel.ironman.IronMan;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class StereotypesDeclarationEnricherTestCase extends AbstractJavaExtensionDeclarationTestCase {

  public static final String MARVEL_NAMESPACE = MARVEL_EXTENSION.toUpperCase();
  private final ExtensionModel marvelExtension = loadExtension(MarvelExtension.class);
  private final ExtensionModel heisenbergExtension = loadExtension(HeisenbergExtension.class);
  private final ConfigurationModel configuration = marvelExtension.getConfigurationModel(DrStrange.CONFIG_NAME).get();

  @Test
  public void connectionProviderWithMultipleConfigReferenceParameter() {
    ParameterModel paramWithReferences = configuration.getConnectionProviders().get(0).getAllParameterModels().get(0);
    List<StereotypeModel> allowedStereotypes = paramWithReferences.getAllowedStereotypes();
    assertThat(allowedStereotypes, hasSize(2));
    assertStereotype(allowedStereotypes.get(0), HEISENBERG, "config", CONFIG);
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
    Optional<ObjectType> withStereoType = marvelExtension.getTypes().stream()
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
    assertThat(operation.getAllParameterModels(), hasSize(3));
    ParameterModel param = operation.getAllParameterModels().get(0);
    List<StereotypeModel> stereotypes = param.getAllowedStereotypes();
    assertThat(stereotypes, hasSize(1));
    assertThat(stereotypes.get(0), is(FLOW));
  }

  @Test
  public void configurationWithConfigReferenceParameter() {
    List<ParameterModel> params = configuration.getAllParameterModels();
    assertThat(params, hasSize(4));
    ParameterModel param = params.get(0);
    List<StereotypeModel> allowedStereotypes = param.getAllowedStereotypes();
    assertThat(allowedStereotypes, hasSize(1));
    assertStereotype(allowedStereotypes.get(0), MARVEL_EXTENSION, CONFIG_NAME, CONFIG);
  }

  @Test
  public void defaultConfigStereotype() {
    StereotypeModel stereotypeModel = marvelExtension.getConfigurationModel(IronMan.CONFIG_NAME).get().getStereotype();

    assertThat(stereotypeModel.isAssignableTo(CONFIG), is(true));
    assertStereotype(stereotypeModel, MARVEL_NAMESPACE, "IRON_MAN", CONFIG);
  }

  @Test
  public void defaultConnectionStereotype() {
    StereotypeModel stereotypeModel = marvelExtension.getConfigurationModel(DrStrange.CONFIG_NAME).get()
        .getConnectionProviderModel("mystic").get().getStereotype();

    assertThat(stereotypeModel.isAssignableTo(CONNECTION), is(true));
    assertStereotype(stereotypeModel, MARVEL_NAMESPACE, "MYSTIC", CONNECTION);
  }

  @Test
  public void defaultProcessorStereotype() {
    StereotypeModel stereotypeModel = marvelExtension.getConfigurationModel(IronMan.CONFIG_NAME).get()
        .getOperationModel("fireMissile").get().getStereotype();

    assertThat(stereotypeModel.isAssignableTo(PROCESSOR), is(true));
    StereotypeModel expectedParent = newStereotype(PROCESSOR.getType(), MARVEL_NAMESPACE).withParent(PROCESSOR).build();
    assertStereotype(stereotypeModel, MARVEL_NAMESPACE, "FIRE_MISSILE", expectedParent);
  }

  @Test
  public void defaultSourceStereotype() {
    StereotypeModel stereotypeModel = marvelExtension.getConfigurationModel(DrStrange.CONFIG_NAME).get()
        .getSourceModel("bytes-caster").get().getStereotype();

    assertThat(stereotypeModel.isAssignableTo(SOURCE), is(true));
    StereotypeModel expectedParent = newStereotype(SOURCE.getType(), MARVEL_NAMESPACE).withParent(SOURCE).build();
    assertStereotype(stereotypeModel, MARVEL_NAMESPACE, "BYTES-CASTER", expectedParent);
  }

  @Test
  public void defaultConstructStereotype() {
    StereotypeModel stereotypeModel = heisenbergExtension.getConstructModel("simpleRouter").get().getStereotype();

    assertThat(stereotypeModel.isAssignableTo(PROCESSOR), is(true));
    String namespace = HEISENBERG.toUpperCase();
    StereotypeModel expectedParent = newStereotype(PROCESSOR.getType(), namespace).withParent(PROCESSOR).build();
    assertStereotype(stereotypeModel, namespace, "SIMPLE_ROUTER", expectedParent);
  }

  @Test
  public void customStereotype() {
    OperationModel operation = heisenbergExtension.getConfigurationModels().get(0).getOperationModel("callSaul").get();

    StereotypeModel stereotypeModel = operation.getStereotype();
    assertThat(stereotypeModel.isAssignableTo(PROCESSOR), is(true));

    assertThat(stereotypeModel.getType(), is(new EmpireStereotype().getName().toUpperCase()));
    assertThat(stereotypeModel.getNamespace(), is(HEISENBERG.toUpperCase()));
    assertThat(stereotypeModel.getParent().get(), is(PROCESSOR));

    assertThat(operation.getModelProperty(CustomStereotypeModelProperty.class).isPresent(), is(true));
  }

  @Test
  public void validatorStereotype() {
    OperationModel operation = heisenbergExtension.getOperationModel("validateMoney").get();

    StereotypeModel stereotypeModel = operation.getStereotype();
    assertThat(stereotypeModel.isAssignableTo(PROCESSOR), is(true));
    assertThat(stereotypeModel.isAssignableTo(VALIDATOR), is(true));

    assertThat(stereotypeModel.getType(), is(VALIDATOR_DEFINITION.getName()));
    assertThat(stereotypeModel.getNamespace(), is(HEISENBERG.toUpperCase()));
    assertThat(stereotypeModel.getParent().get(), is(VALIDATOR));
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
      assertStereotype(stereotypeModel.getParent().get(),
                       parent.getNamespace(), parent.getType(), parent.getParent().orElse(null));
    } else {
      assertThat(expected.getParent().isPresent(), is(false));
    }
  }
}
