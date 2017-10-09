/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validator;

import static java.lang.String.format;
import org.mule.apache.xerces.util.XMLChar;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

/**
 * {@link ExtensionModelValidator} which applies to {@link ExtensionModel}s which are XML based, to validate every name defined
 * within the module is XML valid accordingly to <a href="http://www.w3.org/TR/xmlschema-2/#NCName">http://www.w3.org/TR/xmlschema-2/#NCName</a>
 *
 * @since 4.0
 */
public class CorrectXmlNamesValidator implements ExtensionModelValidator {

  public static final String WRONG_XML_NAME_FORMAT_MESSAGE =
      "The value [%s] is not XML valid accordingly to XML naming conventions (see http://www.w3.org/TR/xmlschema-2/#NCName)";

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new ExtensionWalker() {

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel operationModel) {
        validateXmlName(operationModel, problemsReporter);
      }

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validateXmlName(model, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void validateXmlName(NamedObject model, ProblemsReporter problemsReporter) {
    if (!XMLChar.isValidNCName(model.getName())) {
      problemsReporter.addError(new Problem(model, format(
                                                          WRONG_XML_NAME_FORMAT_MESSAGE, model.getName())));
    }
  }
}
