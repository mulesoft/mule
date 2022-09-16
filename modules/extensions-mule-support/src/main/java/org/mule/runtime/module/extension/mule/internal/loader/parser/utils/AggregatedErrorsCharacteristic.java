/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ON_ERROR_CONTINUE;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.RAISE_ERROR;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.TRY_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkExtensionModelParser.APP_LOCAL_EXTENSION_NAMESPACE;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkErrorModelParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * {@link Characteristic} that retrieves all the {@link ErrorModelParser} emitted by the inner components of this Model
 */
// TODO: Contemplate the cases identified in W-11640594
public class AggregatedErrorsCharacteristic extends Characteristic<List<ErrorModelParser>> {

  private static final String ERROR_TYPE_PARAM = "type";
  private static final String WHEN_PARAM = "when";

  private static final ComponentIdentifier RAISE_ERROR_IDENTIFIER =
      builder().namespace(OPERATION_DSL_NAMESPACE).name(RAISE_ERROR).build();

  public AggregatedErrorsCharacteristic() {
    super(new Aggregator(), emptyList(), null);
  }

  private static class Aggregator
      implements BiFunction<ComponentAstWithHierarchy, List<ErrorModelParser>, List<ErrorModelParser>> {

    @Override
    public List<ErrorModelParser> apply(ComponentAstWithHierarchy componentAstWithHierarchy, List<ErrorModelParser> errorModels) {
      List<ErrorModelParser> models = errorModels;
      if (models == null) {
        models = new ArrayList<>(5);
      }

      ComponentAst operationAst = componentAstWithHierarchy.getComponentAst();
      List<ComponentAst> hierarchy = componentAstWithHierarchy.getHierarchy();
      if (isRaiseError(operationAst)) {
        // The raise error component indicates what error it raises
        handleRaiseError(operationAst, models, hierarchy);
      } else {
        // The other operations indicate what errors may emit in the OperationModel
        handleOperationOtherThanRaiseError(operationAst, models, hierarchy);
      }

      return models;
    }

    private void handleOperationOtherThanRaiseError(ComponentAst operationAst, List<ErrorModelParser> models,
                                                    List<ComponentAst> hierarchy) {
      Optional<OperationModel> operationModel = operationAst.getModel(OperationModel.class);
      operationModel.ifPresent(model -> model.getErrorModels().stream().map(MuleSdkErrorModelParser::new)
          .forEach(errorModelParser -> addParserAndMarkIfSuppressed(errorModelParser, models, hierarchy)));
    }

    private void handleRaiseError(ComponentAst raiseErrorAst, List<ErrorModelParser> errorModels, List<ComponentAst> hierarchy) {
      final ComponentParameterAst typeParameter = raiseErrorAst.getParameter(DEFAULT_GROUP_NAME, ERROR_TYPE_PARAM);
      if (null == typeParameter) {
        return;
      }

      Optional<String> errorId = typeParameter.getValue().getValue();
      if (!errorId.isPresent()) {
        return;
      }

      // TODO: Use the extension parser's namespace.
      addParserAndMarkIfSuppressed(new MuleSdkErrorModelParser(APP_LOCAL_EXTENSION_NAMESPACE, errorId.get(), null), errorModels, hierarchy);
    }

    private static boolean isRaiseError(ComponentAst operationAst) {
      return operationAst.getIdentifier().equals(RAISE_ERROR_IDENTIFIER);
    }

    private void addParserAndMarkIfSuppressed(MuleSdkErrorModelParser errorModelParser, List<ErrorModelParser> models,
                                              List<ComponentAst> hierarchy) {
      List<ErrorModelParserMatcher> suppressedErrors = getSuppressedErrors(hierarchy);
      if (isErrorSuppressed(errorModelParser, suppressedErrors)) {
        errorModelParser.setSuppressed();
      }
      models.add(errorModelParser);
    }

    /**
     * Iterates the AST hierarchy starting from the component being handled until the operation:def body, by collecting the errors
     * that would be caught by an on-error-continue if raised by that component. Those errors won't be raised by the operation
     * being defined.
     *
     * @param hierarchy the list of {@link ComponentAst} in the hierarchy, since the operation:def to the component being handled.
     * @return the errors that, if raised by the handled component, would be caught by an on-error-continue before reaching the
     *         operation body.
     */
    private List<ErrorModelParserMatcher> getSuppressedErrors(List<ComponentAst> hierarchy) {
      if (hierarchy.isEmpty()) {
        return emptyList();
      }

      List<ErrorModelParserMatcher> suppressedErrors = new ArrayList<>();

      // Start by the direct parent, which is the last element in the hierarchy list, and iterate the hierarchy upwards
      int containerIndex = hierarchy.size() - 1;
      while (containerIndex >= 0) {
        ComponentAst container = hierarchy.get(containerIndex);
        if (container.getIdentifier().equals(TRY_IDENTIFIER)) {
          // If we find a try scope, then check what errors are caught by an associated on-error-continue
          addSuppressedErrors(container, suppressedErrors);
        } else if (container.getIdentifier().equals(ERROR_HANDLER_IDENTIFIER)) {
          // If we were within an error-handler, skip the parent try scope, since it's like throwing an exception in a
          // catch clause: it shouldn't be caught by the same catch clause
          --containerIndex;
        }
        // Move to immediate parent
        --containerIndex;
      }
      return suppressedErrors;
    }

    /**
     * Given a try scope AST, add all the errors that would be caught by its on-error-continue handlers.
     *
     * @param tryScopeAst      the try scope's {@link ComponentAst}
     * @param suppressedErrors the list where the caught errors must be added
     */
    private void addSuppressedErrors(ComponentAst tryScopeAst, List<ErrorModelParserMatcher> suppressedErrors) {
      Optional<ComponentAst> errorHandler = tryScopeAst.directChildrenStreamByIdentifier(CORE_PREFIX, ERROR_HANDLER).findFirst();
      if (!errorHandler.isPresent()) {
        return;
      }

      errorHandler.get().directChildrenStreamByIdentifier(CORE_PREFIX, ON_ERROR_CONTINUE)
          // If it has a "when" parameter specified, it would raise the errors when the expression evaluates to false.
          .filter(onErrorContinue -> !onErrorContinue.getParameter(DEFAULT_GROUP_NAME, WHEN_PARAM).getValue().getValue()
              .isPresent())
          // An error-handler may have multiple on-error-continues.
          .forEach(onErrorContinue -> {
            String typeAsString =
                (String) onErrorContinue.getParameter(DEFAULT_GROUP_NAME, ERROR_TYPE_PARAM).getValue().getValue().orElse(null);
            suppressedErrors.add(createErrorModelParserMatcher(typeAsString));
          });
    }

    private boolean isErrorSuppressed(ErrorModelParser errorModelParser, List<ErrorModelParserMatcher> suppressedErrors) {
      return suppressedErrors.stream().anyMatch(matcher -> matcher.matches(errorModelParser));
    }

    private static ErrorModelParserMatcher createErrorModelParserMatcher(String typesAsString) {
      if (typesAsString == null) {
        return ErrorModelParserMatcher.ANY_ERROR_PARSER_MATCHER;
      }
      String[] errorTypeIdentifiers = typesAsString.split(",");
      List<ErrorModelParserMatcher> matchers = stream(errorTypeIdentifiers).map((identifier) -> {
        String parsedIdentifier = identifier.trim();
        final ComponentIdentifier errorTypeComponentIdentifier = buildFromStringRepresentation(parsedIdentifier);

        if (doesErrorTypeContainWildcards(errorTypeComponentIdentifier)) {
          return new WildcardErrorModelParserMatcher(errorTypeComponentIdentifier);
        } else {
          return new SingleErrorModelParserMatcher(errorTypeComponentIdentifier);
        }
      }).collect(toList());
      return new DisjunctiveErrorModelParserMatcher(matchers);
    }

    private static boolean doesErrorTypeContainWildcards(ComponentIdentifier errorTypeIdentifier) {
      if (errorTypeIdentifier == null) {
        return false;
      }

      if (Objects.equals(WildcardErrorModelParserMatcher.WILDCARD_TOKEN, errorTypeIdentifier.getName())) {
        return true;
      }

      if (Objects.equals(WildcardErrorModelParserMatcher.WILDCARD_TOKEN, errorTypeIdentifier.getNamespace())) {
        return true;
      }

      return false;
    }
  }

  private interface ErrorModelParserMatcher {

    ErrorModelParserMatcher ANY_ERROR_PARSER_MATCHER = new AggregatedErrorsCharacteristic.AnyErrorModelParserMatcher();

    boolean matches(ErrorModelParser errorModelParser);
  }

  private static class AnyErrorModelParserMatcher implements ErrorModelParserMatcher {

    @Override
    public boolean matches(ErrorModelParser errorModelParser) {
      return true;
    }
  }

  private static final class WildcardErrorModelParserMatcher implements ErrorModelParserMatcher {

    public static final String WILDCARD_TOKEN = "*";

    private final ComponentIdentifier errorIdentifier;

    private final boolean nameIsWildcard;

    private final boolean namespaceIsWildcard;

    private WildcardErrorModelParserMatcher(ComponentIdentifier errorIdentifier) {
      this.errorIdentifier = errorIdentifier;
      this.nameIsWildcard = WILDCARD_TOKEN.equals(errorIdentifier.getName());
      this.namespaceIsWildcard = WILDCARD_TOKEN.equals(errorIdentifier.getNamespace());
    }

    @Override
    public boolean matches(ErrorModelParser errorModelParser) {
      if (matchIdentifier(errorModelParser) && matchNamespace(errorModelParser)) {
        return true;
      }

      // If the error to match is "*:ID", then also match children in any namespace.
      if (!nameIsWildcard && isChild(errorModelParser)) {
        return true;
      }

      // If the error to match is "NS:*" and the namespace didn't match, then don't match children.
      return false;
    }

    private boolean matchNamespace(ErrorModelParser errorModelParser) {
      if (namespaceIsWildcard) {
        return true;
      }

      return Objects.equals(this.errorIdentifier.getNamespace(), errorModelParser.getNamespace());
    }

    private boolean matchIdentifier(ErrorModelParser errorModelParser) {
      if (nameIsWildcard) {
        return true;
      }

      return Objects.equals(this.errorIdentifier.getName(), errorModelParser.getType());
    }

    private boolean isChild(ErrorModelParser errorModelParser) {
      Optional<ErrorModelParser> parentErrorType = errorModelParser.getParent();
      return parentErrorType.isPresent() && this.matches(parentErrorType.get());
    }
  }

  private static final class SingleErrorModelParserMatcher implements ErrorModelParserMatcher {

    private final ComponentIdentifier errorIdentifier;

    private SingleErrorModelParserMatcher(ComponentIdentifier errorIdentifier) {
      this.errorIdentifier = errorIdentifier;
    }

    @Override
    public boolean matches(ErrorModelParser errorModelParser) {
      return matchIdentifier(errorModelParser) && matchNamespace(errorModelParser) || isChild(errorModelParser);
    }

    private boolean matchNamespace(ErrorModelParser errorModelParser) {
      return Objects.equals(this.errorIdentifier.getNamespace(), errorModelParser.getNamespace());
    }

    private boolean matchIdentifier(ErrorModelParser errorModelParser) {
      return Objects.equals(this.errorIdentifier.getName(), errorModelParser.getType());
    }

    private boolean isChild(ErrorModelParser errorModelParser) {
      Optional<ErrorModelParser> parentErrorType = errorModelParser.getParent();
      return parentErrorType.isPresent() && this.matches(parentErrorType.get());
    }
  }

  private static final class DisjunctiveErrorModelParserMatcher implements ErrorModelParserMatcher {

    private final List<ErrorModelParserMatcher> matchers;

    private DisjunctiveErrorModelParserMatcher(List<ErrorModelParserMatcher> matchers) {
      this.matchers = new CopyOnWriteArrayList<>(matchers);
    }

    @Override
    public boolean matches(ErrorModelParser errorModelParser) {
      for (ErrorModelParserMatcher matcher : matchers) {
        if (matcher.matches(errorModelParser)) {
          return true;
        }
      }
      return false;
    }
  }
}
