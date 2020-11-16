/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.error;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.config.internal.error.MuleCoreErrorTypeRepository.MULE_CORE_ERROR_TYPE_REPOSITORY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.runtime.core.api.error.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;

import org.mule.runtime.api.message.ErrorType;

import java.util.Optional;

import org.junit.Test;

public class MuleCoreErrorTypeRepositoryTestCase {

  @Test
  public void lookupsAvailableErrorType() {
    Optional<ErrorType> errorType = MULE_CORE_ERROR_TYPE_REPOSITORY.lookupErrorType(CONNECTIVITY);
    assertThat(errorType.isPresent(), is(true));
    assertThat(errorType.get().getIdentifier(), is(CONNECTIVITY.getName()));
    assertThat(errorType.get().getParentErrorType().getIdentifier(), is(ANY_IDENTIFIER));
  }

  @Test
  public void getsAvailableErrorTypes() {
    Optional<ErrorType> myErrorType = MULE_CORE_ERROR_TYPE_REPOSITORY.getErrorType(CONNECTIVITY);
    assertThat(myErrorType.isPresent(), is(true));
    assertThat(myErrorType.get().getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(myErrorType.get().getParentErrorType().getIdentifier(), is(ANY_IDENTIFIER));
  }

}
