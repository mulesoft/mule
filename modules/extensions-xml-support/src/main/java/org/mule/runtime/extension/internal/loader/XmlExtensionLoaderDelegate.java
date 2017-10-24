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
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.display.LayoutModel.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.config.api.XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.TNS_PREFIX;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModulesModel.getUsedNamespaces;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.MODULE_CONNECTION_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.catalog.api.TypeResolver;
import org.mule.metadata.catalog.api.TypeResolverException;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.config.api.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.api.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.internal.dsl.model.ComponentModelReader;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel;
import org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModulesModel;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.OperationComponentModelModelProperty;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.TestConnectionGlobalElementModelProperty;
import org.mule.runtime.config.internal.util.NoOpXmlErrorHandler;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.xml.declaration.DeclarationOperation;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;
import org.mule.runtime.extension.internal.property.NoReconnectionStrategyModelProperty;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.w3c.dom.Document;

/**
 * Describes an {@link ExtensionModel} by scanning an XML provided in the constructor
 *
 * @since 4.0
 */
public final class XmlExtensionLoaderDelegate {

  public static final String CYCLIC_OPERATIONS_ERROR = "Cyclic operations detected, offending ones: [%s]";

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
  private static final String DOC_DESCRIPTION = "doc:description";
  private static final String PASSWORD = "password";
  private static final String ORDER_ATTRIBUTE = "order";
  private static final String TAB_ATTRIBUTE = "tab";
  private static final String DISPLAY_NAME_ATTRIBUTE = "displayName";
  private static final String SUMMARY_ATTRIBUTE = "summary";
  private static final String EXAMPLE_ATTRIBUTE = "example";
  private static final String ERROR_TYPE_ATTRIBUTE = "type";
  private static final String ROLE = "role";
  private static final String ATTRIBUTE_USE = "use";
  private static final String NAMESPACE_SEPARATOR = ":";

  private static final String TRANSFORMATION_FOR_TNS_RESOURCE = "META-INF/transform_for_tns.xsl";
  private static final String XMLNS_TNS = XMLNS_ATTRIBUTE + ":" + TNS_PREFIX;
  private static final String MODULE_CONNECTION_MARKER_ATTRIBUTE = "xmlns:connection";
  private static final String GLOBAL_ELEMENT_NAME_ATTRIBUTE = "name";

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
  private static final ComponentIdentifier CONNECTION_PROPERTIES_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("connection").build();
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
  private static final ComponentIdentifier OPERATION_ERRORS_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("errors").build();
  private static final ComponentIdentifier OPERATION_ERROR_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name("error").build();
  private static final ComponentIdentifier MODULE_IDENTIFIER =
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name(MODULE_NAMESPACE_NAME)
          .build();
  public static final String XSD_SUFFIX = ".xsd";
  private static final String XML_SUFFIX = ".xml";
  private static final String TYPES_XML_SUFFIX = "-catalog" + XML_SUFFIX;

  private final String modulePath;
  private final boolean validateXml;
  private final Optional<String> declarationPath;
  private TypeResolver typeResolver;
  private Map<String, DeclarationOperation> declarationMap;

  /**
   * @param modulePath relative path to a file that will be loaded from the current {@link ClassLoader}. Non null.
   * @param validateXml true if the XML of the Smart Connector must ve valid, false otherwise. It will be false at runtime, as
   * @param declarationPath relative path to a file that contains the {@link MetadataType}s of all <operations/>.
   */
  public XmlExtensionLoaderDelegate(String modulePath, boolean validateXml, Optional<String> declarationPath) {
    checkArgument(!isEmpty(modulePath), "modulePath must not be empty");
    this.modulePath = modulePath;
    this.validateXml = validateXml;
    this.declarationPath = declarationPath;
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
    loadDeclaration();
    Document moduleDocument = getModuleDocument(context, resource);
    loadModuleExtension(context.getExtensionDeclarer(), resource, moduleDocument,
                        context.getDslResolvingContext().getExtensions());
  }

  private URL getResource(String resource) {
    return currentThread().getContextClassLoader().getResource(resource);
  }

  /**
   * Custom types might not exist for the current module, that's why it's handled with {@link #getEmptyTypeResolver()}.
   */
  private void loadCustomTypes() {
    final String customTypes = getCustomTypeFilename();
    final URL resourceCustomType = getResource(customTypes);
    if (resourceCustomType != null) {
      typeResolver = TypeResolver.createFrom(resourceCustomType, currentThread().getContextClassLoader());
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

  /**
   * If a declaration file does exists, then it reads into a map to use it later on when describing the {@link ExtensionModel} of
   * the current <module/> for ever <operation/>s output and output attributes.
   *
   */
  private void loadDeclaration() {
    declarationMap = new HashMap<>();
    declarationPath.ifPresent(operationsOutputPathValue -> {
      final URL operationsOutputResource = getResource(operationsOutputPathValue);
      if (operationsOutputResource != null) {
        try {
          declarationMap = DeclarationOperation.fromString(IOUtils.toString(operationsOutputResource));
        } catch (IOException e) {
          throw new IllegalArgumentException(format("The declarations file [%s] for the module '%s' cannot be read properly",
                                                    operationsOutputPathValue, modulePath),
                                             e);
        }
      }
    });
  }

  private Document getModuleDocument(ExtensionLoadingContext context, URL resource) {
    XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader =
        validateXml ? schemaValidatingDocumentLoader() : schemaValidatingDocumentLoader(NoOpXmlErrorHandler::new);
    try {
      final Set<ExtensionModel> extensions = new HashSet<>(context.getDslResolvingContext().getExtensions());
      createTnsExtensionModel(resource, extensions).ifPresent(extensions::add);
      return xmlConfigurationDocumentLoader.loadDocument(extensions, resource.getFile(), resource.openStream());
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("There was an issue reading the stream for the resource %s",
                                                                resource.getFile())));
    }
  }

  /**
   * Transforms the current <module/> by stripping out the <body/>'s content, so that there are not parsing errors, to generate
   * a simpler {@link ExtensionModel} if there are references to the TNS prefix defined by the {@link #XMLNS_TNS}.
   *
   * @param resource <module/>'s resource
   * @param extensions complete list of extensions the current module depends on
   * @return an {@link ExtensionModel} if there's a {@link #XMLNS_TNS} defined, {@link Optional#empty()} otherwise
   * @throws IOException if it fails reading the resource
   */
  private Optional<ExtensionModel> createTnsExtensionModel(URL resource, Set<ExtensionModel> extensions) throws IOException {
    ExtensionModel result = null;
    final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
    try {
      final Source xslt = new StreamSource(getClass().getClassLoader().getResourceAsStream(TRANSFORMATION_FOR_TNS_RESOURCE));
      final Source moduleToTransform = new StreamSource(resource.openStream());
      TransformerFactory.newInstance()
          .newTransformer(xslt)
          .transform(moduleToTransform, new StreamResult(resultStream));
    } catch (TransformerException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("There was an issue transforming the stream for the resource %s while trying to remove the content of the <body> element to generate an XSD",
                                                                resource.getFile())),
                                     e);
    }
    final Document transformedModuleDocument = schemaValidatingDocumentLoader(NoOpXmlErrorHandler::new)
        .loadDocument(extensions, resource.getFile(), new ByteArrayInputStream(resultStream.toByteArray()));
    if (StringUtils.isNotBlank(transformedModuleDocument.getDocumentElement().getAttribute(XMLNS_TNS))) {
      final ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
      loadModuleExtension(extensionDeclarer, resource, transformedModuleDocument, extensions);

      result = new ExtensionModelFactory()
          .create(new DefaultExtensionLoadingContext(extensionDeclarer, currentThread().getContextClassLoader(),
                                                     new NullDslResolvingContext()));
    }
    return Optional.ofNullable(result);
  }

  private ComponentModel getModuleComponentModel(URL resource, Document moduleDocument) {
    XmlApplicationParser xmlApplicationParser =
        new XmlApplicationParser(new SpiServiceRegistry(), singletonList(currentThread().getContextClassLoader()));
    Optional<ConfigLine> parseModule = xmlApplicationParser.parse(moduleDocument.getDocumentElement());
    if (!parseModule.isPresent()) {
      // This happens in org.mule.runtime.config.dsl.processor.xml.XmlApplicationParser.configLineFromElement()
      throw new IllegalArgumentException(format("There was an issue trying to read the stream of '%s'", resource.getFile()));
    }
    ComponentModelReader componentModelReader =
        new ComponentModelReader(new DefaultConfigurationPropertiesResolver(empty(),
                                                                            new SystemPropertiesConfigurationProvider()));
    return componentModelReader.extractComponentDefinitionModel(parseModule.get(), modulePath);
  }

  private void loadModuleExtension(ExtensionDeclarer declarer, URL resource, Document moduleDocument,
                                   Set<ExtensionModel> extensions) {
    final ComponentModel moduleModel = getModuleComponentModel(resource, moduleDocument);
    if (!moduleModel.getIdentifier().equals(MODULE_IDENTIFIER)) {
      throw new MuleRuntimeException(createStaticMessage(format("The root element of a module must be '%s', but found '%s'",
                                                                MODULE_IDENTIFIER.toString(),
                                                                moduleModel.getIdentifier().toString())));
    }
    String name = moduleModel.getParameters().get(MODULE_NAME);

    String version = "4.0.0"; // TODO(fernandezlautaro): MULE-11010 remove version from ExtensionModel
    final String category = moduleModel.getParameters().get(CATEGORY);
    final String vendor = moduleModel.getParameters().get(VENDOR);
    final XmlDslModel xmlDslModel = getXmlDslModel(moduleModel, name, version);

    final String xmlnsTnsValue = moduleModel.getParameters().get(XMLNS_TNS);
    if (xmlnsTnsValue != null && !xmlDslModel.getNamespace().equals(xmlnsTnsValue)) {
      throw new MuleRuntimeException(createStaticMessage(format("The %s attribute value of the module must be '%s', but found '%s'",
                                                                XMLNS_TNS,
                                                                xmlDslModel.getNamespace(),
                                                                xmlnsTnsValue)));
    }

    DirectedGraph<String, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    declarer.named(name)
        .describedAs(getDescription(moduleModel))
        .fromVendor(vendor)
        .onVersion(version)
        .withCategory(Category.valueOf(category.toUpperCase()))
        .withXmlDsl(xmlDslModel);
    declarer.withModelProperty(getXmlExtensionModelProperty(moduleModel, xmlDslModel));
    final Optional<ConfigurationDeclarer> configurationDeclarer = loadPropertiesFrom(declarer, moduleModel, extensions);
    if (configurationDeclarer.isPresent()) {
      loadOperationsFrom(configurationDeclarer.get(), moduleModel, directedGraph, xmlDslModel);
    } else {
      loadOperationsFrom(declarer, moduleModel, directedGraph, xmlDslModel);
    }

    final CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(directedGraph);
    final Set<String> cycles = cycleDetector.findCycles();
    if (!cycles.isEmpty()) {
      throw new MuleRuntimeException(createStaticMessage(format(CYCLIC_OPERATIONS_ERROR, new TreeSet(cycles))));
    }
  }

  /**
   * Calculates all the used namespaces of the given <module/> leaving behind the (possible) cyclic reference if there are
   * {@link MacroExpansionModuleModel#TNS_PREFIX} references by removing the current namespace generation.
   *
   * @param moduleModel XML of the <module/>
   * @param xmlDslModel the {@link XmlDslModel} for the current {@link ExtensionModel} generation
   * @return a {@link XmlExtensionModelProperty} which contains all the namespaces dependencies. Among them could be dependencies
   * that must be macro expanded and others which might not, but that job is left for the
   * {@link MacroExpansionModulesModel#getDirectExpandableNamespaceDependencies(ComponentModel, Set)}
   */
  private XmlExtensionModelProperty getXmlExtensionModelProperty(ComponentModel moduleModel,
                                                                 XmlDslModel xmlDslModel) {
    final Set<String> namespaceDependencies = getUsedNamespaces(moduleModel).stream()
        .filter(namespace -> !xmlDslModel.getNamespace().equals(namespace))
        .collect(Collectors.toSet());
    return new XmlExtensionModelProperty(namespaceDependencies);
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
    final Set<ComponentIdentifier> NOT_GLOBAL_ELEMENT_IDENTIFIERS = Sets
        .newHashSet(OPERATION_PROPERTY_IDENTIFIER, CONNECTION_PROPERTIES_IDENTIFIER, OPERATION_IDENTIFIER);
    return moduleModel.getInnerComponents().stream()
        .filter(child -> !NOT_GLOBAL_ELEMENT_IDENTIFIERS.contains(child.getIdentifier()))
        .collect(Collectors.toList());
  }

  private Optional<ConfigurationDeclarer> loadPropertiesFrom(ExtensionDeclarer declarer, ComponentModel moduleModel,
                                                             Set<ExtensionModel> extensions) {
    List<ComponentModel> globalElementsComponentModel = extractGlobalElementsFrom(moduleModel);
    List<ComponentModel> configurationProperties = extractProperties(moduleModel);
    List<ComponentModel> connectionProperties = extractConnectionProperties(moduleModel);
    validateProperties(configurationProperties, connectionProperties);

    if (!configurationProperties.isEmpty() || !connectionProperties.isEmpty() || !globalElementsComponentModel.isEmpty()) {
      declarer.withModelProperty(new NoReconnectionStrategyModelProperty());
      ConfigurationDeclarer configurationDeclarer = declarer.withConfig(CONFIG_NAME);
      configurationDeclarer.withModelProperty(new GlobalElementComponentModelModelProperty(globalElementsComponentModel));
      configurationProperties.stream().forEach(param -> extractProperty(configurationDeclarer, param));

      addConnectionProvider(configurationDeclarer, connectionProperties, globalElementsComponentModel, extensions);
      return of(configurationDeclarer);
    }
    return empty();
  }

  private List<ComponentModel> extractProperties(ComponentModel moduleModel) {
    return moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER))
        .collect(Collectors.toList());
  }

  private List<ComponentModel> extractConnectionProperties(ComponentModel moduleModel) {
    final List<ComponentModel> connectionsComponentModel = moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(CONNECTION_PROPERTIES_IDENTIFIER))
        .collect(Collectors.toList());
    if (connectionsComponentModel.size() > 1) {
      throw new MuleRuntimeException(createStaticMessage(format("There cannot be more than 1 child [%s] element per [%s], found [%d]",
                                                                CONNECTION_PROPERTIES_IDENTIFIER.getName(),
                                                                MODULE_IDENTIFIER.getName(),
                                                                connectionsComponentModel.size())));
    }
    return connectionsComponentModel.isEmpty() ? Collections.EMPTY_LIST : extractProperties(connectionsComponentModel.get(0));
  }

  /**
   * Throws exception if a <property/> for a configuration or connection have the same name.
   * @param configurationProperties properties that will go in the configuration
   * @param connectionProperties properties that will go in the connection
   */
  private void validateProperties(List<ComponentModel> configurationProperties, List<ComponentModel> connectionProperties) {
    final List<String> connectionPropertiesNames =
        connectionProperties.stream().map(ComponentModel::getNameAttribute).collect(Collectors.toList());
    List<String> intersectedProperties = configurationProperties.stream()
        .map(ComponentModel::getNameAttribute)
        .filter(connectionPropertiesNames::contains)
        .collect(Collectors.toList());
    if (!intersectedProperties.isEmpty()) {
      throw new MuleRuntimeException(createStaticMessage(format("There cannot be properties with the same name even if they are within a <connection>, repeated properties are: [%s]",
                                                                intersectedProperties.stream()
                                                                    .collect(Collectors.joining(", ")))));
    }
  }

  /**
   * Adds a connection provider if (a) there's at least one global element that has test connection or (b) there's at least one
   * <property/> that has been placed within a <connection/> wrapper in the <module/> element.
   *
   * @param configurationDeclarer declarer to add the {@link ConnectionProviderDeclarer} if applies.
   * @param connectionProperties collection of <property/>s that should be added to the {@link ConnectionProviderDeclarer}.
   * @param globalElementsComponentModel collection of global elements where through {@link #getTestConnectionGlobalElement(List, Set)}
   *                                     will look for one that supports test connectivity.
   * @param extensions used also in {@link #getTestConnectionGlobalElement(List, Set)}, through the {@link #findTestConnectionGlobalElementFrom},
   *                   as the XML of the extensions might change of the values that the {@link ExtensionModel} has (heavily relies
   *                   on {@link DslSyntaxResolver#resolve(NamedObject)}).
   */
  private void addConnectionProvider(ConfigurationDeclarer configurationDeclarer,
                                     List<ComponentModel> connectionProperties,
                                     List<ComponentModel> globalElementsComponentModel, Set<ExtensionModel> extensions) {
    final Optional<ComponentModel> testConnectionGlobalElementOptional =
        getTestConnectionGlobalElement(globalElementsComponentModel, extensions);

    if (testConnectionGlobalElementOptional.isPresent() || !connectionProperties.isEmpty()) {
      final ConnectionProviderDeclarer connectionProviderDeclarer =
          configurationDeclarer.withConnectionProvider(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME);
      connectionProviderDeclarer
          .withConnectionManagementType(ConnectionManagementType.NONE);
      connectionProperties.stream().forEach(param -> extractProperty(connectionProviderDeclarer, param));

      testConnectionGlobalElementOptional.ifPresent(
                                                    testConnectionGlobalElement -> {
                                                      final String testConnectionGlobalElementName = testConnectionGlobalElement
                                                          .getParameters().get(GLOBAL_ELEMENT_NAME_ATTRIBUTE);
                                                      connectionProviderDeclarer
                                                          .withModelProperty(new TestConnectionGlobalElementModelProperty(testConnectionGlobalElementName));
                                                    });
      //TODO until MULE-12734, all test connection must be shut down in smart connectors
      connectionProviderDeclarer.supportsConnectivityTesting(false);
    }

  }

  private Optional<ComponentModel> getTestConnectionGlobalElement(List<ComponentModel> globalElementsComponentModel,
                                                                  Set<ExtensionModel> extensions) {
    final List<ComponentModel> markedAsTestConnectionGlobalElements =
        globalElementsComponentModel.stream()
            .filter(globalElementComponentModel -> Boolean
                .parseBoolean(globalElementComponentModel.getParameters().get(MODULE_CONNECTION_MARKER_ATTRIBUTE)))
            .collect(Collectors.toList());

    if (markedAsTestConnectionGlobalElements.size() > 1) {
      throw new MuleRuntimeException(createStaticMessage(format("It can only be one global element marked as test connectivity [%s] but found [%d], offended global elements are: [%s]",
                                                                MODULE_CONNECTION_MARKER_ATTRIBUTE,
                                                                markedAsTestConnectionGlobalElements.size(),
                                                                markedAsTestConnectionGlobalElements.stream().map(
                                                                                                                  ComponentModel::getNameAttribute)
                                                                    .collect(Collectors.joining(", ")))));
    }
    Optional<ComponentModel> testConnectionGlobalElement = markedAsTestConnectionGlobalElements.stream().findFirst();
    if (!testConnectionGlobalElement.isPresent()) {
      testConnectionGlobalElement = findTestConnectionGlobalElementFrom(globalElementsComponentModel, extensions);
    }
    return testConnectionGlobalElement;
  }

  /**
   * Goes over all {@code globalElementsComponentModel} looking for the configuration and connection elements (parent and child),
   * where if present looks for the {@link ExtensionModel}s validating if the element is in fact a {@link ConnectionProvider}.
   * It heavily relies on the {@link DslSyntaxResolver}, as many elements in the XML do not match to the names of the model.
   * 
   * @param globalElementsComponentModel global elements of the smart connector
   * @param extensions set of extensions used to generate the current {@link ExtensionModel}
   * @return a {@link ComponentModel} of the global element to do test connection, empty otherwise.
   */
  private Optional<ComponentModel> findTestConnectionGlobalElementFrom(List<ComponentModel> globalElementsComponentModel,
                                                                       Set<ExtensionModel> extensions) {
    Optional<ComponentModel> testConnectionGlobalElement;
    final DslResolvingContext dslResolvingContext = DslResolvingContext.getDefault(extensions);
    final Set<ComponentModel> testConnectionComponentModels = new HashSet<>();

    for (ComponentModel globalElementComponentModel : globalElementsComponentModel) {
      for (ComponentModel connectionProviderChildElement : globalElementComponentModel.getInnerComponents()) {
        final String globalElementConfigurationModelName = globalElementComponentModel.getIdentifier().getName();
        final String childConnectionProviderName = connectionProviderChildElement.getIdentifier().getName();
        for (ExtensionModel extensionModel : extensions) {
          final DslSyntaxResolver dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel, dslResolvingContext);
          for (ConfigurationModel configurationModel : extensionModel.getConfigurationModels()) {

            if (dslSyntaxResolver.resolve(configurationModel).getElementName().equals(
                                                                                      globalElementConfigurationModelName)) {
              for (ConnectionProviderModel connectionProviderModel : configurationModel.getConnectionProviders()) {
                if (dslSyntaxResolver.resolve(connectionProviderModel).getElementName()
                    .equals(childConnectionProviderName)) {
                  testConnectionComponentModels.add(globalElementComponentModel);
                }
              }
            }
          }
        }
      }
    }
    if (testConnectionComponentModels.size() > 1) {
      throw new MuleRuntimeException(createStaticMessage(format("There are [%d] global elements that can be potentially used for test connection when it should be just one. Mark any of them with the attribute [%s=\"true\"], offended global elements are: [%s]",
                                                                testConnectionComponentModels.size(),
                                                                MODULE_CONNECTION_MARKER_ATTRIBUTE,
                                                                testConnectionComponentModels.stream()
                                                                    .map(ComponentModel::getNameAttribute)
                                                                    .sorted()
                                                                    .collect(Collectors.joining(", ")))));
    }
    testConnectionGlobalElement = testConnectionComponentModels.stream().findFirst();
    return testConnectionGlobalElement;
  }

  private void loadOperationsFrom(HasOperationDeclarer declarer, ComponentModel moduleModel,
                                  DirectedGraph<String, DefaultEdge> directedGraph, XmlDslModel xmlDslModel) {
    moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .forEach(operationModel -> extractOperationExtension(declarer, operationModel, directedGraph, xmlDslModel));
  }

  private void extractOperationExtension(HasOperationDeclarer declarer, ComponentModel operationModel,
                                         DirectedGraph<String, DefaultEdge> directedGraph, XmlDslModel xmlDslModel) {
    String operationName = operationModel.getNameAttribute();
    OperationDeclarer operationDeclarer = declarer.withOperation(operationName);
    ComponentModel bodyComponentModel = operationModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_BODY_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("The operation '%s' is missing the <body> statement",
                                                               operationName)));

    directedGraph.addVertex(operationName);
    fillGraphWithTnsReferences(directedGraph, operationName, bodyComponentModel.getInnerComponents());

    operationDeclarer.withModelProperty(new OperationComponentModelModelProperty(operationModel, bodyComponentModel));
    operationDeclarer.describedAs(getDescription(operationModel));
    operationDeclarer.getDeclaration().setDisplayModel(getDisplayModel(operationModel));
    extractOperationParameters(operationDeclarer, operationModel);
    extractOutputType(operationDeclarer.withOutput(), OPERATION_OUTPUT_IDENTIFIER, operationModel,
                      getDeclarationOutputFor(operationName));
    extractOutputType(operationDeclarer.withOutputAttributes(), OPERATION_OUTPUT_ATTRIBUTES_IDENTIFIER, operationModel,
                      getDeclarationOutputAttributesFor(operationName));
    declareErrorModels(operationDeclarer, xmlDslModel, operationName, operationModel);
  }

  private Optional<MetadataType> getDeclarationOutputFor(String operationName) {
    Optional<MetadataType> result = Optional.empty();
    if (declarationMap.containsKey(operationName)) {
      result = Optional.of(declarationMap.get(operationName).getOutput());
    }
    return result;
  }

  private Optional<MetadataType> getDeclarationOutputAttributesFor(String operationName) {
    Optional<MetadataType> result = Optional.empty();
    if (declarationMap.containsKey(operationName)) {
      result = Optional.of(declarationMap.get(operationName).getOutputAttributes());
    }
    return result;
  }

  /**
   * Goes over the {@code innerComponents} collection checking if any reference is a {@link MacroExpansionModuleModel#TNS_PREFIX},
   * in which case it adds an edge to the current vertex {@code sourceOperationVertex}
   *
   * @param directedGraph graph to contain all the vertex operations and linkage with other operations
   * @param sourceOperationVertex current vertex we are working on
   * @param innerComponents collection of elements to introspect and assembly the graph with
   */
  private void fillGraphWithTnsReferences(DirectedGraph<String, DefaultEdge> directedGraph, String sourceOperationVertex,
                                          final List<ComponentModel> innerComponents) {
    innerComponents.forEach(childMPComponentModel -> {
      if (TNS_PREFIX.equals(childMPComponentModel.getIdentifier().getNamespace())) {
        //we will take the current component model name, as any child of it are actually TNS child references (aka: parameters)
        final String targetOperationVertex = childMPComponentModel.getIdentifier().getName();
        if (!directedGraph.containsVertex(targetOperationVertex)) {
          directedGraph.addVertex(targetOperationVertex);
        }
        directedGraph.addEdge(sourceOperationVertex, targetOperationVertex);
      } else {
        //scenario for nested scopes that might be having cyclic references to operations
        childMPComponentModel.getInnerComponents()
            .forEach(childChildMPComponentModel -> fillGraphWithTnsReferences(directedGraph, sourceOperationVertex,
                                                                              childMPComponentModel.getInnerComponents()));
      }
    });
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
    final LayoutModel.LayoutModelBuilder layoutModelBuilder = builder();
    if (parseBoolean(parameters.get(PASSWORD))) {
      layoutModelBuilder.asPassword();
    }
    layoutModelBuilder.order(getOrder(parameters.get(ORDER_ATTRIBUTE)));
    layoutModelBuilder.tabName(getTab(parameters.get(TAB_ATTRIBUTE)));

    final DisplayModel displayModel = getDisplayModel(param);
    MetadataType parameterType = extractType(receivedInputType);

    ParameterDeclarer parameterDeclarer = getParameterDeclarer(parameterizedDeclarer, parameters);
    parameterDeclarer.describedAs(getDescription(param))
        .withLayout(layoutModelBuilder.build())
        .withDisplayModel(displayModel)
        .withRole(role)
        .ofType(parameterType);
  }

  private DisplayModel getDisplayModel(ComponentModel componentModel) {
    final DisplayModel.DisplayModelBuilder displayModelBuilder = DisplayModel.builder();
    displayModelBuilder.displayName(componentModel.getParameters().get(DISPLAY_NAME_ATTRIBUTE));
    displayModelBuilder.summary(componentModel.getParameters().get(SUMMARY_ATTRIBUTE));
    displayModelBuilder.example(componentModel.getParameters().get(EXAMPLE_ATTRIBUTE));
    return displayModelBuilder.build();
  }

  private String getTab(String tab) {
    return StringUtils.isBlank(tab) ? Placement.DEFAULT_TAB : tab;
  }

  private int getOrder(final String order) {
    try {
      return Integer.parseInt(order);
    } catch (NumberFormatException e) {
      return Placement.DEFAULT_ORDER;
    }
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
    final String parameterName = parameters.get(PARAMETER_NAME);
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

  private void extractOutputType(OutputDeclarer outputDeclarer, ComponentIdentifier componentIdentifier,
                                 ComponentModel operationModel, Optional<MetadataType> calculatedOutput) {
    Optional<ComponentModel> outputAttributesComponentModel = operationModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(componentIdentifier)).findFirst();
    outputAttributesComponentModel
        .ifPresent(outputComponentModel -> outputDeclarer.describedAs(getDescription(outputComponentModel)));

    MetadataType metadataType = getMetadataType(outputAttributesComponentModel, calculatedOutput);
    outputDeclarer.ofType(metadataType);
  }

  private MetadataType getMetadataType(Optional<ComponentModel> outputAttributesComponentModel,
                                       Optional<MetadataType> declarationMetadataType) {
    MetadataType metadataType;
    //the calculated metadata has precedence over the one configured in the xml
    if (declarationMetadataType.isPresent()) {
      metadataType = declarationMetadataType.get();
    } else {
      //if tye element is absent, it will default to the VOID type
      if (outputAttributesComponentModel.isPresent()) {
        String receivedOutputAttributeType = outputAttributesComponentModel.get().getParameters().get(TYPE_ATTRIBUTE);
        metadataType = extractType(receivedOutputAttributeType);
      } else {
        metadataType = BaseTypeBuilder.create(JAVA).voidType().build();
      }
    }
    return metadataType;
  }

  private MetadataType extractType(String receivedType) {
    Optional<MetadataType> metadataType = empty();
    try {
      metadataType = typeResolver.resolveType(receivedType);
    } catch (TypeResolverException e) {
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

  private void declareErrorModels(OperationDeclarer operationDeclarer, XmlDslModel xmlDslModel, String operationName,
                                  ComponentModel operationModel) {
    Optional<ComponentModel> optionalParametersComponentModel = operationModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_ERRORS_IDENTIFIER)).findAny();
    optionalParametersComponentModel.ifPresent(componentModel -> componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_ERROR_IDENTIFIER))
        .forEach(param -> {
          final String namespace = xmlDslModel.getPrefix().toUpperCase();
          final String typeName = param.getParameters().get(ERROR_TYPE_ATTRIBUTE);
          if (StringUtils.isBlank(typeName)) {
            throw new IllegalModelDefinitionException(format("The operation [%s] cannot have an <error> with an empty 'type' attribute",
                                                             operationName));
          }
          if (typeName.contains(NAMESPACE_SEPARATOR)) {
            throw new IllegalModelDefinitionException(format("The operation [%s] cannot have an <error> [%s] that contains a reserved character [%s]",
                                                             operationName, typeName,
                                                             NAMESPACE_SEPARATOR));
          }
          operationDeclarer.withErrorModel(ErrorModelBuilder.newError(typeName, namespace)
              .withParent(ErrorModelBuilder.newError(ANY).build())
              .build());
        }));
  }
}
