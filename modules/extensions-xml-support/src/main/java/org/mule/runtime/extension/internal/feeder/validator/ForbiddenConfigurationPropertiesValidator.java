/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validator;

import static org.mule.runtime.config.api.properties.PropertiesResolverUtils.loadProviderFactories;
import static java.lang.String.format;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.internal.ast.property.GlobalElementComponentModelModelProperty;

import java.util.Set;

/**
 * {@link ExtensionModelValidator} which applies to {@link ExtensionModel}s which are XML based, as those that contain usages of
 * configuration properties. If the developer wants to use constants, then it must rely on <mule:global-property/>'s than in the
 * properties file.
 *
 * @since 4.1.2
 */
public class ForbiddenConfigurationPropertiesValidator implements ExtensionModelValidator {

  public static final String CONFIGURATION_PROPERTY_NOT_SUPPORTED_FORMAT_MESSAGE =
      "Configuration properties is not supported, either use <mule:global-property ../>, ${file::file.txt} or <module:property/> instead. Offending global element '%s'";

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).ifPresent(modelProperty -> {
      final Set<ComponentIdentifier> configurationPropertiesCollection = loadProviderFactories().keySet();
      modelProperty.getGlobalElements().forEach(globalElementComponentModel -> {
        if (configurationPropertiesCollection.contains(globalElementComponentModel.getIdentifier())) {
          problemsReporter.addError(new Problem(extensionModel, format(CONFIGURATION_PROPERTY_NOT_SUPPORTED_FORMAT_MESSAGE,
                                                                       globalElementComponentModel.getIdentifier())));
        }
      });
    });
  }

}
