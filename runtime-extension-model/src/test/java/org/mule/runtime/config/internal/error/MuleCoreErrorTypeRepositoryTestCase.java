/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
  public void lookupAvailableErrorType() {
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
