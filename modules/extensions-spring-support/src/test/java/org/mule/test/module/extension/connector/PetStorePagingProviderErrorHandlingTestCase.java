/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.petstore.extension.PetstoreErrorTypeDefinition.PET_ERROR;

import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.error.ErrorAction;

import org.junit.Test;

public class PetStorePagingProviderErrorHandlingTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "pagingprovider/petstore-paging-provider-error-handling.xml";
  }

  @Test
  public void throwConnectivityError() throws Exception {
    flowRunner("fail-paged")
        .runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));
  }

  @Test
  public void throwCustomModuleException() throws Exception {
    flowRunner("fail-paged")
        .withVariable("throwConnectivity", false)
        .runExpectingException(errorType(PET_ERROR));
  }

  @Test
  public void errorHandlerThrowsModuleException() throws Exception {
    flowRunner("fail-paged-error-handler")
        .withVariable("throwConnectivity", false)
        .runExpectingException(errorType(PET_ERROR));
  }

  @Test
  public void errorHandlerThrowsConnectivity() throws Exception {
    flowRunner("fail-paged-error-handler")
        .withVariable("throwConnectivity", false)
        .withVariable("errorAction", ErrorAction.CONNECTIVITY)
        .runExpectingException(errorType(MuleErrors.CONNECTIVITY));
  }
}
