/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.model.extension.xml;

import static java.util.stream.Collectors.toList;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.spring.api.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.property.XmlExtensionModelProperty;

import java.util.List;
import java.util.Set;

/**
 * A {@link MacroExpansionModulesModel} goes over all the parametrized {@link ExtensionModel} by filtering them if they have
 * the {@link XmlExtensionModelProperty} (implies that has to be macro expanded).
 * <p/>
 * For every occurrence that happens, it will expand the operations/configurations by working with the
 * {@link MacroExpansionModuleModel} passing through just one {@link ExtensionModel} to macro expand in the current Mule
 * Application (held by the {@link ApplicationModel}.
 *
 * @since 4.0
 */
public class MacroExpansionModulesModel {

  private final ApplicationModel applicationModel;
  private final List<ExtensionModel> extensions;

  /**
   * From a mutable {@code applicationModel}, it will store it to apply changes when the {@link #expand()} method is executed.
   *
   * @param applicationModel to modify given the usages of elements that belong to the {@link ExtensionModel}s contained in the
   *        {@code extensions} map.
   * @param extensions set with all the loaded {@link ExtensionModel}s from the deployment that will be filtered by looking up
   *        only those that are coming from an XML context through the {@link XmlExtensionModelProperty} property.
   */
  public MacroExpansionModulesModel(ApplicationModel applicationModel, Set<ExtensionModel> extensions) {
    this.applicationModel = applicationModel;
    this.extensions = extensions.stream()
        .filter(extensionModel -> extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent())
        .collect(toList());
  }

  /**
   * Goes through the entire xml mule application looking for the message processors that can be expanded, and then takes care of
   * the global elements.
   */
  public void expand() {
    for (int i = 0; i < extensions.size(); i++) {
      for (ExtensionModel extensionModel : extensions) {
        new MacroExpansionModuleModel(applicationModel, extensionModel).expand();
      }
    }
  }
}
