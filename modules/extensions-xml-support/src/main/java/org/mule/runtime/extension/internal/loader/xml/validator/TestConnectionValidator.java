/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.extension.internal.loader.xml.validator;

import static java.lang.String.format;
import static org.mule.runtime.extension.internal.loader.xml.XmlExtensionLoaderDelegate.MODULE_CONNECTION_MARKER_ANNOTATION_ATTRIBUTE;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.internal.loader.xml.validator.property.InvalidTestConnectionMarkerModelProperty;

/**
 * {@link ExtensionModelValidator} which applies to {@link ExtensionModel}s which are XML based. It validates global element names
 * are properly written, and there's no name clashing between them.
 *
 * @since 4.1.3
 */
public class TestConnectionValidator implements ExtensionModelValidator {

  public static final String TEST_CONNECTION_SELECTED_ELEMENT_INVALID =
      "The annotated element [%s] with [%s] is not valid to be used as a test connection (the [%s] does not supports it)";

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {

    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        model.getModelProperty(InvalidTestConnectionMarkerModelProperty.class).ifPresent(mp -> {

          problemsReporter.addWarning(new Problem(model,
                                                  format(TEST_CONNECTION_SELECTED_ELEMENT_INVALID,
                                                         mp.getMarkedElement(),
                                                         MODULE_CONNECTION_MARKER_ANNOTATION_ATTRIBUTE,
                                                         mp.getOffendingElement())));
        });

      }
    }.walk(extensionModel);
  }
}
