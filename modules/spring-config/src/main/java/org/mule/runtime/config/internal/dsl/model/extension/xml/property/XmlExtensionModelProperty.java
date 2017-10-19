/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml.property;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModulesModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.Set;

/**
 * Marker {@link ModelProperty} to indicate if the current {@link ExtensionModel} containing it should be expanded in
 * the Mule application.
 * <p>
 * It also works to determine that the {@link ComponentBuildingDefinitionProvider} must NOT be registered
 * (as it's templating, it doesn't make sense to register a definition provider)
 *
 * @since 4.0
 */
public class XmlExtensionModelProperty implements ModelProperty {

  final private Set<String> namespacesDependencies;

  public XmlExtensionModelProperty(Set<String> namespacesDependencies) {
    this.namespacesDependencies = namespacesDependencies;
  }

  /**
   * @return list of namespaces dependencies used in {@link MacroExpansionModulesModel} to determine the order in which the
   * <module/>s must be macro expanded. Not null.
   */
  public Set<String> getNamespacesDependencies() {
    return namespacesDependencies;
  }

  @Override
  public String getName() {
    return "xmlExtensionModelProperty";
  }

  @Override
  public boolean isPublic() {
    return true;
  }
}
