/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.config.internal.dsl.model.DefaultDslElementModelFactory;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.Optional;

/**
 * Provides the {@link DslElementModel} of any {@link ComponentConfiguration} within the context of the available application
 * plugins, provided for each instance of this {@link DslElementModelFactory}
 *
 * @since 4.0
 */
public interface DslElementModelFactory {

  /**
   * Provides a default implementation of the {@link DslElementModelFactory}
   *
   * @param context the {@link DslResolvingContext} to be used when performing a
   *        {@link org.mule.runtime.api.component.ComponentIdentifier#getNamespace namespace} or
   *        {@link ElementDeclaration#getName} based lookup for a given {@link ExtensionModel}.
   * @return a default implementation of the {@link DslElementModelFactory}
   */
  static DslElementModelFactory getDefault(DslResolvingContext context) {
    return new DefaultDslElementModelFactory(context);
  }

  /**
   * Resolves the {@link DslElementModel} for the given {@link ElementDeclaration}, providing an element with all the required
   * information for representing this {@code elementDeclaration} element in the DSL and binding it to its {@link ExtensionModel
   * model} component or {@link MetadataType}. This resolution can only be performed from DSL top-level-elements, which have
   * global representations in the {@link ExtensionModel}, so this method will return an {@link Optional#empty} result if the
   * given {@code applicationElement} does not identify either a {@link ConfigurationModel}, {@link OperationModel},
   * {@link SourceModel}
   *
   * @param elementDeclaration the {@link ElementDeclaration} for which its {@link DslElementModel} representation is required.
   * @param <T> the expected model type of the {@link DslElementModel element}
   * @return a {@link DslElementModel} representation of the {@link ElementDeclaration} if one is possible to be built based on
   *         the {@link ExtensionModel extensions} provided as resolution context, or {@link Optional#empty} if no
   *         {@link DslElementModel} could be created for the given {@code applicationElement} with the current extensions
   *         context.
   */
  <T> Optional<DslElementModel<T>> create(ElementDeclaration elementDeclaration);

  /**
   * Resolves the {@link DslElementModel} for the given {@link ComponentConfiguration applicationElement}, providing an element
   * with all the required information for representing this {@code applicationElement} element in the DSL and binding it to its
   * {@link ExtensionModel model} component or {@link MetadataType}. This resolution can only be performed from DSL
   * top-level-elements, which have global representations in the {@link ExtensionModel}, so this method will return an
   * {@link Optional#empty} result if the given {@code applicationElement} does not identify either a {@link ConfigurationModel},
   * {@link OperationModel}, {@link SourceModel} or an {@link ObjectType} than can be expressed as an explicit top level element.
   *
   * @param componentConfiguration the {@link ComponentConfiguration} for which its {@link DslElementModel} representation is
   *        required.
   * @param <T> the expected model type of the {@link DslElementModel element}
   * @return a {@link DslElementModel} representation of the {@link ComponentConfiguration} if one is possible to be built based
   *         on the {@link ExtensionModel extensions} provided as resolution context, or {@link Optional#empty} if no
   *         {@link DslElementModel} could be created for the given {@code applicationElement} with the current extensions
   *         context.
   */
  <T> Optional<DslElementModel<T>> create(ComponentConfiguration componentConfiguration);

}
