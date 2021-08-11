/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Parses the syntactic definition of a {@link ParameterGroupModel} so that the semantics reflected in it can be extracted in a
 * uniform way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface ParameterGroupModelParser {

  /**
   * @return the group's name
   */
  String getName();

  /**
   * @return the group's description
   */
  String getDescription();

  /**
   * Returns a list with a {@link ParameterModelParser} per each parameter defined in the group. Each parameter is listed in the
   * same order as defined in the syntax.
   *
   * @return a list with the config's {@link ParameterModelParser}
   */
  List<ParameterModelParser> getParameterParsers();

  /**
   * @return a {@link DisplayModel} if one was defined
   */
  Optional<DisplayModel> getDisplayModel();

  /**
   * @return a {@link LayoutModel} if one was defined
   */
  Optional<LayoutModel> getLayoutModel();

  /**
   * A {@link ExclusiveOptionalDescriptor} describing the groups exclusive optional parameters, if any were defined
   *
   * @return an {@link Optional} {@link ExclusiveOptionalDescriptor}
   */
  Optional<ExclusiveOptionalDescriptor> getExclusiveOptionals();

  /**
   * @return whether this group should be made explicit in the DSL
   */
  boolean showsInDsl();

  /**
   * Returns a list with all the {@link ModelProperty model properties} to be applied at the group level which are specifically
   * linked to the type of syntax used to define the extension.
   *
   * @return a list with {@link ModelProperty} instances.
   */
  List<ModelProperty> getAdditionalModelProperties();

  /**
   * Describes the group's exclusive optional parameters
   */
  class ExclusiveOptionalDescriptor {

    private final Set<String> exclusiveOptionals;
    private final boolean oneRequired;

    /**
     * Creates a new instance
     *
     * @param exclusiveOptionals the names of the parameters which are mutually exclusive
     * @param oneRequired        whether one of these should be enforced to be present
     */
    public ExclusiveOptionalDescriptor(Set<String> exclusiveOptionals, boolean oneRequired) {
      this.exclusiveOptionals = exclusiveOptionals;
      this.oneRequired = oneRequired;
    }

    /**
     * @return The names of the parameters which are mutually exclusive
     */
    public Set<String> getExclusiveOptionals() {
      return exclusiveOptionals;
    }

    /**
     * @return whether one of the parameters should be enforced to be present
     */
    public boolean isOneRequired() {
      return oneRequired;
    }
  }
}
