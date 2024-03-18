/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.meta.Category;

import java.util.List;

/**
 * A contract for an element from which an Extension can be derived
 *
 * @since 4.0
 */
@NoImplement
public interface ExtensionElement extends ParameterizableTypeElement, ComponentElement, WithOperations, WithFunctions {

  /**
   * @return A list {@link ConfigurationElement} of declared configurations
   */
  List<ConfigurationElement> getConfigurations();

  /**
   * @return The Extension's {@link Category}
   */
  Category getCategory();

  /**
   * @return The Extension's Vendor
   */
  String getVendor();

  /**
   * @return The Extension's Name
   */
  String getName();
}
