/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.ClassValueModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.runtime.extension.api.annotation.license.RequiresEntitlement;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.ClassValue;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.parser.MediaTypeParser;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecationModel;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.api.loader.java.type.WithOperationContainers;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.extension.api.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.extension.api.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.extension.api.loader.parser.FunctionModelParser;
import org.mule.runtime.extension.api.loader.parser.OperationModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParserDecorator;
import org.mule.runtime.extension.api.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEnterpriseLicenseInfo;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEntitlementInfo;
import org.mule.sdk.api.annotation.semantics.file.FilePath;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Enums;

/**
 * Utility class for {@link ModelLoaderDelegate model loaders}
 *
 * @since 1.0
 */
public final class JavaExtensionModelParserUtils {

  private JavaExtensionModelParserUtils() {}

  public static List<ExtensionParameter> getCompletionCallbackParameters(MethodElement method) {
    return method.getParameters().stream()
        .filter(p -> {
          Type type = p.getType();
          return type.isAssignableTo(CompletionCallback.class) ||
              type.isAssignableTo(org.mule.sdk.api.runtime.process.CompletionCallback.class) ||
              type.isAssignableTo(org.mule.sdk.api.runtime.process.VoidCompletionCallback.class);
        })
        .collect(toList());
  }

  public static boolean isCompletionCallbackParameter(ExtensionParameter extensionParameter) {
    return extensionParameter.getType().isAssignableTo(CompletionCallback.class) ||
        extensionParameter.getType().isAssignableTo(org.mule.sdk.api.runtime.process.CompletionCallback.class);
  }

  public static boolean isAutoPaging(MethodElement operationMethod) {
    Type returnType = operationMethod.getReturnType();
    return returnType.isAssignableTo(PagingProvider.class)
        || returnType.isAssignableTo(org.mule.sdk.api.runtime.streaming.PagingProvider.class);
  }

  public static boolean isProcessorChain(ExtensionParameter parameter) {
    Type type = parameter.getType();
    return type.isAssignableTo(Chain.class)
        || type.isAssignableTo(org.mule.sdk.api.runtime.route.Chain.class);
  }

  public static boolean isParameterGroup(ExtensionParameter groupParameter) {
    return groupParameter.isAnnotatedWith(ParameterGroup.class)
        || groupParameter.isAnnotatedWith(org.mule.sdk.api.annotation.param.ParameterGroup.class);
  }

  public static boolean isParameter(ExtensionParameter parameter) {
    return parameter.isAnnotatedWith(Parameter.class)
        || parameter.isAnnotatedWith(org.mule.sdk.api.annotation.param.Parameter.class);
  }


  public static Stream<OperationModelParser> getOperationParsers(JavaExtensionModelParser extensionModelParser,
                                                                 ExtensionElement extensionElement,
                                                                 WithOperationContainers operationContainers,
                                                                 ExtensionLoadingContext loadingContext) {
    return operationContainers.getOperationContainers().stream()
        .flatMap(container -> container.getOperations().stream()
            .map(method -> new JavaOperationModelParser(extensionModelParser, extensionElement, container, method,
                                                        loadingContext)));
  }

  public static Stream<SourceModelParser> getSourceParsers(ExtensionElement extensionElement,
                                                           List<SourceElement> sources,
                                                           ExtensionLoadingContext loadingContext) {
    return sources.stream()
        .map(source -> new JavaSourceModelParser(extensionElement, source, loadingContext));
  }

  public static List<ConnectionProviderModelParser> getConnectionProviderModelParsers(
                                                                                      ExtensionElement extensionElement,
                                                                                      List<ConnectionProviderElement> connectionProviderElements,
                                                                                      ExtensionLoadingContext loadingContext) {

    return connectionProviderElements.stream()
        .map(cpElement -> new JavaConnectionProviderModelParser(extensionElement, cpElement,
                                                                loadingContext))
        .collect(toList());
  }

  public static List<FunctionModelParser> getFunctionModelParsers(ExtensionElement extensionElement,
                                                                  List<FunctionContainerElement> functionContainers,
                                                                  ExtensionLoadingContext loadingContext) {
    return functionContainers.stream()
        .flatMap(container -> container.getFunctions().stream())
        .map(func -> new JavaFunctionModelParser(extensionElement, func, loadingContext))
        .collect(toList());
  }

  public static List<ParameterGroupModelParser> getParameterGroupParsers(List<? extends ExtensionParameter> parameters,
                                                                         ParameterDeclarationContext context) {
    return getParameterGroupParsers(parameters, context, null);
  }

  public static List<ParameterGroupModelParser> getSourceParameterGroupParsers(List<? extends ExtensionParameter> parameters,
                                                                               ParameterDeclarationContext context) {

    return getParameterGroupParsers(parameters, context, p -> new ParameterModelParserDecorator(p) {

      @Override
      public ExpressionSupport getExpressionSupport() {
        return NOT_SUPPORTED;
      }
    });
  }

  static List<ParameterGroupModelParser> getParameterGroupParsers(List<? extends ExtensionParameter> parameters,
                                                                  ParameterDeclarationContext context,
                                                                  Function<ParameterModelParser, ParameterModelParser> parameterMutator) {
    checkAnnotationsNotUsedMoreThanOnce(parameters,
                                        Connection.class,
                                        org.mule.sdk.api.annotation.param.Connection.class,
                                        Config.class,
                                        org.mule.sdk.api.annotation.param.Config.class,
                                        MetadataKeyId.class,
                                        org.mule.sdk.api.annotation.metadata.MetadataKeyId.class);

    List<ParameterGroupModelParser> groups = new LinkedList<>();
    List<ExtensionParameter> defaultGroupParams = new LinkedList<>();
    boolean defaultGroupAdded = false;

    for (ExtensionParameter extensionParameter : parameters) {
      if (!extensionParameter.shouldBeAdvertised()) {
        continue;
      }

      if (isParameterGroup(extensionParameter)) {
        groups.add(new JavaDeclaredParameterGroupModelParser(extensionParameter, context, parameterMutator));
      } else {
        defaultGroupParams.add(extensionParameter);
        if (!defaultGroupAdded) {
          groups.add(new JavaDefaultParameterGroupParser(defaultGroupParams, context, parameterMutator));
          defaultGroupAdded = true;
        }
      }
    }

    return groups;
  }

  private static void checkAnnotationsNotUsedMoreThanOnce(List<? extends ExtensionParameter> parameters,
                                                          Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      int usages = 0;
      for (ExtensionParameter param : parameters) {
        if (param.isAnnotatedWith(annotation) && ++usages > 1) {
          throw new IllegalModelDefinitionException(format("The defined parameters %s from %s, uses the annotation @%s more than once",
                                                           parameters.stream().map(p -> p.getName()).collect(toList()),
                                                           parameters.iterator().next().getOwnerDescription(),
                                                           annotation.getSimpleName()));
        }
      }
    }
  }

  public static Optional<ExtensionParameter> getConfigParameter(WithParameters element) {
    Optional<ExtensionParameter> configParameter = element.getParametersAnnotatedWith(Config.class).stream().findFirst();
    if (!configParameter.isPresent()) {
      configParameter = element.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.Config.class).stream().findFirst();
    }

    return configParameter;
  }

  public static Optional<ExtensionParameter> getConnectionParameter(WithParameters element) {
    Optional<ExtensionParameter> connectionParameter = element.getParametersAnnotatedWith(Connection.class).stream().findFirst();
    if (!connectionParameter.isPresent()) {
      connectionParameter =
          element.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.Connection.class).stream().findFirst();
    }

    return connectionParameter;
  }

  public static Optional<DeprecationModel> getDeprecationModel(ExtensionParameter extensionParameter) {
    return getDeprecationModel(extensionParameter, "Parameter", extensionParameter.getName());
  }

  public static Optional<DeprecationModel> getDeprecationModel(FunctionElement functionElement) {
    return getDeprecationModel(functionElement, "Function", functionElement.getName());
  }

  public static Optional<DeprecationModel> getDeprecationModel(OperationElement operationElement) {
    return getDeprecationModel(operationElement, "Operation", operationElement.getName());
  }

  public static Optional<DeprecationModel> getDeprecationModel(SourceElement sourceElement) {
    return getDeprecationModel(sourceElement, "Source", sourceElement.getName());
  }

  public static Optional<DeprecationModel> getDeprecationModel(ConnectionProviderElement connectionProviderElement) {
    return getDeprecationModel(connectionProviderElement, "Connection provider", connectionProviderElement.getName());
  }

  public static Optional<DeprecationModel> getDeprecationModel(ComponentElement componentElement) {
    return getDeprecationModel(componentElement, "Component", componentElement.getName());
  }

  public static Optional<DeprecationModel> getDeprecationModel(ExtensionElement extensionElement) {
    return getDeprecationModel(extensionElement, "Extension", extensionElement.getName());
  }

  public static Optional<RequiresEnterpriseLicenseInfo> getRequiresEnterpriseLicenseInfo(ExtensionElement extensionElement) {
    return mapReduceSingleAnnotation(extensionElement, RequiresEnterpriseLicense.class,
                                     org.mule.sdk.api.annotation.license.RequiresEnterpriseLicense.class,
                                     value -> new RequiresEnterpriseLicenseInfo(value
                                         .getBooleanValue(RequiresEnterpriseLicense::allowEvaluationLicense)),
                                     value -> new RequiresEnterpriseLicenseInfo(value
                                         .getBooleanValue(org.mule.sdk.api.annotation.license.RequiresEnterpriseLicense::allowEvaluationLicense)));
  }

  public static Optional<RequiresEntitlementInfo> getRequiresEntitlementInfo(ExtensionElement extensionElement) {
    return mapReduceSingleAnnotation(
                                     extensionElement,
                                     RequiresEntitlement.class,
                                     org.mule.sdk.api.annotation.license.RequiresEntitlement.class,
                                     value -> new RequiresEntitlementInfo(
                                                                          value.getStringValue(RequiresEntitlement::name),
                                                                          value.getStringValue(RequiresEntitlement::description)),
                                     value -> new RequiresEntitlementInfo(
                                                                          value
                                                                              .getStringValue(org.mule.sdk.api.annotation.license.RequiresEntitlement::name),
                                                                          value
                                                                              .getStringValue(org.mule.sdk.api.annotation.license.RequiresEntitlement::description)));
  }

  public static Optional<DisplayModel> getDisplayModel(WithAnnotations element, String elementType, String elementName) {
    Optional<String> summary = mapReduceSingleAnnotation(
                                                         element,
                                                         elementType,
                                                         elementName,
                                                         Summary.class,
                                                         org.mule.sdk.api.annotation.param.display.Summary.class,
                                                         value -> value
                                                             .getStringValue(Summary::value),
                                                         value -> value
                                                             .getStringValue(org.mule.sdk.api.annotation.param.display.Summary::value));

    Optional<String> displayName = mapReduceSingleAnnotation(
                                                             element,
                                                             elementType,
                                                             elementName,
                                                             DisplayName.class,
                                                             org.mule.sdk.api.annotation.param.display.DisplayName.class,
                                                             value -> value.getStringValue(DisplayName::value),
                                                             value -> value
                                                                 .getStringValue(org.mule.sdk.api.annotation.param.display.DisplayName::value));

    Optional<String> example = mapReduceSingleAnnotation(
                                                         element,
                                                         elementType,
                                                         elementName,
                                                         Example.class,
                                                         org.mule.sdk.api.annotation.param.display.Example.class,
                                                         value -> value.getStringValue(Example::value),
                                                         value -> value
                                                             .getStringValue(org.mule.sdk.api.annotation.param.display.Example::value));

    Function<AnnotationValueFetcher<ClassValue>, ClassValueModel> valueFromLegacyAnnotation =
        classValue -> new ClassValueModel(classValue.getArrayValue(ClassValue::extendsOrImplements).stream()
            .filter(p -> !isBlank(p))
            .collect(toList()));

    Function<AnnotationValueFetcher<org.mule.sdk.api.annotation.param.display.ClassValue>, ClassValueModel> valueFromSdkAnnotation =
        classValue -> new ClassValueModel(classValue
            .getArrayValue(org.mule.sdk.api.annotation.param.display.ClassValue::extendsOrImplements)
            .stream()
            .filter(p -> !isBlank(p))
            .collect(toList()));

    Optional<ClassValueModel> classValueModel = mapReduceSingleAnnotation(
                                                                          element,
                                                                          elementType,
                                                                          elementName,
                                                                          ClassValue.class,
                                                                          org.mule.sdk.api.annotation.param.display.ClassValue.class,
                                                                          valueFromLegacyAnnotation,
                                                                          valueFromSdkAnnotation);

    Function<AnnotationValueFetcher<Path>, PathModel> pathModelFromLegacyAnnotation =
        value -> new PathModel(
                               value.getEnumValue(Path::type),
                               value.getBooleanValue(Path::acceptsUrls),
                               value.getEnumValue(Path::location),
                               value.getArrayValue(Path::acceptedFileExtensions).stream().toArray(String[]::new));

    Function<AnnotationValueFetcher<FilePath>, PathModel> pathModelFromSdkAnnotation =
        value -> new PathModel(
                               value.getEnumValue(FilePath::type),
                               value.getBooleanValue(FilePath::acceptsUrls),
                               value.getEnumValue(FilePath::location),
                               value.getArrayValue(FilePath::acceptedFileExtensions).stream().toArray(String[]::new));

    Optional<PathModel> pathModel = mapReduceSingleAnnotation(
                                                              element,
                                                              elementType,
                                                              elementName,
                                                              Path.class,
                                                              FilePath.class,
                                                              pathModelFromLegacyAnnotation,
                                                              pathModelFromSdkAnnotation);

    Optional<DisplayModel> displayModel;
    if (summary.isPresent() || displayName.isPresent() || example.isPresent() || classValueModel.isPresent()
        || pathModel.isPresent()) {

      DisplayModel.DisplayModelBuilder builder = DisplayModel.builder();
      summary.ifPresent(builder::summary);
      displayName.ifPresent(builder::displayName);
      example.ifPresent(builder::example);
      classValueModel.ifPresent(builder::classValue);
      pathModel.ifPresent(builder::path);

      displayModel = of(builder.build());
    } else {
      displayModel = empty();
    }

    return displayModel;
  }

  public static Optional<ExecutionType> getExecutionType(OperationElement operationElement) {
    return mapReduceSingleAnnotation(operationElement,
                                     "Operation",
                                     operationElement.getName(),
                                     Execution.class,
                                     org.mule.sdk.api.annotation.execution.Execution.class,
                                     value -> value.getEnumValue(Execution::value),
                                     value -> mapEnumTo(value.getEnumValue(
                                                                           org.mule.sdk.api.annotation.execution.Execution::value),
                                                        ExecutionType.class));
  }

  private static <T extends Enum<T>> T mapEnumTo(Enum fromEnum, Class<T> toEnumClass) {
    return Enums.getIfPresent(toEnumClass, fromEnum.name()).orNull();
  }

  private static Optional<DeprecationModel> getDeprecationModel(WithAnnotations element, String elementType, String elementName) {
    return mapReduceAnnotation(
                               element,
                               Deprecated.class,
                               org.mule.sdk.api.annotation.deprecated.Deprecated.class,
                               value -> buildDeprecationModel(
                                                              value.getStringValue(Deprecated::message),
                                                              value.getStringValue(Deprecated::since),
                                                              value.getStringValue(Deprecated::toRemoveIn)),
                               value -> buildDeprecationModel(
                                                              value
                                                                  .getStringValue(org.mule.sdk.api.annotation.deprecated.Deprecated::message),
                                                              value
                                                                  .getStringValue(org.mule.sdk.api.annotation.deprecated.Deprecated::since),
                                                              value
                                                                  .getStringValue(org.mule.sdk.api.annotation.deprecated.Deprecated::toRemoveIn)),
                               () -> new IllegalParameterModelDefinitionException(format("%s '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                                         elementType,
                                                                                         elementName,
                                                                                         Deprecated.class.getName(),
                                                                                         org.mule.sdk.api.annotation.deprecated.Deprecated.class
                                                                                             .getName())));
  }

  private static DeprecationModel buildDeprecationModel(String message, String since, String toRemoveIn) {
    if (isBlank(toRemoveIn)) {
      toRemoveIn = null;
    }
    return new ImmutableDeprecationModel(message, since, toRemoveIn);
  }

  public static Optional<MediaTypeParser> getMediaType(WithAnnotations element,
                                                       String elementType,
                                                       String elementName) {
    Optional<String> stringValue = mapReduceSingleAnnotation(
                                                             element,
                                                             elementType,
                                                             elementName,
                                                             MediaType.class,
                                                             org.mule.sdk.api.annotation.param.MediaType.class,
                                                             ann -> ann.getStringValue(MediaType::value),
                                                             ann -> ann
                                                                 .getStringValue(org.mule.sdk.api.annotation.param.MediaType::value));

    Optional<Boolean> booleanValue = mapReduceSingleAnnotation(
                                                               element,
                                                               elementType,
                                                               elementName,
                                                               MediaType.class,
                                                               org.mule.sdk.api.annotation.param.MediaType.class,
                                                               ann -> ann.getBooleanValue(MediaType::strict),
                                                               ann -> ann
                                                                   .getBooleanValue(org.mule.sdk.api.annotation.param.MediaType::strict));


    return stringValue.flatMap(str -> booleanValue.map(bool -> new DefaultMediaTypeParser(str, bool)));
  }

  private static class DefaultMediaTypeParser implements MediaTypeParser {

    private final String mimeType;
    private final boolean strict;

    public DefaultMediaTypeParser(String mimeType, boolean strict) {
      this.mimeType = mimeType;
      this.strict = strict;
    }

    public String getMimeType() {
      return mimeType;
    }

    public boolean isStrict() {
      return strict;
    }
  }
}
