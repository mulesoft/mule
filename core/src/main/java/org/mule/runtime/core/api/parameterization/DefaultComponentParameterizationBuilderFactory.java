/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.parameterization;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.parameterization.AbstractComponentParameterizationBuilderFactory;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.internal.parameterization.DefaultComponentParameterizationBuilder;

/**
 * Default implementation of the {@link AbstractComponentParameterizationBuilderFactory} interface
 *
 * @since 4.5
 */
public class DefaultComponentParameterizationBuilderFactory
    extends AbstractComponentParameterizationBuilderFactory {

  @Override
  protected <M extends ParameterizedModel> ComponentParameterization.Builder<M> create(M model) {
    return new DefaultComponentParameterizationBuilder(model);
  }

}
