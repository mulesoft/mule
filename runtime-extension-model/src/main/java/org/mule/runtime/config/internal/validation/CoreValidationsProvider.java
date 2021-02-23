/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Arrays.asList;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationsProvider;

import java.util.List;

public class CoreValidationsProvider implements ValidationsProvider {

  @Override
  public List<Validation> get() {
    return asList(new SingletonsAreNotRepeated(),
                  new SingletonsPerFileAreNotRepeated(),
                  new NamedTopLevelElementsHaveName(),
                  new NameHasValidCharacters(),
                  new NameIsNotRepeated(),
                  // make this general for all references via stereotypes
                  new FlowRefPointsToExistingFlow(),
                  new SourceErrorMappingAnyNotRepeated(),
                  new SourceErrorMappingAnyLast(),
                  new SourceErrorMappingTypeNotRepeated(),
                  new ErrorHandlerRefOrOnErrorExclusiveness(),
                  new ErrorHandlerOnErrorHasTypeOrWhen(),
                  new RaiseErrorTypeReferencesPresent(),
                  new RaiseErrorTypeReferencesExist(),
                  new ErrorMappingTargetTypeReferencesExist(),
                  new ErrorMappingSourceTypeReferencesExist(),
                  new ErrorHandlerOnErrorTypeExists()
    // TODO MULE-17711 (AST) re-enable (and possibly refactor) this validation
    // new ParameterAndChildForSameAttributeNotDefinedTogether(),
    // Commented out because this causes failures because of a lying extension model for munit, in the 'ignore' parameter
    // new NoExpressionsInNoExpressionsSupportedParams()
    // validate expressions!
    );
  }


}
