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
public class AggregatedErrorsCharacteristic extends Characteristic<List<ErrorModelParser>> {

  private static final String ERROR_TYPE_PARAM = "type";

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
        handleRaiseError(operationAst, models, hierarchy);
      } else {
        handleOperationOtherThanRaiseError(operationAst, models, hierarchy);
      }

      return models;
    }

    private void handleOperationOtherThanRaiseError(ComponentAst operationAst, List<ErrorModelParser> models,
                                                    List<ComponentAst> hierarchy) {
      Optional<OperationModel> operationModel = operationAst.getModel(OperationModel.class);
      operationModel.ifPresent(model -> model.getErrorModels().stream().map(MuleSdkErrorModelParser::new)
          .forEach(errorModelParser -> addIfNotSuppressed(errorModelParser, models, hierarchy)));
    }

    private void handleRaiseError(ComponentAst raiseErrorAst, List<ErrorModelParser> errorModels, List<ComponentAst> hierarchy) {
      final ComponentParameterAst typeParameter = raiseErrorAst.getParameter(DEFAULT_GROUP_NAME, ERROR_TYPE_PARAM);
      if (null == typeParameter) {
        return;
      }

      Optional<String> errorId = typeParameter.getValue().<String>getValue();
      if (!errorId.isPresent()) {
        return;
      }

      // TODO: Use the extension parser's namespace.
      addIfNotSuppressed(new MuleSdkErrorModelParser(APP_LOCAL_EXTENSION_NAMESPACE, errorId.get(), null), errorModels, hierarchy);
    }

    private boolean isRaiseError(ComponentAst operationAst) {
      return operationAst.getIdentifier().equals(RAISE_ERROR_IDENTIFIER);
    }

    private void addIfNotSuppressed(ErrorModelParser errorModelParser, List<ErrorModelParser> models,
                                    List<ComponentAst> hierarchy) {
      List<ErrorModelParserMatcher> suppressedErrors = getSuppressedErrors(hierarchy);
      if (!isErrorSuppressed(errorModelParser, suppressedErrors)) {
        models.add(errorModelParser);
      }
    }

    private List<ErrorModelParserMatcher> getSuppressedErrors(List<ComponentAst> hierarchy) {
      if (hierarchy.isEmpty()) {
        return emptyList();
      }

      int containerIndex = hierarchy.size() - 1;
      List<ErrorModelParserMatcher> suppressedErrors = new ArrayList<>();
      while (containerIndex >= 0) {
        ComponentAst container = hierarchy.get(containerIndex);
        if (container.getIdentifier().equals(TRY_IDENTIFIER)) {
          addSuppressedErrors(container, suppressedErrors);
        } else if (container.getIdentifier().equals(ERROR_HANDLER_IDENTIFIER)) {
          --containerIndex;
        }
        --containerIndex;
      }
      return suppressedErrors;
    }

    private void addSuppressedErrors(ComponentAst tryScopeAst, List<ErrorModelParserMatcher> suppressedErrors) {
      Optional<ComponentAst> errorHandler = tryScopeAst.directChildrenStreamByIdentifier(null, ERROR_HANDLER).findFirst();
      if (!errorHandler.isPresent()) {
        return;
      }

      Optional<ComponentAst> onErrorContinue =
          errorHandler.get().directChildrenStreamByIdentifier(null, ON_ERROR_CONTINUE).findFirst();
      if (!onErrorContinue.isPresent()) {
        return;
      }

      String typeAsString =
          (String) onErrorContinue.get().getParameter(DEFAULT_GROUP_NAME, ERROR_TYPE_PARAM).getValue().getValue().orElse(null);
      suppressedErrors.add(createErrorModelParserMatcher(typeAsString));
    }

    private boolean isErrorSuppressed(ErrorModelParser errorModelParser, List<ErrorModelParserMatcher> suppressedErrors) {
      return suppressedErrors.stream().anyMatch(matcher -> matcher.matches(errorModelParser));
    }
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

  private static class WildcardErrorModelParserMatcher implements ErrorModelParserMatcher {

    public static final String WILDCARD_TOKEN = "*";

    private final ComponentIdentifier errorIdentifier;

    private final boolean nameIsWildcard;

    private final boolean namespaceIsWildcard;

    public WildcardErrorModelParserMatcher(ComponentIdentifier errorIdentifier) {
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

  private static class SingleErrorModelParserMatcher implements ErrorModelParserMatcher {

    private final ComponentIdentifier errorIdentifier;

    public SingleErrorModelParserMatcher(ComponentIdentifier errorIdentifier) {
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

  private static class DisjunctiveErrorModelParserMatcher implements ErrorModelParserMatcher {

    private final List<ErrorModelParserMatcher> matchers;

    public DisjunctiveErrorModelParserMatcher(List<ErrorModelParserMatcher> matchers) {
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
