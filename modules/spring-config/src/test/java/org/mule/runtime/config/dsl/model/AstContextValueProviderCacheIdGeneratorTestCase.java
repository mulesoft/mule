/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.metadata.ContextBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.api.dsl.model.metadata.context.AstValueProviderCacheIdGeneratorContext;
import org.mule.runtime.config.api.dsl.model.metadata.context.ValueProviderCacheIdGeneratorContext;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;

import java.util.Optional;

public class AstContextValueProviderCacheIdGeneratorTestCase extends AbstractValueProviderCacheIdGeneratorTestCase {

  private ValueProviderCacheIdGenerator<ValueProviderCacheIdGeneratorContext<?>> contextBasedValueProviderCacheIdGenerator =
      new ContextBasedValueProviderCacheIdGenerator();

  @Override
  protected Optional<ValueProviderCacheId> computeIdFor(ArtifactDeclaration appDeclaration,
                                                        String location,
                                                        String parameterName)
      throws Exception {
    ApplicationModel app = loadApplicationModel(appDeclaration);
    Locator locator = new Locator(app);
    ComponentAst component = getComponentAst(app, location);

    Optional<ComponentAst> configAst = resolveConfigName(component)
        .flatMap(configName -> locator.get(Location.builderFromStringRepresentation(configName).build()));

    ValueProviderCacheIdGeneratorContext astContext = configAst.map(
                                                                    c -> new AstValueProviderCacheIdGeneratorContext(component,
                                                                                                                     c))
        .orElse(new AstValueProviderCacheIdGeneratorContext(component));

    return contextBasedValueProviderCacheIdGenerator.getIdForResolvedValues(astContext, parameterName);
  }

}
