/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.getNamedObject;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.EXTENSION_DESCRIPTION;

import org.junit.Test;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergErrors;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class ExtensionErrorsModelEnricherTestCase extends AbstractMuleTestCase {

  private static final String HEISENBERG = "HEISENBERG";
  private static final String MULE_NAMESPACE = "MULE";
  private static final String TYPE = "type";
  private ExtensionModel extensionModel;

  @Test
  public void detectErrorTypesCycleDependency() {
    assertThatThrownBy(() -> describe(HeisenbergWithCyclicErrorTypes.class))
        .hasMessageContaining("Cyclic Error Types reference detected")
        .isInstanceOf(IllegalModelDefinitionException.class);
  }

  @Test
  public void operationsWithConnectionsThrowsConnectivityError() {
    describe(HeisenbergExtension.class);
    OperationModel callSaul =
        getNamedObject(extensionModel.getConfigurationModel("config").get().getOperationModels(), "callSaul");
    Set<ErrorModel> errorTypesIdentifiers = callSaul.getErrorModels();
    assertThat(errorTypesIdentifiers, hasItem(hasProperty(TYPE, is(CONNECTIVITY_ERROR_IDENTIFIER))));
  }

  @Test
  public void extensionErrorsInheritFromMuleErrors() {
    describe(HeisenbergExtension.class);
    OperationModel cureCancer = getNamedObject(extensionModel.getOperationModels(), "cureCancer");
    assertThat(cureCancer.getErrorModels(), hasItem(hasProperty(TYPE, is(HEALTH.getType()))));

    Optional<ErrorModel> healthError =
        extensionModel.getErrorModels().stream()
            .filter(errorModel -> errorModel.getType().equals(HEALTH.getType())).findFirst();
    assertThat(healthError.isPresent(), is(true));
    Optional<ErrorModel> optConnectivityError = healthError.get().getParent();
    assertThat(optConnectivityError.isPresent(), is(true));
    ErrorModel connectivityError = optConnectivityError.get();
    assertThat(connectivityError.getType(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(connectivityError.getNamespace(), is(HEISENBERG));

    Optional<ErrorModel> optMuleConnectivityError = connectivityError.getParent();
    assertThat(optMuleConnectivityError.isPresent(), is(true));
    ErrorModel muleConnectivityError = optMuleConnectivityError.get();
    assertThat(muleConnectivityError.getType(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(muleConnectivityError.getNamespace(), is(MULE_NAMESPACE));
  }

  @Test
  public void operationUsesANotMappedErrorType() {
    assertThatThrownBy(() -> describe(HeisenbergWithNotMappedErrorType.class))
        .hasMessageContaining("Invalid operation throws detected, the extension declared to throw errors")
        .isInstanceOf(IllegalModelDefinitionException.class);
  }

  private void describe(Class aClass) {
    final DefaultDescribingContext describingContext = new DefaultDescribingContext(getClass().getClassLoader());
    ExtensionDeclarer declarer =
        new AnnotationsBasedDescriber(aClass, new StaticVersionResolver("4.0")).describe(
                                                                                         describingContext);
    extensionModel =
        new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader()).createFrom(declarer,
                                                                                                      describingContext);
  }

  @ErrorTypes(CyclicErrorTypes.class)
  @Extension(name = "Heisenberg", description = EXTENSION_DESCRIPTION)
  public static class HeisenbergWithCyclicErrorTypes extends HeisenbergExtension {

  }

  @Extension(name = "Heisenberg", description = EXTENSION_DESCRIPTION)
  @Operations(InvalidErrorOperations.class)
  @ErrorTypes(HeisenbergErrors.class)
  public static class HeisenbergWithNotMappedErrorType extends HeisenbergExtension {

  }

  private static class InvalidErrorOperations {

    @Throws(ErrorTypeProviderWithInvalidErrors.class)
    public void someOperation() {

    }

    public static class ErrorTypeProviderWithInvalidErrors implements ErrorTypeProvider {

      @Override
      public Set<ErrorTypeDefinition> getErrorTypes() {
        return Collections.singleton(MuleErrors.CONNECTIVITY);
      }
    }
  }

  public enum CyclicErrorTypes implements ErrorTypeDefinition<CyclicErrorTypes> {
    TYPE_A {

      @Override
      public Optional<ErrorTypeDefinition<?>> getParent() {
        return Optional.of(TYPE_B);
      }
    },
    TYPE_B {

      @Override
      public Optional<ErrorTypeDefinition<?>> getParent() {
        return Optional.of(TYPE_C);
      }
    },
    TYPE_C {

      @Override
      public Optional<ErrorTypeDefinition<?>> getParent() {
        return Optional.of(TYPE_A);
      }
    }
  }
}
