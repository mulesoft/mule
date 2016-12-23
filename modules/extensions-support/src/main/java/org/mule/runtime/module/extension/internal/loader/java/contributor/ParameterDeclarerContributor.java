/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

/**
 * Contract for parameter declarer contributors, this contributors are injected in the declaration of
 * {@link ParameterModel}s to be able to enrich the models in declaring time.
 *
 * @since 4.0
 */
public interface ParameterDeclarerContributor {

  /**
   * Given an {@link ExtensionParameter} describing the parameter it self and a {@link ParameterDeclarationContext}
   * gives the chance to contribute to the {@link ParameterDeclarer}
   *
   * @param parameter {@link ExtensionParameter} with introspected information of the Java parameter
   * @param declarer declarer to be enriched
   * @param declarationContext context of the parameter to be declared
   */
  void contribute(ExtensionParameter parameter, ParameterDeclarer declarer, ParameterDeclarationContext declarationContext);
}
