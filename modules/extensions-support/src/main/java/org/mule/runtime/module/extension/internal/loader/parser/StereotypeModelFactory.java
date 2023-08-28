/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

/**
 * Factory for creating {@link StereotypeModel} instances for the context of extension being parsed.
 *
 * @since 4.5.0
 */
public interface StereotypeModelFactory {

  /**
   * @param stereotypeDefinition a {@link StereotypeDefinition}
   * @return a {@link StereotypeModel}
   */
  StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition);

  /**
   * @param stereotypeDefinition a {@link StereotypeDefinition}
   * @param namespace            the namespace of model to be created
   * @return a {@link StereotypeModel}
   */
  StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition, String namespace);

  /**
   * @param name   the stereotype name
   * @param parent the parent stereotype
   * @return a {@link StereotypeModel}
   */
  StereotypeModel createStereotype(String name, StereotypeModel parent);

  /**
   * @param name      the stereotype name
   * @param namespace the namespace of model to be created
   * @param parent    the parent stereotype
   * @return a {@link StereotypeModel}
   */
  StereotypeModel createStereotype(String name, String namespace, StereotypeModel parent);

  /**
   * @return the parent to use when defining stereotypes for a processor
   */
  StereotypeModel getProcessorParentStereotype();

  /**
   * @return the parent to use when defining stereotypes for a source
   */
  StereotypeModel getSourceParentStereotype();

  /**
   * @return a stereotype for validator components
   */
  StereotypeModel getValidatorStereotype();
}
