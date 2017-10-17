/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getNamedObject;
import static org.mule.runtime.module.extension.internal.loader.enricher.LevelErrorTypes.EXTENSION;
import static org.mule.runtime.module.extension.internal.loader.enricher.LevelErrorTypes.OPERATION;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.heisenberg.extension.HeisenbergErrors;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.heisenberg.extension.HeisenbergRouters;
import org.mule.test.heisenberg.extension.HeisenbergScopes;
import org.mule.test.heisenberg.extension.KillingOperations;
import org.mule.test.heisenberg.extension.MoneyLaunderingOperation;

import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ErrorsDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final String HEISENBERG = "HEISENBERG";
  private static final String MULE_NAMESPACE = "MULE";
  private static final String TYPE = "type";

  @Rule
  public ExpectedException expectedException = none();

  private ExtensionModel extensionModel;

  @Test
  public void detectErrorTypesCycleDependency() {
    assertThatThrownBy(() -> extensionModel = loadExtension(HeisenbergWithCyclicErrorTypes.class))
        .hasMessageContaining("Cyclic Error Types reference detected")
        .isInstanceOf(IllegalModelDefinitionException.class);
  }

  @Test
  public void operationsWithConnectionsThrowsConnectivityError() {
    extensionModel = loadExtension(HeisenbergExtension.class);
    OperationModel callSaul =
        getNamedObject(extensionModel.getConfigurationModel("config").get().getOperationModels(), "callSaul");
    Set<ErrorModel> errorTypesIdentifiers = callSaul.getErrorModels();
    assertThat(errorTypesIdentifiers, hasItem(hasProperty(TYPE, is(CONNECTIVITY_ERROR_IDENTIFIER))));
  }

  @Test
  public void extensionErrorsInheritFromMuleErrors() {
    extensionModel = loadExtension(HeisenbergExtension.class);
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
    assertThatThrownBy(() -> extensionModel = loadExtension(HeisenbergWithNotMappedErrorType.class))
        .hasMessageContaining("Invalid operation throws detected, the extension declared to throw errors")
        .isInstanceOf(IllegalModelDefinitionException.class);
  }

  @Test
  public void orphanErrorsUsesAnyAsParent() {
    extensionModel = loadExtension(HeisenbergWithOrphanErrors.class);
    ErrorModel errorModel =
        extensionModel.getErrorModels().stream().filter(error -> error.getType().equals("HEALTH")).findFirst().get();
    assertThat(errorModel.getNamespace(), is(HEISENBERG));

    Optional<ErrorModel> anyExtensionError = errorModel.getParent();
    assertThat(anyExtensionError.isPresent(), is(true));
    assertThat(anyExtensionError.get().getType(), is(ANY.getType()));
    assertThat(anyExtensionError.get().getNamespace(), is(MULE_NAMESPACE));
  }

  @Test
  public void operationThrowsOverridesExtensionThrows() {
    extensionModel = loadExtension(HeisenbergWithOperationThrows.class);
    OperationModel someOperation = extensionModel.getOperationModel("someOperation").get();
    Optional<ErrorModel> operationError = someOperation.getErrorModels().stream()
        .filter(errorModel -> errorModel.getType().equals(OPERATION.getType())).findFirst();
    assertThat(operationError.isPresent(), is(true));
  }

  @Test
  public void operationInheritsExtensionErrorThrows() {
    extensionModel = loadExtension(HeisenbergWithExtensionThrows.class);

    OperationModel someOperation = extensionModel.getOperationModel("someOperation").get();
    Optional<ErrorModel> operationError = someOperation.getErrorModels().stream()
        .filter(errorModel -> errorModel.getType().equals(EXTENSION.getType())).findFirst();
    assertThat(operationError.isPresent(), is(true));
  }

  @Test
  public void operationsWithErrorTypesButExtensionWithNone() throws Exception {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage("There are 2 operations annotated with @Throws, but class "
        + "org.mule.runtime.module.extension.internal.loader.enricher.ErrorsDeclarationEnricherTestCase$HeisenbergWithoutErrorTypes "
        + "does not specify any error type through the @ErrorTypes annotation");

    loadExtension(HeisenbergWithoutErrorTypes.class);
  }

  @ErrorTypes(CyclicErrorTypes.class)
  @Extension(name = "Heisenberg")
  public static class HeisenbergWithCyclicErrorTypes extends HeisenbergExtension {

  }


  @Extension(name = "Heisenberg")
  @Operations(InvalidErrorOperations.class)
  @ErrorTypes(HeisenbergErrors.class)
  public static class HeisenbergWithNotMappedErrorType extends HeisenbergExtension {

  }


  @Extension(name = "Heisenberg")
  @ErrorTypes(OrphanErrorTypes.class)
  public static class HeisenbergWithOrphanErrors extends HeisenbergExtension {

  }


  @Extension(name = "Heisenberg")
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class,
      KillingOperations.class, HeisenbergScopes.class, HeisenbergRouters.class})
  public static class HeisenbergWithoutErrorTypes extends HeisenbergExtension {

  }


  @Extension(name = "Heisenberg")
  @ErrorTypes(LevelErrorTypes.class)
  @Throws(ExtensionLevelErrorTypeProvider.class)
  @Operations(OperationWithThrows.class)
  public static class HeisenbergWithOperationThrows extends HeisenbergExtension {

  }


  @Extension(name = "Heisenberg")
  @ErrorTypes(LevelErrorTypes.class)
  @Throws(ExtensionLevelErrorTypeProvider.class)
  @Operations(OperationWithOutThrows.class)
  public static class HeisenbergWithExtensionThrows extends HeisenbergExtension {

  }


  private static class InvalidErrorOperations {

    @Throws(ErrorTypeProviderWithInvalidErrors.class)
    public void someOperation() {

    }

    public static class ErrorTypeProviderWithInvalidErrors implements ErrorTypeProvider {

      public enum WrongErrors implements ErrorTypeDefinition<org.mule.runtime.extension.api.error.MuleErrors> {
        WHATEVER {

          @Override
          public Optional<ErrorTypeDefinition<?>> getParent() {
            return empty();
          }
        };

        @Override
        public Optional<ErrorTypeDefinition<?>> getParent() {
          return Optional.of(ANY);
        }
      }


      @Override
      public Set<ErrorTypeDefinition> getErrorTypes() {
        return singleton(WrongErrors.WHATEVER);
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


  private static class OperationWithThrows {

    @Throws(OperationLevelErrorTypeProvider.class)
    public void someOperation() {

    }
  }


  private static class OperationWithOutThrows {

    public void someOperation() {

    }
  }


  public static class OperationLevelErrorTypeProvider implements ErrorTypeProvider {

    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
      return singleton(OPERATION);
    }
  }


  public static class ExtensionLevelErrorTypeProvider implements ErrorTypeProvider {

    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
      return singleton(EXTENSION);
    }
  }


  public enum OrphanErrorTypes implements ErrorTypeDefinition<OrphanErrorTypes> {
    HEALTH, CONNECTIVITY, OAUTH2
  }
}
