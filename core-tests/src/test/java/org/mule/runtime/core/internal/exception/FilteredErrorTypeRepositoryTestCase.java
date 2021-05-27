/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.ast.AllureConstants.ArtifactAst.Errors.ERROR_TYPES;
import static org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider.getCoreErrorTypeRepo;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.ast.internal.error.DefaultErrorTypeRepository;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ERROR_TYPES)
@SmallTest
public class FilteredErrorTypeRepositoryTestCase extends AbstractMuleTestCase {

  private ErrorTypeRepository errorTypeRepository;

  private final ComponentIdentifier ns1error = ComponentIdentifier.builder().namespace("NS1").name("AN_ERROR").build();
  private final ComponentIdentifier ns2error = ComponentIdentifier.builder().namespace("NS2").name("AN_ERROR").build();
  private final ComponentIdentifier ns1internalError =
      ComponentIdentifier.builder().namespace("NS1").name("AN_INTERNAL_ERROR").build();
  private final ComponentIdentifier ns2internalError =
      ComponentIdentifier.builder().namespace("NS2").name("AN_INTERNAL_ERROR").build();

  @Before
  public void before() {
    errorTypeRepository = new DefaultErrorTypeRepository();
    errorTypeRepository.addErrorType(ns1error, getCoreErrorTypeRepo().getAnyErrorType());
    errorTypeRepository.addErrorType(ns2error, getCoreErrorTypeRepo().getAnyErrorType());
    errorTypeRepository.addInternalErrorType(ns1internalError, getCoreErrorTypeRepo().getAnyErrorType());
    errorTypeRepository.addInternalErrorType(ns2internalError, getCoreErrorTypeRepo().getAnyErrorType());
  }

  @Test
  public void getErrorNamespaces() {
    final FilteredErrorTypeRepository filtered = new FilteredErrorTypeRepository(errorTypeRepository, singleton("NS1"));

    assertThat(filtered.getErrorNamespaces(), containsInAnyOrder("NS1"));
    assertThat(filtered.getErrorNamespaces(), not(contains("NS2")));
  }

  @Test
  public void getErrorTypes() {
    final FilteredErrorTypeRepository filtered = new FilteredErrorTypeRepository(errorTypeRepository, singleton("NS1"));

    final Set<ComponentIdentifier> errorTypeIdentifiers = filtered.getErrorTypes().stream()
        .map(err -> ComponentIdentifier.builder().namespace(err.getNamespace()).name(err.getIdentifier()).build())
        .collect(toSet());

    assertThat(errorTypeIdentifiers, containsInAnyOrder(ns1error));
    assertThat(errorTypeIdentifiers, not(contains(ns2error)));
    assertThat(errorTypeIdentifiers, not(contains(ns1internalError)));
    assertThat(errorTypeIdentifiers, not(contains(ns2internalError)));
  }

  @Test
  public void getInternalErrorTypes() {
    final FilteredErrorTypeRepository filtered = new FilteredErrorTypeRepository(errorTypeRepository, singleton("NS1"));

    final Set<ComponentIdentifier> internalErrorTypeIdentifiers = filtered.getInternalErrorTypes().stream()
        .map(err -> ComponentIdentifier.builder().namespace(err.getNamespace()).name(err.getIdentifier()).build())
        .collect(toSet());

    assertThat(internalErrorTypeIdentifiers, containsInAnyOrder(ns1internalError));
    assertThat(internalErrorTypeIdentifiers, not(contains(ns2internalError)));
    assertThat(internalErrorTypeIdentifiers, not(contains(ns1error)));
    assertThat(internalErrorTypeIdentifiers, not(contains(ns2error)));
  }
}
