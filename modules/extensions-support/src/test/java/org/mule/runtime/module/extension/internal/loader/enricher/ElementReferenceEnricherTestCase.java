/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.meta.model.parameter.ElementReference.ElementType.CONFIG;
import static org.mule.runtime.api.meta.model.parameter.ElementReference.ElementType.FLOW;
import static org.mule.runtime.api.meta.model.parameter.ElementReference.ElementType.OBJECT_STORE;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.loadExtension;
import static org.mule.test.marvel.MarvelExtension.MARVEL_EXTENSION;
import static org.mule.test.marvel.ironman.IronMan.CONFIG_NAME;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ElementReference;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.marvel.drstrange.DrStrange;

import java.util.List;

import org.junit.Test;

public class ElementReferenceEnricherTestCase extends AbstractMuleTestCase {

  private final ExtensionModel extension = loadExtension(MarvelExtension.class);
  private final ConfigurationModel configuration = extension.getConfigurationModel(DrStrange.CONFIG_NAME).get();

  @Test
  public void connectionProviderWithMultipleConfigReferenceParameter() {
    ParameterModel paramWithReferences = configuration.getConnectionProviders().get(0).getAllParameterModels().get(0);
    List<ElementReference> references = paramWithReferences.getElementReferences();
    assertThat(references, hasSize(2));
    assertReference(references.get(0), HeisenbergExtension.HEISENBERG, "config", CONFIG);
    assertReference(references.get(1), MARVEL_EXTENSION, CONFIG_NAME, CONFIG);
  }

  @Test
  public void configWithObjectStoreReference() {
    ParameterModel osParam = configuration.getAllParameterModels().stream()
        .filter(p -> p.getName().equals("spellStore"))
        .findFirst().get();

    assertThat(osParam.getElementReferences(), hasSize(1));
    ElementReference reference = osParam.getElementReferences().get(0);
    assertThat(reference.getNamespace(), equalTo("os"));
    assertThat(reference.getElementName(), equalTo("objectStore"));
    assertThat(reference.getType(), is(OBJECT_STORE));
  }

  @Test
  public void operationParameterWithFlowReferenceParameter() {
    OperationModel operation = configuration.getOperationModel("withFlowReference").get();
    assertThat(operation.getAllParameterModels(), hasSize(1));
    ParameterModel param = operation.getAllParameterModels().get(0);
    List<ElementReference> references = param.getElementReferences();
    assertThat(references, hasSize(1));
    assertReference(references.get(0), "mule", "flow", FLOW);
  }

  @Test
  public void configurationWithConfigReferenceParameter() {
    List<ParameterModel> params = configuration.getAllParameterModels();
    assertThat(params, hasSize(2));
    ParameterModel param = params.get(0);
    List<ElementReference> references = param.getElementReferences();
    assertThat(references, hasSize(1));
    assertReference(references.get(0), MARVEL_EXTENSION, CONFIG_NAME, CONFIG);
  }

  private void assertReference(ElementReference reference, String ns, String name, ElementReference.ElementType type) {
    assertThat(reference.getNamespace(), is(ns));
    assertThat(reference.getElementName(), is(name));
    assertThat(reference.getType(), is(type));
  }
}
