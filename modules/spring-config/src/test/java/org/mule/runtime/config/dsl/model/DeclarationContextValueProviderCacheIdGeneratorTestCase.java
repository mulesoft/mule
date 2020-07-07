/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.lang.String.format;
import static junit.framework.TestCase.fail;
import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.model.metadata.ContextBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.context.DeclarationValueProviderCacheIdGeneratorContextFactory;
import org.mule.runtime.config.api.dsl.model.metadata.context.ValueProviderCacheIdGeneratorContext;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;

import java.util.Optional;

public class DeclarationContextValueProviderCacheIdGeneratorTestCase extends AbstractValueProviderCacheIdGeneratorTestCase {

  private ValueProviderCacheIdGenerator<ValueProviderCacheIdGeneratorContext<?>> contextBasedValueProviderCacheIdGenerator =
      new ContextBasedValueProviderCacheIdGenerator();
  private DeclarationValueProviderCacheIdGeneratorContextFactory declarationContextFactory;

  @Override
  public void before() {
    super.before();
    declarationContextFactory =
        new DeclarationValueProviderCacheIdGeneratorContextFactory(DslElementModelFactory.getDefault(dslContext));
  }

  @Override
  protected Optional<ValueProviderCacheId> computeIdFor(ArtifactDeclaration appDeclaration,
                                                        String location,
                                                        String parameterName)
      throws Exception {
    ApplicationModel app = loadApplicationModel(appDeclaration);
    Locator locator = new Locator(app);
    ComponentAst component = getComponentAst(app, location);

    Optional<ParameterizedElementDeclaration> elementDeclaration =
        appDeclaration.findElement(builderFromStringRepresentation(location).build());
    Optional<ParameterizedModel> elementModel = component.getModel(ParameterizedModel.class);

    if (!elementDeclaration.isPresent() || !elementModel.isPresent()) {
      fail(format("missing declaration or model for:", location));
    }

    Optional<ComponentAst> configAst = resolveConfigName(component)
        .flatMap(configName -> locator.get(Location.builderFromStringRepresentation(configName).build()));

    ValueProviderCacheIdGeneratorContext declarationContext = configAst
        .map(
             c -> {
               final Optional<ConfigurationElementDeclaration> configDeclaration =
                   appDeclaration.findElement(builderFromStringRepresentation(c.getLocation().getLocation()).build());
               final Optional<ConfigurationModel> configModel = c.getModel(ConfigurationModel.class);
               if (!configDeclaration.isPresent() || !configModel.isPresent()) {
                 fail(format("missing declaration or model for config: %s", c.getLocation().getLocation()));
               }
               return declarationContextFactory.createContext(elementDeclaration.get(), elementModel.get(),
                                                              configDeclaration.get(), configModel.get());
             })
        .orElse(declarationContextFactory.createContext(elementDeclaration.get(), elementModel.get()));

    return contextBasedValueProviderCacheIdGenerator.getIdForResolvedValues(declarationContext, parameterName);
  }

}
