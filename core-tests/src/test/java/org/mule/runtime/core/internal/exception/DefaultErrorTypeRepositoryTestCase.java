/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ROUTING;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_TYPES;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collection;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(ERROR_HANDLING)
@Story(ERROR_TYPES)
public class DefaultErrorTypeRepositoryTestCase extends AbstractMuleTestCase {

  private ErrorTypeRepository errorTypeRepository = new DefaultErrorTypeRepository();
  private ComponentIdentifier INTERNAL_ERROR =
      ComponentIdentifier.builder().namespace("NS").name("NAME").build();
  private ComponentIdentifier EXPOSED_ERROR =
      ComponentIdentifier.builder().namespace("NS2").name("OTHER_NAME").build();
  private ComponentIdentifier OTHER_INTERNAL_ERROR =
      ComponentIdentifier.builder().namespace("NS2").name("NAME").build();


  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    errorTypeRepository.addErrorType(CONNECTIVITY, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(ROUTING, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType(EXPOSED_ERROR, errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addInternalErrorType(INTERNAL_ERROR, errorTypeRepository.getCriticalErrorType());
    errorTypeRepository.addInternalErrorType(OTHER_INTERNAL_ERROR, errorTypeRepository.getCriticalErrorType());
  }

  @Test
  public void lookupsAvailableErrorType() {
    Optional<ErrorType> errorType = errorTypeRepository.lookupErrorType(CONNECTIVITY);
    assertThat(errorType.isPresent(), is(true));
    assertThat(errorType.get().getIdentifier(), is(CONNECTIVITY.getName()));
    assertThat(errorType.get().getParentErrorType().getIdentifier(), is(ANY_IDENTIFIER));
  }

  @Test
  public void doesNotLookupUnavailableErrorType() {
    assertThat(errorTypeRepository.lookupErrorType(INTERNAL_ERROR).isPresent(), is(false));
  }

  @Test
  public void getsAvailableErrorTypes() {
    Optional<ErrorType> myErrorType = errorTypeRepository.getErrorType(CONNECTIVITY);
    assertThat(myErrorType.isPresent(), is(true));
    assertThat(myErrorType.get().getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(myErrorType.get().getParentErrorType().getIdentifier(), is(ANY_IDENTIFIER));
  }

  @Test
  public void getsUnavailableErrorTypes() {
    Optional<ErrorType> myErrorType = errorTypeRepository.getErrorType(INTERNAL_ERROR);
    assertThat(myErrorType.isPresent(), is(true));
    assertThat(myErrorType.get().getIdentifier(), is("NAME"));
    assertThat(myErrorType.get().getParentErrorType().getIdentifier(), is(CRITICAL_IDENTIFIER));
  }

  @Test
  public void doesNotAddInternalTypeAsRegularOne() {
    expectedException.expectMessage(is("An error type with identifier 'NS:NAME' already exists"));
    errorTypeRepository.addErrorType(INTERNAL_ERROR, errorTypeRepository.getAnyErrorType());
  }

  @Test
  public void doesNotAddRegularTypeAsInternalOne() {
    expectedException.expectMessage(is("An error type with identifier 'MULE:CONNECTIVITY' already exists"));
    errorTypeRepository.addInternalErrorType(CONNECTIVITY, errorTypeRepository.getCriticalErrorType());
  }

  @Test
  public void returnsUnrepeatedExposedAndInternalNamespaces() {
    Collection<String> errorNamespaces = errorTypeRepository.getErrorNamespaces();
    assertThat(errorNamespaces, hasSize(3));
    assertThat(errorNamespaces, containsInAnyOrder("MULE", "NS", "NS2"));
  }

}
