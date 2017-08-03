/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.display.LayoutModel.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.config.spring.api.XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;
import static org.mule.runtime.extension.internal.loader.catalog.loader.common.XmlMatcher.match;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.catalog.api.TypeResolver;
import org.mule.metadata.catalog.api.TypeResolverException;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.config.spring.api.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.api.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.api.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.spring.internal.dsl.model.ComponentModelReader;
import org.mule.runtime.config.spring.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.spring.internal.dsl.model.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.OperationComponentModelModelProperty;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.XmlExtensionModelProperty;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.catalog.loader.common.XmlMatcher;
import org.mule.runtime.extension.internal.loader.catalog.loader.xml.TypesCatalogXmlLoader;
import org.mule.runtime.extension.internal.loader.catalog.model.TypesCatalog;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Describes an {@link ExtensionModel} by scanning an XML provided in the constructor
 *
 * @since 4.0
 */
final class XmlExtensionLoaderDelegate {

  private static final String PARAMETER_NAME = "name";
  private static final String PARAMETER_DEFAULT_VALUE = "defaultValue";
  private static final String TYPE_ATTRIBUTE = "type";
  private static final String MODULE_NAME = "name";
  private static final String MODULE_PREFIX_ATTRIBUTE = "prefix";
  private static final String MODULE_NAMESPACE_ATTRIBUTE = "namespace";
  private static final String MODULE_NAMESPACE_NAME = "module";
  protected static final String CONFIG_NAME = "config";

  private static final Map<String, ParameterRole> parameterRoleTypes = ImmutableMap.<String, ParameterRole>builder()
      .put("BEHAVIOUR", ParameterRole.BEHAVIOUR)
      .put("CONTENT", ParameterRole.CONTENT)
      .put("PRIMARY", ParameterRole.PRIMARY_CONTENT)
      .build();

  private static final String CATEGORY = "category";
  private static final String VENDOR = "vendor";
  private static final String MIN_MULE_VERSION = "minMuleVersion";
  private static final String DOC_DESCRIPTION = "doc:description";
  private static final String PASSWORD = "password";
  private static final String ROLE = "role";
  private static final String ATTRIBUTE_USE = "use";

  private static final Pattern VALID_XML_NAME = Pattern.compile("[A-Za-z]+[a-zA-Z0-9\\-_]*");

  /**
   * ENUM used to discriminate which type of {@link ParameterDeclarer} has to be created (required or not).
   *
   * @see #getParameterDeclarer(ParameterizedDeclarer, Map)
   */
  private enum UseEnum {
    REQUIRED, OPTIONAL, AUTO
  }

  private static ParameterRole getRole(final String role) {
    if (!parameterRoleTypes.containsKey(role)) {
      throw new IllegalParameterModelDefinitionException(format("The parametrized role [%s] doesn't match any of the expected types [%s]",
                                                                role, join(", ", parameterRoleTypes.keySet())));
    }
    return parameterRoleTypes.get(role);
  }

  private static final ComponentIdentifier OPERATION_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("operation").build();
  private static final ComponentIdentifier OPERATION_PROPERTY_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("property").build();
  private static final ComponentIdentifier OPERATION_PARAMETERS_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("parameters").build();
  private static final ComponentIdentifier OPERATION_PARAMETER_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("parameter").build();
  private static final ComponentIdentifier OPERATION_BODY_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("body").build();
  private static final ComponentIdentifier OPERATION_OUTPUT_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("output").build();
  private static final ComponentIdentifier OPERATION_OUTPUT_ATTRIBUTES_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("output-attributes").build();
  private static final ComponentIdentifier MODULE_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name(MODULE_NAMESPACE_NAME)
          .build();
  public static final String XSD_SUFFIX = ".xsd";
  private static final String XML_SUFFIX = ".xml";
  private static final String TYPES_XML_SUFFIX = "-catalog" + XML_SUFFIX;

  private final String modulePath;
  // TODO MULE-13214: typesCatalog could be removed once MULE-13214 is done
  private Optional<TypesCatalog> typesCatalog;
  private TypeResolver typeResolver;

  /**
   * @param modulePath relative path to a file that will be loaded from the current {@link ClassLoader}. Non null.
   */
  public XmlExtensionLoaderDelegate(String modulePath) {
    checkArgument(!isEmpty(modulePath), "modulePath must not be empty");
    this.modulePath = modulePath;
  }

  public void declare(ExtensionLoadingContext context) {
    // We will assume the context classLoader of the current thread will be the one defined for the plugin (which is not filtered
    // and will allow us to access any resource in it
    URL resource = getResource(modulePath);
    if (resource == null) {
      throw new IllegalArgumentException(format("There's no reachable XML in the path '%s'", modulePath));
    }
    try {
      loadCustomTypes();
    } catch (Exception e) {
      throw new IllegalArgumentException(format("The custom type file [%s] for the module '%s' cannot be read properly",
                                                getCustomTypeFilename(), modulePath),
                                         e);
    }

    Document moduleDocument = getModuleDocument(context, resource);
    XmlApplicationParser xmlApplicationParser =
        new XmlApplicationParser(new SpiServiceRegistry(), singletonList(currentThread().getContextClassLoader()));
    Optional<ConfigLine> parseModule = xmlApplicationParser.parse(moduleDocument.getDocumentElement());
    if (!parseModule.isPresent()) {
      // This happens in org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser.configLineFromElement()
      throw new IllegalArgumentException(format("There was an issue trying to read the stream of '%s'", resource.getFile()));
    }
    ComponentModelReader componentModelReader =
        new ComponentModelReader(new DefaultConfigurationPropertiesResolver(empty(),
                                                                            new SystemPropertiesConfigurationProvider()));
    // TODO MULE-12291: we would, ideally, leave either the relative path (modulePath) or the URL to the current config file,
    // rather than just the name of the file (which will be useless from a tooling POV)
    final String configFileName = modulePath.substring(modulePath.lastIndexOf("/") + 1);
    ComponentModel componentModel = componentModelReader.extractComponentDefinitionModel(parseModule.get(), configFileName);

    loadModuleExtension(context.getExtensionDeclarer(), componentModel);
  }

  private URL getResource(String resource) {
    return currentThread().getContextClassLoader().getResource(resource);
  }

  /**
   * Custom types might not exist for the current module, that's why it's handled with {@link Optional}
   *
   * @throws Exception
   */
  private void loadCustomTypes() throws Exception {
    typesCatalog = empty();

    final String customTypes = getCustomTypeFilename();
    final URL resourceCustomType = getResource(customTypes);
    if (resourceCustomType != null) {
      final Element typesDocument = TypesCatalogXmlLoader.parseRootElement(resourceCustomType);
      final Optional<XmlMatcher> match = match(typesDocument, TypesCatalogXmlLoader.ELEM_MULE);
      if (match.isPresent()) {
        // TODO MULE-13214: then could be removed once MULE-13214 is done
        TypesCatalogXmlLoader typesCatalogXmlLoader = new TypesCatalogXmlLoader();
        typesCatalog = of(typesCatalogXmlLoader.load(resourceCustomType));
        typeResolver = getEmptyTypeResolver();
      } else {
        typeResolver = TypeResolver.createFrom(resourceCustomType, currentThread().getContextClassLoader());
      }
    } else {
      typeResolver = getEmptyTypeResolver();
    }
  }

  private TypeResolver getEmptyTypeResolver() {
    return TypeResolver.create(currentThread().getContextClassLoader());
  }

  /**
   * Possible file with the custom types, works by convention.
   *
   * @return given a {@code modulePath} such as "module-custom-types.xml" returns "module-custom-types-types.xml". Not null
   */
  private String getCustomTypeFilename() {
    return modulePath.replace(XML_SUFFIX, TYPES_XML_SUFFIX);
  }

  private Document getModuleDocument(ExtensionLoadingContext context, URL resource) {
    XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader = schemaValidatingDocumentLoader();
    try {
      final Set<ExtensionModel> extensions = new HashSet<>(context.getDslResolvingContext().getExtensions());
      return xmlConfigurationDocumentLoader.loadDocument(extensions, resource.getFile(), resource.openStream());
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("There was an issue reading the stream for the resource %s",
                                                                resource.getFile())));
    }
  }

  private void loadModuleExtension(ExtensionDeclarer declarer, ComponentModel moduleModel) {
    if (!moduleModel.getIdentifier().equals(MODULE_IDENTIFIER)) {
      throw new MuleRuntimeException(createStaticMessage(format("The root element of a module must be '%s', but found '%s'",
                                                                MODULE_IDENTIFIER.toString(),
                                                                moduleModel.getIdentifier().toString())));
    }
    String name = moduleModel.getParameters().get(MODULE_NAME);

    String version = "4.0"; // TODO(fernandezlautaro): MULE-11010 remove version from ExtensionModel
    final String category = moduleModel.getParameters().get(CATEGORY);
    final String vendor = moduleModel.getParameters().get(VENDOR);
    final String minMuleVersion = moduleModel.getParameters().get(MIN_MULE_VERSION);
    declarer.named(name)
        .describedAs(getDescription(moduleModel))
        .fromVendor(vendor)
        .withMinMuleVersion(new MuleVersion(minMuleVersion))
        .onVersion(version)
        .withCategory(Category.valueOf(category.toUpperCase()))
        .withXmlDsl(getXmlDslModel(moduleModel, name, version));
    declarer.withModelProperty(new XmlExtensionModelProperty());
    final Optional<ConfigurationDeclarer> configurationDeclarer = loadPropertiesFrom(declarer, moduleModel);
    if (configurationDeclarer.isPresent()) {
      loadOperationsFrom(configurationDeclarer.get(), moduleModel);
    } else {
      loadOperationsFrom(declarer, moduleModel);
    }
  }

  private XmlDslModel getXmlDslModel(ComponentModel moduleModel, String name, String version) {
    final Optional<String> prefix = ofNullable(moduleModel.getParameters().get(MODULE_PREFIX_ATTRIBUTE));
    final Optional<String> namespace = ofNullable(moduleModel.getParameters().get(MODULE_NAMESPACE_ATTRIBUTE));
    return createXmlLanguageModel(prefix, namespace, name, version);
  }

  private String getDescription(ComponentModel componentModel) {
    return componentModel.getParameters().getOrDefault(DOC_DESCRIPTION, "");
  }

  private List<ComponentModel> extractGlobalElementsFrom(ComponentModel moduleModel) {
    return moduleModel.getInnerComponents().stream()
        .filter(child -> !child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER)
            && !child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .collect(Collectors.toList());
  }

  private Optional<ConfigurationDeclarer> loadPropertiesFrom(ExtensionDeclarer declarer, ComponentModel moduleModel) {
    List<ComponentModel> globalElementsComponentModel = extractGlobalElementsFrom(moduleModel);
    List<ComponentModel> properties = moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER))
        .collect(Collectors.toList());

    if (!properties.isEmpty() || !globalElementsComponentModel.isEmpty()) {
      ConfigurationDeclarer configurationDeclarer = declarer.withConfig(CONFIG_NAME);
      configurationDeclarer.withModelProperty(new GlobalElementComponentModelModelProperty(globalElementsComponentModel));

      properties.stream().forEach(param -> extractProperty(configurationDeclarer, param));
      return of(configurationDeclarer);
    }
    return empty();
  }

  private void loadOperationsFrom(HasOperationDeclarer declarer, ComponentModel moduleModel) {
    moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .forEach(operationModel -> extractOperationExtension(declarer, operationModel));
  }

  private void extractOperationExtension(HasOperationDeclarer declarer, ComponentModel operationModel) {
    String operationName = assertValidName(operationModel.getNameAttribute());
    OperationDeclarer operationDeclarer = declarer.withOperation(operationName);
    ComponentModel bodyComponentModel = operationModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_BODY_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("The operation '%s' is missing the <body> statement",
                                                               operationName)));

    operationDeclarer.withModelProperty(new OperationComponentModelModelProperty(operationModel, bodyComponentModel));
    operationDeclarer.describedAs(getDescription(operationModel));
    extractOperationParameters(operationDeclarer, operationModel);
    extractOutputType(operationDeclarer, operationModel);
  }

  // TODO MULE-12619: until the internals of ExtensionModel doesn't validate or corrects the name, this is the custom validation
  private String assertValidName(String name) {
    if (!VALID_XML_NAME.matcher(name).matches()) {
      throw new IllegalModelDefinitionException(format("The name being used '%s' is not XML valid, it must start with a letter and can be followed by any letter, number or -, _. ",
                                                       name));
    }
    return name;
  }

  private void extractOperationParameters(OperationDeclarer operationDeclarer, ComponentModel componentModel) {
    Optional<ComponentModel> optionalParametersComponentModel = componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETERS_IDENTIFIER)).findAny();
    if (optionalParametersComponentModel.isPresent()) {
      optionalParametersComponentModel.get().getInnerComponents()
          .stream()
          .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETER_IDENTIFIER))
          .forEach(param -> {
            final String role = param.getParameters().get(ROLE);
            extractParameter(operationDeclarer, param, getRole(role));
          });
    }
  }

  private void extractProperty(ParameterizedDeclarer parameterizedDeclarer, ComponentModel param) {
    extractParameter(parameterizedDeclarer, param, BEHAVIOUR);
  }

  private void extractParameter(ParameterizedDeclarer parameterizedDeclarer, ComponentModel param, ParameterRole role) {
    Map<String, String> parameters = param.getParameters();
    String receivedInputType = parameters.get(TYPE_ATTRIBUTE);
    LayoutModel layoutModel = parseBoolean(parameters.get(PASSWORD)) ? builder().asPassword().build()
        : builder().build();
    MetadataType parameterType = extractType(receivedInputType);

    ParameterDeclarer parameterDeclarer = getParameterDeclarer(parameterizedDeclarer, parameters);
    parameterDeclarer.describedAs(getDescription(param))
        .withLayout(layoutModel)
        .withRole(role)
        .ofType(parameterType);
  }

  /**
   * Giving a {@link ParameterDeclarer} for the parameter and the attributes in the {@code parameters}, this method will verify
   * the rules for the {@link #ATTRIBUTE_USE} where:
   * <ul>
   * <li>{@link UseEnum#REQUIRED} marks the attribute as required in the XSD, failing if leaved empty when consuming the
   * parameter/property. It can not be {@link UseEnum#REQUIRED} if the parameter/property has a {@link #PARAMETER_DEFAULT_VALUE}
   * attribute</li>
   * <li>{@link UseEnum#OPTIONAL} marks the attribute as optional in the XSD. Can be {@link UseEnum#OPTIONAL} if the
   * parameter/property has a {@link #PARAMETER_DEFAULT_VALUE} attribute</li>
   * <li>{@link UseEnum#AUTO} will default at runtime to {@link UseEnum#REQUIRED} if {@link #PARAMETER_DEFAULT_VALUE} attribute is
   * absent, otherwise it will be marked as {@link UseEnum#OPTIONAL}</li>
   * </ul>
   *
   * @param parameterizedDeclarer builder to declare the {@link ParameterDeclarer}
   * @param parameters attributes to consume the values from
   * @return the {@link ParameterDeclarer}, being created as required or optional with a default value if applies.
   */
  private ParameterDeclarer getParameterDeclarer(ParameterizedDeclarer parameterizedDeclarer, Map<String, String> parameters) {
    final String parameterName = assertValidName(parameters.get(PARAMETER_NAME));
    final String parameterDefaultValue = parameters.get(PARAMETER_DEFAULT_VALUE);
    final UseEnum use = UseEnum.valueOf(parameters.get(ATTRIBUTE_USE));
    if (UseEnum.REQUIRED.equals(use) && isNotBlank(parameterDefaultValue)) {
      throw new IllegalParameterModelDefinitionException(format("The parameter [%s] cannot have the %s attribute set to %s when it has a default value",
                                                                parameterName, ATTRIBUTE_USE, UseEnum.REQUIRED));
    }
    // Is required if either is marked as REQUIRED or it's marked as AUTO an doesn't have a default value
    boolean parameterRequired = UseEnum.REQUIRED.equals(use) || (UseEnum.AUTO.equals(use) && isBlank(parameterDefaultValue));
    return parameterRequired ? parameterizedDeclarer.onDefaultParameterGroup().withRequiredParameter(parameterName)
        : parameterizedDeclarer.onDefaultParameterGroup().withOptionalParameter(parameterName)
            .defaultingTo(parameterDefaultValue);
  }

  private void extractOutputType(OperationDeclarer operationDeclarer, ComponentModel componentModel) {
    // output processing
    ComponentModel outputComponentModel = componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_OUTPUT_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Having an operation without <output> is not supported"));

    String receivedOutputType = outputComponentModel.getParameters().get(TYPE_ATTRIBUTE);
    MetadataType outputType = extractType(receivedOutputType);
    operationDeclarer.withOutput().describedAs(getDescription(outputComponentModel))
        .ofType(outputType);

    // output attribute processing
    Optional<ComponentModel> outputAttributesComponentModel = componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_OUTPUT_ATTRIBUTES_IDENTIFIER)).findFirst();
    OutputDeclarer outputAttributesDeclarer = operationDeclarer.withOutputAttributes();

    if (outputAttributesComponentModel.isPresent()) {
      String receivedOutputAttributeType = outputAttributesComponentModel.get().getParameters().get(TYPE_ATTRIBUTE);
      final MetadataType metadataType = extractType(receivedOutputAttributeType);
      outputAttributesDeclarer.describedAs(getDescription(outputAttributesComponentModel.get()))
          .ofType(metadataType);
    } else {
      outputAttributesDeclarer.ofType(BaseTypeBuilder.create(JAVA).voidType().build());
    }
  }

  private MetadataType extractType(String receivedType) {
    Optional<MetadataType> metadataType = empty();
    try {
      metadataType = typeResolver.resolveType(receivedType);
    } catch (TypeResolverException e) {
      // TODO MULE-13214: could be removed once MULE-13214 is done, as when fails fetching the type, then retries with the old
      // model
      if (typesCatalog.isPresent()) {
        metadataType = typesCatalog.get().resolveType(receivedType);
      }
      if (!metadataType.isPresent()) {
        throw new IllegalParameterModelDefinitionException(format("The type obtained [%s] cannot be resolved", receivedType), e);
      }
    }
    if (!metadataType.isPresent()) {
      String errorMessage = format(
                                   "should not have reach here. Type obtained [%s] when supported default types are [%s].",
                                   receivedType,
                                   join(", ", PRIMITIVE_TYPES.keySet()));
      throw new IllegalParameterModelDefinitionException(errorMessage);
    }
    return metadataType.get();
  }
}
