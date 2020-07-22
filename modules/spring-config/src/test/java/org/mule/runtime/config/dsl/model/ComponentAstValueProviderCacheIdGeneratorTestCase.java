/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.metadata.ComponentAstBasedValueProviderCacheIdGenerator;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;

import java.util.Optional;

public class ComponentAstValueProviderCacheIdGeneratorTestCase extends AbstractValueProviderCacheIdGeneratorTestCase {

  @Override
  protected Optional<ValueProviderCacheId> computeIdFor(ArtifactDeclaration appDeclaration,
                                                        String location,
                                                        String parameterName)
      throws Exception {
    ApplicationModel app = loadApplicationModel(appDeclaration);
    Locator locator = new Locator(app);
    ValueProviderCacheIdGenerator<ComponentAst> cacheIdGenerator = new ComponentAstBasedValueProviderCacheIdGenerator(locator);
    ComponentAst component = getComponentAst(app, location);
    return cacheIdGenerator.getIdForResolvedValues(component, parameterName);
  }
}
