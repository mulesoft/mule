/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.display.LayoutModel.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.util.IOUtils.getInputStreamWithCacheControl;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyComponentTreeRecursively;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.extension.api.util.XmlModelUtils.createXmlLanguageModel;
import static org.mule.runtime.extension.internal.ast.MacroExpansionModuleModel.MODULE_CONNECTION_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.extension.internal.ast.MacroExpansionModuleModel.TNS_PREFIX;
import static org.mule.runtime.extension.internal.config.dsl.xml.ModuleXmlNamespaceInfoProvider.MODULE_DSL_NAMESPACE;
import static org.mule.runtime.extension.internal.config.dsl.xml.ModuleXmlNamespaceInfoProvider.MODULE_ROOT_NODE_NAME;
import static org.mule.runtime.extension.internal.loader.xml.TlsEnabledComponentUtils.MODULE_TLS_ENABLED_MARKER_ANNOTATION_QNAME;
import static org.mule.runtime.extension.internal.loader.xml.TlsEnabledComponentUtils.addTlsContextParameter;
import static org.mule.runtime.extension.internal.loader.xml.TlsEnabledComponentUtils.isTlsConfigurationSupported;
import static org.mule.runtime.extension.privileged.util.ComponentDeclarationUtils.withNoReconnectionStrategy;
import static org.mule.runtime.module.extension.internal.loader.ExtensionDevelopmentFramework.XML_SDK;
import static org.mule.runtime.module.extension.internal.runtime.exception.ErrorMappingUtils.forEachErrorMappingDo;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getValidatedJavaVersionsIntersection;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.catalog.api.TypeResolver;
import org.mule.metadata.catalog.api.TypeResolverException;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
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
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationProperty;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.extension.XmlSdk1ExtensionModelProvider;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.xml.declaration.DeclarationOperation;
import org.mule.runtime.extension.api.model.operation.ImmutableOperationModel;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.internal.ast.MacroExpansionModuleModel;
import org.mule.runtime.extension.internal.ast.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.extension.internal.ast.property.OperationComponentModelModelProperty;
import org.mule.runtime.extension.internal.ast.property.PrivateOperationsModelProperty;
import org.mule.runtime.extension.internal.ast.property.TestConnectionGlobalElementModelProperty;
import org.mule.runtime.extension.internal.factories.XmlSdkConfigurationFactory;
import org.mule.runtime.extension.internal.factories.XmlSdkConnectionProviderFactory;
import org.mule.runtime.extension.internal.loader.xml.validator.property.InvalidTestConnectionMarkerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DevelopmentFrameworkModelProperty;
import org.mule.runtime.properties.api.ConfigurationProperty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.vibur.objectpool.ConcurrentPool;
import org.vibur.objectpool.PoolService;
import org.vibur.objectpool.util.ConcurrentLinkedQueueCollection;

/**
 * Describes an {@link ExtensionModel} by scanning an XML provided in the constructor
 *
 * @since 4.0
 */
public final class XmlExtensionLoaderDelegate {

  public static final String CYCLIC_OPERATIONS_ERROR = "Cyclic operations detected, offending ones: [%s]";

  private static final String RAISE_ERROR = "raise-error";
  private static final ComponentIdentifier RAISE_ERROR_IDENTIFIER =
      ComponentIdentifier.builder().namespace(CORE_PREFIX).name(RAISE_ERROR).build();
  public static final String GLOBAL_PROPERTY = "global-property";

  private static final String PARAMETER_NAME = "name";
  private static final String PARAMETER_DEFAULT_VALUE = "defaultValue";
  private static final String TYPE_ATTRIBUTE = "type";
  private static final String MODULE_NAME = "name";
  private static final String MODULE_NAMESPACE_NAME = MODULE_DSL_NAMESPACE;
  protected static final String CONFIG_NAME = "config";

  private static final Map<String, ParameterRole> parameterRoleTypes = ImmutableMap.<String, ParameterRole>builder()
      .put("BEHAVIOUR", ParameterRole.BEHAVIOUR)
      .put("CONTENT", ParameterRole.CONTENT)
      .put("PRIMARY", ParameterRole.PRIMARY_CONTENT)
      .build();

  private static final String CATEGORY = "category";
  private static final String VENDOR = "vendor";
  private static final String PASSWORD = "password";
  private static final String ORDER_ATTRIBUTE = "order";
  private static final String TAB_ATTRIBUTE = "tab";
  private static final String DISPLAY_NAME_ATTRIBUTE = "displayName";
  private static final String SUMMARY_ATTRIBUTE = "summary";
  private static final String EXAMPLE_ATTRIBUTE = "example";
  private static final String ERROR_TYPE_ATTRIBUTE = "type";
  private static final String ROLE = "role";
  private static final String ATTRIBUTE_USE = "use";
  private static final String ATTRIBUTE_VISIBILITY = "visibility";
  private static final String NAMESPACE_SEPARATOR = ":";

  private static final String XMLNS_TNS = XMLNS_ATTRIBUTE + ":" + TNS_PREFIX;
  private static final QName MODULE_CONNECTION_MARKER_ANNOTATION_QNAME =
      new QName("http://www.w3.org/2000/xmlns/", "connection", "xmlns");
  public static final String MODULE_CONNECTION_MARKER_ANNOTATION_ATTRIBUTE =
      MODULE_CONNECTION_MARKER_ANNOTATION_QNAME.getPrefix() + ":" + MODULE_CONNECTION_MARKER_ANNOTATION_QNAME.getLocalPart();
  private static final String GLOBAL_ELEMENT_NAME_ATTRIBUTE = "name";

  /**
   * ENUM used to discriminate which type of {@link ParameterDeclarer} has to be created (required or not).
   *
   * @see #getParameterDeclarer(ParameterizedDeclarer, Map)
   */
  private enum UseEnum {
    REQUIRED, OPTIONAL, AUTO
  }

  /**
   * ENUM used to discriminate which visibility an <operation/> has.
   *
   * @see {@link XmlExtensionLoaderDelegate#loadOperationsFrom(HasOperationDeclarer, ComponentAst, DirectedGraph, XmlDslModel,
   *      XmlExtensionLoaderDelegate.OperationVisibility, Optional<ExtensionModel>)}
   */
  public enum OperationVisibility {
    PRIVATE, PUBLIC
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
      ComponentIdentifier.builder().namespace(MODULE_NAMESPACE_NAME).name(MODULE_ROOT_NODE_NAME)
          .build();
  public static final String XSD_SUFFIX = ".xsd";
  private static final String XML_SUFFIX = ".xml";
  private static final String TYPES_XML_SUFFIX = "-catalog" + XML_SUFFIX;

  // Set a conservative value for how big the pool can get
  private static final int FOR_TNS_XSTL_TRANSFORMER_POOL_MAX_SIZE = max(1, getRuntime().availableProcessors() / 2);
  private static PoolService<Transformer> FOR_TNS_XSTL_TRANSFORMER_POOL = calculateTransformerPool();

  /**
   * Only for testing purposes. Determines whether to force the recreation of the transformer pool
   * {@code FOR_TNS_XSTL_TRANSFORMER_POOL}.
   */
  @Deprecated
  private static boolean forceTransformerPoolRecreation;

  /**
   * Only for testing purposes.
   *
   * @param force whether to force the recreation of the transformer pool {@code FOR_TNS_XSTL_TRANSFORMER_POOL}.
   */
  @Deprecated
  public static void forceTransformerPoolRecreation(boolean force) {
    forceTransformerPoolRecreation = force;
  }

  private static final Set<ComponentIdentifier> NOT_GLOBAL_ELEMENT_IDENTIFIERS =
      newHashSet(OPERATION_PROPERTY_IDENTIFIER, CONNECTION_PROPERTIES_IDENTIFIER, OPERATION_IDENTIFIER);

  private static PoolService<Transformer> getTransformerPool() {
    if (forceTransformerPoolRecreation) {
      FOR_TNS_XSTL_TRANSFORMER_POOL = calculateTransformerPool();
    }

    return FOR_TNS_XSTL_TRANSFORMER_POOL;
  }

  private static PoolService<Transformer> calculateTransformerPool() {
    return withContextClassLoader(XmlExtensionLoaderDelegate.class.getClassLoader(),
                                  () -> new ConcurrentPool<>(new ConcurrentLinkedQueueCollection<>(),
                                                             new ForTnsTransformerFactory(),
                                                             1, FOR_TNS_XSTL_TRANSFORMER_POOL_MAX_SIZE, false));
  }

  private static final ExtensionModelLoader TEMP_EXTENSION_MODEL_LOADER = new ExtensionModelLoader() {

    @Override
    public String getId() {
      return "xmlSdkTemp";
    }

    @Override
    protected void declareExtension(ExtensionLoadingContext context) {
      // nothing to do
    }
  };

  private final String modulePath;
  private final boolean validateXml;
  private final Optional<String> declarationPath;
  private final List<String> resourcesPaths;
  private TypeResolver typeResolver;
  private Map<String, DeclarationOperation> declarationMap;

  /**
   * @param modulePath      relative path to a file that will be loaded from the current {@link ClassLoader}. Non null.
   * @param validateXml     true if the XML of the Smart Connector must ve valid, false otherwise. It will be false at runtime, as
   *                        the packaging of a connector will previously validate it's XML.
   * @param declarationPath relative path to a file that contains the {@link MetadataType}s of all <operations/>.
   * @param resourcesPaths  set of resources that will be exported in the {@link ExtensionModel}
   */
  public XmlExtensionLoaderDelegate(String modulePath, boolean validateXml, Optional<String> declarationPath,
                                    List<String> resourcesPaths) {
    checkArgument(!isEmpty(modulePath), "modulePath must not be empty");
    this.modulePath = modulePath;
    this.validateXml = validateXml;
    this.declarationPath = declarationPath;
    this.resourcesPaths = resourcesPaths;
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

    final Set<ExtensionModel> loaderExtensions = new HashSet<>(context.getDslResolvingContext().getExtensions());
    loaderExtensions.add(XmlSdk1ExtensionModelProvider.getExtensionModel());

    final Set<ExtensionModel> extensions = new HashSet<>(loaderExtensions);
    try {
      createTnsExtensionModel(resource, loaderExtensions).ifPresent(extensions::add);
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage(format("There was an issue reading the stream for the resource %s",
                                                                resource.getFile())));
    }

    ArtifactAst moduleAst = getModuleDocument(extensions, resource);
    loadModuleExtension(context.getExtensionDeclarer(), moduleAst, false);
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
   */
  private void loadDeclaration() {
    declarationMap = new HashMap<>();
    declarationPath.ifPresent(operationsOutputPathValue -> {
      final URL operationsOutputResource = getResource(operationsOutputPathValue);
      if (operationsOutputResource != null) {
        try {
          declarationMap = DeclarationOperation.fromString(IOUtils.toString(operationsOutputResource, UTF_8));
        } catch (IOException e) {
          throw new IllegalArgumentException(format("The declarations file [%s] for the module '%s' cannot be read properly",
                                                    operationsOutputPathValue, modulePath),
                                             e);
        }
      }
    });
  }

  private ArtifactAst getModuleDocument(Set<ExtensionModel> extensions, URL resource) {
    Builder parserBuilder = AstXmlParser.builder()
        .withExtensionModels(extensions);
    if (!validateXml) {
      parserBuilder = parserBuilder.withSchemaValidationsDisabled();
    }
    AstXmlParser xmlToAstParser = parserBuilder
        .build();

    return xmlToAstParser.parse(resource);
  }

  /**
   * Transforms the current <module/> by stripping out the <body/>'s content, so that there are not parsing errors, to generate a
   * simpler {@link ExtensionModel} if there are references to the TNS prefix defined by the {@link #XMLNS_TNS}.
   *
   * @param resource        <module/>'s resource
   * @param extensionModels models for the extensions in context
   * @return an {@link ExtensionModel} if there's a {@link #XMLNS_TNS} defined, {@link Optional#empty()} otherwise
   * @throws IOException if it fails reading the resource
   */
  private Optional<ExtensionModel> createTnsExtensionModel(URL resource, Set<ExtensionModel> extensionModels)
      throws IOException {
    final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
    final Transformer transformer = getTransformerPool().take();
    try (BufferedInputStream resourceIS = new BufferedInputStream(getInputStreamWithCacheControl(resource))) {
      transformer.transform(new StreamSource(resourceIS), new StreamResult(resultStream));
    } catch (TransformerException e) {
      throw new MuleRuntimeException(createStaticMessage(format("There was an issue transforming the stream for the resource %s while trying to remove the content of the <body> element to generate an XSD",
                                                                resource.getFile())),
                                     e);
    } finally {
      getTransformerPool().restore(transformer);
    }

    final ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
    AstXmlParser xmlToAstParser = AstXmlParser.builder()
        .withExtensionModels(extensionModels)
        .withSchemaValidationsDisabled()
        .build();

    ArtifactAst transformedModuleAst =
        xmlToAstParser.parse("transformed_" + resource.getFile(), resultStream.toInputStream());

    if (transformedModuleAst.namespaceDefinition().getUnresovedNamespaces().containsKey(XMLNS_TNS)) {
      loadModuleExtension(extensionDeclarer, transformedModuleAst, true);
      return of(createExtensionModel(extensionDeclarer));
    } else {
      return empty();
    }
  }

  private ComponentAst getModuleComponentModel(ArtifactAst moduleAst) {
    moduleAst.updatePropertiesResolver(getConfigurationPropertiesResolver(moduleAst));
    return moduleAst.topLevelComponentsStream()
        .filter(c -> MODULE_IDENTIFIER.equals(c.getIdentifier()))
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format(
                                                                               "The root element of a module must be '%s' but it wasn't found",
                                                                               MODULE_IDENTIFIER.toString()))));
  }

  private ConfigurationPropertiesResolver getConfigurationPropertiesResolver(ArtifactAst ast) {
    return new ConfigurationPropertiesHierarchyBuilder()
        .withGlobalPropertiesSupplier(createProviderFromGlobalProperties(ast))
        .withEnvironmentProperties()
        .withSystemProperties()
        .withPropertiesFile(new ClassLoaderResourceProvider(currentThread().getContextClassLoader()))
        .withoutFailuresIfPropertyNotPresent()
        .build();
  }

  private LazyValue<Map<String, ConfigurationProperty>> createProviderFromGlobalProperties(ArtifactAst ast) {
    return new LazyValue<>(() -> {
      final Map<String, ConfigurationProperty> globalProperties = new HashMap<>();

      // Root element is the mule:module
      ast.topLevelComponentsStream()
          .flatMap(comp -> comp.directChildrenStream())
          .filter(comp -> GLOBAL_PROPERTY.equals(comp.getIdentifier().getName()))
          .forEach(comp -> {
            final String key = comp.getParameter(DEFAULT_GROUP_NAME, "name").getResolvedRawValue();
            final String rawValue = comp.getParameter(DEFAULT_GROUP_NAME, "value").getRawValue();
            globalProperties.put(key,
                                 new DefaultConfigurationProperty(format("global-property - file: %s - lineNumber %s",
                                                                         comp.getMetadata().getFileName().orElse("(n/a)"),
                                                                         comp.getMetadata().getStartLine().orElse(-1)),
                                                                  key, rawValue));
          });

      return globalProperties;
    });
  }

  private static Optional<String> getStringParameter(ComponentAst componentAst, String parameterName) {
    return ofNullable(componentAst.getParameter(DEFAULT_GROUP_NAME, parameterName)).map(c -> (String) c.getValue().getRight());
  }

  private void loadModuleExtension(ExtensionDeclarer declarer, ArtifactAst artifactAst, boolean comesFromTNS) {
    ComponentAst moduleAst = getModuleComponentModel(artifactAst);

    final String name = getStringParameter(moduleAst, MODULE_NAME).orElse(null);
    final String version = "4.0.0"; // TODO(fernandezlautaro): MULE-11010 remove version from ExtensionModel
    final String category = getStringParameter(moduleAst, CATEGORY).orElse("COMMUNITY");
    final String vendor = getStringParameter(moduleAst, VENDOR).orElse("MuleSoft");
    final XmlDslModel xmlDslModel = comesFromTNS
        ? getTnsXmlDslModel(artifactAst, name, version)
        : getXmlDslModel(artifactAst, name, version);
    final String description = getDescription(moduleAst);
    final String xmlnsTnsValue = artifactAst.namespaceDefinition().getUnresovedNamespaces().getOrDefault(XMLNS_TNS, null);
    if (!comesFromTNS && xmlnsTnsValue != null && !xmlDslModel.getNamespace().equals(xmlnsTnsValue)) {
      throw new MuleRuntimeException(createStaticMessage(format("The %s attribute value of the module must be '%s', but found '%s'",
                                                                XMLNS_TNS,
                                                                xmlDslModel.getNamespace(),
                                                                xmlnsTnsValue)));
    }
    resourcesPaths.stream().forEach(declarer::withResource);

    fillDeclarer(declarer, name, version, category, vendor, xmlDslModel, description);
    declarer
        .withModelProperty(getXmlExtensionModelProperty(artifactAst, xmlDslModel))
        .withModelProperty(new DevelopmentFrameworkModelProperty(XML_SDK))
        .supportingJavaVersions(getValidatedJavaVersionsIntersection(name, "Module", artifactAst.dependencies()));

    Graph<String, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    // loading public operations
    final List<ComponentAst> globalElementsComponentModel = extractGlobalElementsFrom(moduleAst);
    addGlobalElementModelProperty(declarer, globalElementsComponentModel);
    final Optional<ConfigurationDeclarer> configurationDeclarer =
        loadPropertiesFrom(declarer, moduleAst, globalElementsComponentModel);
    final HasOperationDeclarer hasOperationDeclarer = configurationDeclarer.map(d -> (HasOperationDeclarer) d).orElse(declarer);

    ExtensionDeclarer temporalPublicOpsDeclarer = new ExtensionDeclarer();
    fillDeclarer(temporalPublicOpsDeclarer, name, version, category, vendor, xmlDslModel, description);
    loadOperationsFrom(empty(), temporalPublicOpsDeclarer, moduleAst, directedGraph, xmlDslModel,
                       OperationVisibility.PUBLIC, empty());

    validateNoCycles(directedGraph);

    try {
      moduleAst = enrichRecursively(moduleAst, createExtensionModel(temporalPublicOpsDeclarer));
    } catch (IllegalModelDefinitionException e) {
      // Nothing to do, this failure will be thrown again when the actual declarer, hasOperationDeclarer, is used to build the
      // extension model.
    }

    // loading private operations
    if (comesFromTNS) {
      // when parsing for the TNS, we need the <operation/>s to be part of the extension model to validate the XML properly
      loadOperationsFrom(empty(), hasOperationDeclarer, moduleAst, directedGraph, xmlDslModel,
                         OperationVisibility.PRIVATE, empty());
    } else {
      // when parsing for the macro expansion, the <operation/>s will be left in the PrivateOperationsModelProperty model property
      ExtensionDeclarer temporalPrivateOpsDeclarer = new ExtensionDeclarer();
      fillDeclarer(temporalPrivateOpsDeclarer, name, version, category, vendor, xmlDslModel, description);
      loadOperationsFrom(empty(), temporalPrivateOpsDeclarer, moduleAst, directedGraph, xmlDslModel,
                         OperationVisibility.PRIVATE, empty());
      final ExtensionModel privateTnsExtensionModel = createExtensionModel(temporalPrivateOpsDeclarer);
      moduleAst = enrichRecursively(moduleAst, privateTnsExtensionModel);

      final PrivateOperationsModelProperty privateOperations =
          new PrivateOperationsModelProperty(privateTnsExtensionModel.getOperationModels());
      declarer.withModelProperty(privateOperations);
    }

    validateNoCycles(directedGraph);

    // make all operations added to hasOperationDeclarer be properly enriched with the referenced operations, whether they are
    // public or private.
    ExtensionDeclarer temporalAllOpsDeclarer = new ExtensionDeclarer();
    fillDeclarer(temporalAllOpsDeclarer, name, version, category, vendor, xmlDslModel, description);
    loadOperationsFrom(empty(), temporalAllOpsDeclarer, moduleAst, directedGraph, xmlDslModel,
                       OperationVisibility.PRIVATE, empty());
    loadOperationsFrom(empty(), temporalAllOpsDeclarer, moduleAst, directedGraph, xmlDslModel,
                       OperationVisibility.PUBLIC, empty());

    Optional<ExtensionModel> allTnsExtensionModel = empty();
    try {
      allTnsExtensionModel = of(createExtensionModel(temporalAllOpsDeclarer));
    } catch (IllegalModelDefinitionException e) {
      // Nothing to do, this failure will be thrown again when the actual declarer, hasOperationDeclarer, is used to build the
      // extension model.
    }

    loadOperationsFrom(of(declarer), hasOperationDeclarer, moduleAst, directedGraph, xmlDslModel,
                       OperationVisibility.PUBLIC, allTnsExtensionModel);
    addErrorModels(declarer, artifactAst);
  }

  private void addErrorModels(ExtensionDeclarer declarer, ArtifactAst artifactAst) {
    // Add error models for error types used in raise and error mapping for completeness of the extension model.
    artifactAst.recursiveStream()
        .forEach(comp -> {
          if (comp.getIdentifier().equals(RAISE_ERROR_IDENTIFIER)) {
            final ComponentParameterAst parameter = comp.getParameter(DEFAULT_GROUP_NAME, ERROR_TYPE_ATTRIBUTE);

            if (parameter != null) {
              parameter.getValue().getValue()
                  .map(r -> (String) r)
                  // We can just ignore this as we should allow an empty value here
                  .filter(representation -> !isEmpty(representation))
                  .ifPresent(representation -> {
                    ComponentIdentifier raiseType = buildFromStringRepresentation(representation);
                    declarer.withErrorModel(ErrorModelBuilder.newError(raiseType.getName(), raiseType.getNamespace())
                        .withParent(ErrorModelBuilder.newError(ANY).build())
                        .build());
                  });
            }
          }

          forEachErrorMappingDo(comp, mappings -> mappings
              .forEach(mapping -> {
                final String representation = mapping.getTarget();
                if (isEmpty(representation)) {
                  // We can just ignore this as it will be validated afterwards
                  return;
                }

                ComponentIdentifier target = buildFromStringRepresentation(representation);
                declarer.withErrorModel(ErrorModelBuilder.newError(target.getName(), target.getNamespace())
                    .withParent(ErrorModelBuilder.newError(ANY).build())
                    .build());
              }));
        });
  }

  private void validateNoCycles(Graph<String, DefaultEdge> directedGraph) {
    CycleDetector<String, DefaultEdge> cycleDetector;
    Set<String> cycles;
    cycleDetector = new CycleDetector<>(directedGraph);
    cycles = cycleDetector.findCycles();
    if (!cycles.isEmpty()) {
      throw new MuleRuntimeException(createStaticMessage(format(CYCLIC_OPERATIONS_ERROR, new TreeSet<>(cycles))));
    }
  }

  private ComponentAst enrichRecursively(final ComponentAst moduleAst, ExtensionModel result) {
    return copyComponentTreeRecursively(moduleAst, comp -> {
      if (TNS_PREFIX.equals(comp.getIdentifier().getNamespace())) {
        // TODO MULE-17419 (AST) Set all models, not just for operations (routers/scopes are missing here for sure)
        final Optional<OperationModel> enrichedOperationModel = result.getOperationModel(comp.getIdentifier().getName())
            .filter(model -> OperationModel.class.isAssignableFrom(model.getClass()))
            .map(model -> enrichOperationModel(model, result));

        return new BaseComponentAstDecorator(comp) {

          @Override
          public <M> Optional<M> getModel(Class<M> modelClass) {
            return enrichedOperationModel.isPresent() && OperationModel.class.isAssignableFrom(modelClass)
                ? (Optional<M>) enrichedOperationModel
                : comp.getModel(modelClass);
          }
        };
      } else {
        return comp;
      }
    });
  }

  private OperationModel enrichOperationModel(OperationModel model, ExtensionModel result) {

    final Set<ModelProperty> enrichedModelProperties = model.getModelProperties()
        .stream()
        .map(mp -> {
          if (mp instanceof OperationComponentModelModelProperty) {
            final OperationComponentModelModelProperty ocm = (OperationComponentModelModelProperty) mp;
            return new OperationComponentModelModelProperty(enrichRecursively(ocm.getOperationComponentModel(), result),
                                                            enrichRecursively(ocm.getBodyComponentModel(), result));
          } else {
            return mp;
          }
        })
        .collect(toSet());

    if (model instanceof OperationModel) {
      OperationModel opModel = model;
      return new ImmutableOperationModel(opModel.getName(),
                                         opModel.getDescription(),
                                         opModel.getParameterGroupModels(),
                                         opModel.getNestedComponents(),
                                         opModel.getOutput(),
                                         opModel.getOutputAttributes(),
                                         opModel.isBlocking(),
                                         opModel.getExecutionType(),
                                         opModel.requiresConnection(),
                                         opModel.isTransactional(),
                                         opModel.supportsStreaming(),
                                         opModel.getDisplayModel().orElse(null),
                                         opModel.getErrorModels(),
                                         opModel.getStereotype(),
                                         opModel.getVisibility(),
                                         enrichedModelProperties,
                                         opModel.getNotificationModels(),
                                         opModel.getDeprecationModel().orElse(null));
    } else {
      return model;
    }
  }

  private ExtensionModel createExtensionModel(ExtensionDeclarer declarer) {
    return TEMP_EXTENSION_MODEL_LOADER.loadExtensionModel(declarer, builder(currentThread().getContextClassLoader(),
                                                                            nullDslResolvingContext()).build());
  }

  private void fillDeclarer(ExtensionDeclarer declarer, String name, String version, String category, String vendor,
                            XmlDslModel xmlDslModel, String description) {
    declarer.named(name)
        .describedAs(description)
        .fromVendor(vendor)
        .onVersion(version)
        .withCategory(Category.valueOf(category.toUpperCase()))
        .withXmlDsl(xmlDslModel);
  }

  /**
   * Calculates all the used namespaces of the given <module/> leaving behind the (possible) cyclic reference if there are
   * {@link MacroExpansionModuleModel#TNS_PREFIX} references by removing the current namespace generation.
   *
   * @param moduleAst   AST of the <module/>
   * @param xmlDslModel the {@link XmlDslModel} for the current {@link ExtensionModel} generation
   * @return a {@link XmlExtensionModelProperty} which contains all the namespaces dependencies. Among them could be dependencies
   *         that must be macro expanded and others which might not.
   */
  private XmlExtensionModelProperty getXmlExtensionModelProperty(ArtifactAst moduleAst,
                                                                 XmlDslModel xmlDslModel) {
    return new XmlExtensionModelProperty(moduleAst.dependencies().stream()
        .map(em -> em.getXmlDslModel().getNamespace())
        .filter(namespace -> !xmlDslModel.getNamespace().equals(namespace))
        .collect(toSet()));
  }

  private XmlDslModel getTnsXmlDslModel(ArtifactAst moduleAst, String name, String version) {
    final String namespace = moduleAst.namespaceDefinition().getUnresovedNamespaces().get(XMLNS_TNS);
    final String stringPrefix = TNS_PREFIX;

    final Map<String, String> schemaLocations = moduleAst.namespaceDefinition().getSchemaLocations();

    if (!schemaLocations.containsKey(namespace)) {
      return getXmlDslModel(moduleAst, name, version);
    }

    final String[] tnsSchemaLocationParts = schemaLocations.get(namespace).split("/");

    return XmlDslModel.builder()
        .setSchemaVersion(version)
        .setPrefix(stringPrefix)
        .setNamespace(namespace)
        .setSchemaLocation(schemaLocations.get(namespace))
        .setXsdFileName(tnsSchemaLocationParts[tnsSchemaLocationParts.length - 1])
        .build();
  }

  private XmlDslModel getXmlDslModel(ArtifactAst artifactAst, String name, String version) {
    final Optional<String> prefix = ofNullable(artifactAst.namespaceDefinition().getPrefix());
    final Optional<String> namespace = ofNullable(artifactAst.namespaceDefinition().getNamespace());
    return createXmlLanguageModel(prefix, namespace, name, version);
  }

  private String getDescription(ComponentAst moduleAst) {
    return moduleAst.getMetadata().getDocAttributes().getOrDefault("description", "");
  }

  private List<ComponentAst> extractGlobalElementsFrom(ComponentAst moduleAst) {
    return moduleAst.directChildrenStream()
        .filter(child -> !NOT_GLOBAL_ELEMENT_IDENTIFIERS.contains(child.getIdentifier()))
        .collect(toList());
  }

  private Optional<ConfigurationDeclarer> loadPropertiesFrom(ExtensionDeclarer declarer, ComponentAst moduleAst,
                                                             List<ComponentAst> globalElementsComponentModel) {
    List<ComponentAst> configurationProperties = extractProperties(moduleAst);
    List<ComponentAst> connectionProperties = extractConnectionProperties(moduleAst);
    validateProperties(configurationProperties, connectionProperties);

    Optional<ComponentAst> tlsEnabledComponent = getTlsEnabledComponent(moduleAst);

    if (!configurationProperties.isEmpty() || !connectionProperties.isEmpty() || tlsEnabledComponent.isPresent()) {
      withNoReconnectionStrategy(declarer);
      ConfigurationDeclarer configurationDeclarer = declarer.withConfig(CONFIG_NAME);
      tlsEnabledComponent.ifPresent(comp -> addTlsContextParameter(configurationDeclarer.onDefaultParameterGroup(), comp));
      configurationProperties.forEach(param -> extractProperty(configurationDeclarer, param));
      final XmlSdkConfigurationFactory configurationFactory = new XmlSdkConfigurationFactory(configurationDeclarer
          .getDeclaration()
          .getAllParameters());
      addConnectionProvider(configurationDeclarer, connectionProperties, globalElementsComponentModel, configurationFactory);

      configurationDeclarer.withModelProperty(new ConfigurationFactoryModelProperty(configurationFactory));
      return of(configurationDeclarer);
    }
    return empty();
  }

  private void addGlobalElementModelProperty(ExtensionDeclarer declarer, List<ComponentAst> globalElementsComponentModel) {
    if (!globalElementsComponentModel.isEmpty()) {
      declarer.withModelProperty(new GlobalElementComponentModelModelProperty(globalElementsComponentModel));
    }
  }

  private List<ComponentAst> extractProperties(ComponentAst moduleAst) {
    return moduleAst.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER))
        .collect(toList());
  }

  private List<ComponentAst> extractConnectionProperties(ComponentAst moduleAst) {
    final List<ComponentAst> connectionsComponentModel = moduleAst.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(CONNECTION_PROPERTIES_IDENTIFIER))
        .collect(toList());
    if (connectionsComponentModel.size() > 1) {
      throw new MuleRuntimeException(createStaticMessage(format("There cannot be more than 1 child [%s] element per [%s], found [%d]",
                                                                CONNECTION_PROPERTIES_IDENTIFIER.getName(),
                                                                MODULE_IDENTIFIER.getName(),
                                                                connectionsComponentModel.size())));
    }
    return connectionsComponentModel.isEmpty() ? emptyList() : extractProperties(connectionsComponentModel.get(0));
  }

  /**
   * Throws exception if a <property/> for a configuration or connection have the same name.
   *
   * @param configurationProperties properties that will go in the configuration
   * @param connectionProperties    properties that will go in the connection
   */
  private void validateProperties(List<ComponentAst> configurationProperties, List<ComponentAst> connectionProperties) {
    final List<String> connectionPropertiesNames =
        connectionProperties.stream()
            .map(ComponentAst::getComponentId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
    List<String> intersectedProperties = configurationProperties.stream()
        .map(ComponentAst::getComponentId)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(connectionPropertiesNames::contains)
        .collect(toList());
    if (!intersectedProperties.isEmpty()) {
      throw new MuleRuntimeException(createStaticMessage(format("There cannot be properties with the same name even if they are within a <connection>, repeated properties are: [%s]",
                                                                intersectedProperties.stream()
                                                                    .collect(joining(", ")))));
    }
  }

  /**
   * Adds a connection provider if (a) there's at least one global element that has test connection or (b) there's at least one
   * <property/> that has been placed within a <connection/> wrapper in the <module/> element.
   *
   * @param configurationDeclarer        declarer to add the {@link ConnectionProviderDeclarer} if applies.
   * @param connectionProperties         collection of <property/>s that should be added to the
   *                                     {@link ConnectionProviderDeclarer}.
   * @param globalElementsComponentModel collection of global elements where through
   *                                     {@link #getTestConnectionGlobalElement(ConfigurationDeclarer, List, Set)} will look for
   *                                     one that supports test connectivity.
   * @param configurationFactory         the factory of the config.
   */
  private void addConnectionProvider(ConfigurationDeclarer configurationDeclarer,
                                     List<ComponentAst> connectionProperties,
                                     List<ComponentAst> globalElementsComponentModel,
                                     XmlSdkConfigurationFactory configurationFactory) {
    final Optional<ComponentAst> testConnectionGlobalElementOptional =
        getTestConnectionGlobalElement(configurationDeclarer, globalElementsComponentModel);

    if (testConnectionGlobalElementOptional.isPresent() || !connectionProperties.isEmpty()) {
      final ConnectionProviderDeclarer connectionProviderDeclarer =
          configurationDeclarer.withConnectionProvider(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME);
      connectionProviderDeclarer
          .withConnectionManagementType(ConnectionManagementType.NONE);
      connectionProperties.stream().forEach(param -> extractProperty(connectionProviderDeclarer, param));

      delegateConnectionProviderFactory(testConnectionGlobalElementOptional,
                                        configurationDeclarer,
                                        connectionProviderDeclarer,
                                        configurationFactory);

      testConnectionGlobalElementOptional
          .flatMap(testConnectionGlobalElement -> testConnectionGlobalElement
              .getParameter(DEFAULT_GROUP_NAME, GLOBAL_ELEMENT_NAME_ATTRIBUTE)
              .getValue().getValue())
          .ifPresent(testConnectionGlobalElementName -> connectionProviderDeclarer
              .withModelProperty(new TestConnectionGlobalElementModelProperty((String) testConnectionGlobalElementName)));
    }

  }

  /**
   * Make the connectionProviderFactory for the XML SDK extension be just a delegate to the connectionProviderFactory of its used
   * connection provider.
   *
   * @param testConnectionGlobalElementOptional the connection from the XML SDK extension to use for connectivity testing.
   * @param configurationDeclarer               the declarer to get the config parameters from.
   * @param connectionProviderDeclarer          the declarer to enrich with the {@link ConnectionProviderFactory}, if any.
   * @param configurationFactory                the factory of the config.
   */
  private void delegateConnectionProviderFactory(final Optional<ComponentAst> testConnectionGlobalElementOptional,
                                                 final ConfigurationDeclarer configurationDeclarer,
                                                 final ConnectionProviderDeclarer connectionProviderDeclarer,
                                                 XmlSdkConfigurationFactory configurationFactory) {
    Optional<ComponentAst> connectionProvider = testConnectionGlobalElementOptional
        .flatMap(testConnectionGlobalElement -> testConnectionGlobalElement.directChildrenStream()
            .filter(child -> child.getModel(ConnectionProviderModel.class).isPresent())
            .findFirst());

    connectionProvider
        .flatMap(connectionProviderComponent -> connectionProviderComponent.getModel(ConnectionProviderModel.class)
            .flatMap(connectionProviderModel -> connectionProviderModel
                .getModelProperty(ConnectionProviderFactoryModelProperty.class)
                .map(connectionProviderFactoryModelProperty -> new XmlSdkConnectionProviderFactory(connectionProviderComponent,
                                                                                                   configurationDeclarer
                                                                                                       .getDeclaration()
                                                                                                       .getAllParameters(),
                                                                                                   connectionProviderDeclarer
                                                                                                       .getDeclaration()
                                                                                                       .getAllParameters(),
                                                                                                   configurationFactory))))
        .ifPresent(xmlSdkConnectionProviderFactory -> connectionProviderDeclarer
            .withModelProperty(new ConnectionProviderFactoryModelProperty(xmlSdkConnectionProviderFactory)));
  }

  private String getComponentIdForErrorMessage(ComponentAst componentAst) {
    return componentAst.getComponentId().orElse("unnamed@" + componentAst.getLocation().getLocation());
  }

  private boolean isEnabledBooleanAnnotation(ComponentAst componentAst, QName annotationName) {
    Object annotation = componentAst.getAnnotations().get(annotationName.toString());
    if (annotation == null) {
      return false;
    }

    return parseBoolean(annotation.toString());
  }

  private Predicate<ComponentAst> withEnabledBooleanAnnotation(QName annotationName) {
    return componentAst -> isEnabledBooleanAnnotation(componentAst, annotationName);
  }

  private Optional<ComponentAst> findAnnotatedElement(Stream<ComponentAst> elements, QName annotationQName) {

    final List<ComponentAst> annotatedElements = elements
        .filter(withEnabledBooleanAnnotation(annotationQName))
        .collect(toList());
    if (annotatedElements.size() > 1) {
      throw new MuleRuntimeException(createStaticMessage(format("There can only be one global element marked with [%s:%s] but found [%d], offending global elements are: [%s]",
                                                                annotationQName.getPrefix(),
                                                                annotationQName.getLocalPart(),
                                                                annotatedElements.size(),
                                                                annotatedElements.stream()
                                                                    .map(this::getComponentIdForErrorMessage)
                                                                    .collect(joining(", ")))));
    }

    return annotatedElements.stream().findFirst();
  }

  private void validateIsTlsConfigurationSupported(ComponentAst componentAst) {
    if (!isTlsConfigurationSupported(componentAst)) {
      throw new MuleRuntimeException(createStaticMessage(format("The annotated element [%s] with [%s:%s] is not valid to be configured for TLS (the component [%s] does not support it)",
                                                                getComponentIdForErrorMessage(componentAst),
                                                                MODULE_TLS_ENABLED_MARKER_ANNOTATION_QNAME.getPrefix(),
                                                                MODULE_TLS_ENABLED_MARKER_ANNOTATION_QNAME.getLocalPart(),
                                                                componentAst.getIdentifier())));
    }
  }

  private Optional<ComponentAst> getTlsEnabledComponent(ComponentAst moduleAst) {
    Optional<ComponentAst> tlsEnabledElement =
        findAnnotatedElement(moduleAst.recursiveStream(), MODULE_TLS_ENABLED_MARKER_ANNOTATION_QNAME);
    tlsEnabledElement.ifPresent(this::validateIsTlsConfigurationSupported);
    return tlsEnabledElement;
  }

  private Optional<ComponentAst> getTestConnectionGlobalElement(ConfigurationDeclarer configurationDeclarer,
                                                                List<ComponentAst> globalElementsComponentModel) {
    Optional<ComponentAst> testConnectionGlobalElement = findAnnotatedElement(globalElementsComponentModel.stream(),
                                                                              MODULE_CONNECTION_MARKER_ANNOTATION_QNAME);
    if (!testConnectionGlobalElement.isPresent()) {
      testConnectionGlobalElement = findTestConnectionGlobalElementFrom(globalElementsComponentModel);
    } else {
      // validates that the MODULE_CONNECTION_MARKER_ANNOTATION_ATTRIBUTE is on a correct XML element that supports test
      // connection
      Optional<ComponentAst> temporalTestConnectionGlobalElement =
          findTestConnectionGlobalElementFrom(singletonList(testConnectionGlobalElement.get()));
      if ((!temporalTestConnectionGlobalElement.isPresent())
          || (!temporalTestConnectionGlobalElement.get().equals(testConnectionGlobalElement.get()))) {
        configurationDeclarer.withModelProperty(new InvalidTestConnectionMarkerModelProperty(testConnectionGlobalElement
            .flatMap(ComponentAst::getComponentId).orElse(null), testConnectionGlobalElement.get().getIdentifier().toString()));
      }
    }
    return testConnectionGlobalElement;
  }

  /**
   * Goes over all {@code globalElementsComponentModel} looking for the configuration and connection elements (parent and child),
   * where if present looks for the {@link ExtensionModel}s validating if the element is in fact a {@link ConnectionProvider}. It
   * heavily relies on the {@link DslSyntaxResolver}, as many elements in the XML do not match to the names of the model.
   *
   * @param globalElementsComponentModel global elements of the smart connector
   * @return a {@link ComponentAst} of the global element to do test connection, empty otherwise.
   */
  private Optional<ComponentAst> findTestConnectionGlobalElementFrom(List<ComponentAst> globalElementsComponentModel) {
    final List<ComponentAst> testConnectionComponentModels = globalElementsComponentModel
        .stream()
        .filter(globalElementComponentModel -> globalElementComponentModel.getModel(ConfigurationModel.class)
            .map(configurationModel -> configurationModel.getConnectionProviders().stream()
                .anyMatch(ConnectionProviderModel::supportsConnectivityTesting))
            .orElse(false))
        .collect(toList());

    if (testConnectionComponentModels.size() > 1) {
      throw new MuleRuntimeException(createStaticMessage(format("There are [%d] global elements that can be potentially used for test connection when it should be just one. Mark any of them with the attribute [%s=\"true\"], offended global elements are: [%s]",
                                                                testConnectionComponentModels.size(),
                                                                MODULE_CONNECTION_MARKER_ANNOTATION_ATTRIBUTE,
                                                                testConnectionComponentModels.stream()
                                                                    .map(ComponentAst::getComponentId)
                                                                    .filter(Optional::isPresent)
                                                                    .map(Optional::get)
                                                                    .sorted()
                                                                    .collect(joining(", ")))));
    }
    return testConnectionComponentModels.stream().findFirst();
  }

  private void loadOperationsFrom(Optional<ExtensionDeclarer> extensionDeclarer,
                                  HasOperationDeclarer declarer, ComponentAst moduleAst,
                                  Graph<String, DefaultEdge> directedGraph, XmlDslModel xmlDslModel,
                                  final OperationVisibility visibility, Optional<ExtensionModel> tnsExtensionModel) {

    moduleAst.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .filter(operationModel -> operationModel.getParameter(DEFAULT_GROUP_NAME, ATTRIBUTE_VISIBILITY).getValue().getRight()
            .equals(visibility.toString()))
        .forEach(operationModel -> extractOperationExtension(extensionDeclarer, declarer, operationModel, directedGraph,
                                                             xmlDslModel,
                                                             tnsExtensionModel));
  }

  private void extractOperationExtension(Optional<ExtensionDeclarer> extensionDeclarer,
                                         HasOperationDeclarer declarer, ComponentAst operationModel,
                                         Graph<String, DefaultEdge> directedGraph, XmlDslModel xmlDslModel,
                                         Optional<ExtensionModel> tnsExtensionModel) {
    String operationName = operationModel.getComponentId().orElse(null);
    OperationDeclarer operationDeclarer = declarer.withOperation(operationName);
    ComponentAst bodyComponentModel = operationModel.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(OPERATION_BODY_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("The operation '%s' is missing the <body> statement",
                                                               operationName)));

    directedGraph.addVertex(operationName);
    fillGraphWithTnsReferences(directedGraph, operationName, bodyComponentModel.directChildrenStream());

    if (tnsExtensionModel.isPresent()) {
      operationDeclarer.withModelProperty(new OperationComponentModelModelProperty(enrichRecursively(operationModel,
                                                                                                     tnsExtensionModel.get()),
                                                                                   enrichRecursively(bodyComponentModel,
                                                                                                     tnsExtensionModel.get())));
    } else {
      operationDeclarer.withModelProperty(new OperationComponentModelModelProperty(operationModel, bodyComponentModel));
    }

    operationDeclarer.describedAs(getDescription(operationModel));
    operationDeclarer.getDeclaration().setDisplayModel(getDisplayModel(operationModel));
    extractOperationParameters(operationDeclarer, operationModel);
    extractOutputType(operationDeclarer.withOutput(), OPERATION_OUTPUT_IDENTIFIER, operationModel,
                      getDeclarationOutputFor(operationName));
    extractOutputType(operationDeclarer.withOutputAttributes(), OPERATION_OUTPUT_ATTRIBUTES_IDENTIFIER, operationModel,
                      getDeclarationOutputAttributesFor(operationName));
    declareErrorModels(extensionDeclarer, operationDeclarer, xmlDslModel, operationName, operationModel);
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
   * @param directedGraph         graph to contain all the vertex operations and linkage with other operations
   * @param sourceOperationVertex current vertex we are working on
   * @param innerComponents       collection of elements to introspect and assembly the graph with
   */
  private void fillGraphWithTnsReferences(Graph<String, DefaultEdge> directedGraph, String sourceOperationVertex,
                                          final Stream<ComponentAst> innerComponents) {
    innerComponents.forEach(childMPComponentModel -> {
      if (TNS_PREFIX.equals(childMPComponentModel.getIdentifier().getNamespace())) {
        // we will take the current component model name, as any child of it are actually TNS child references (aka: parameters)
        final String targetOperationVertex = childMPComponentModel.getIdentifier().getName();
        if (!directedGraph.containsVertex(targetOperationVertex)) {
          directedGraph.addVertex(targetOperationVertex);
        }
        directedGraph.addEdge(sourceOperationVertex, targetOperationVertex);
      } else {
        // scenario for nested scopes that might be having cyclic references to operations
        childMPComponentModel.directChildrenStream()
            .forEach(childChildMPComponentModel -> fillGraphWithTnsReferences(directedGraph, sourceOperationVertex,
                                                                              childMPComponentModel.directChildrenStream()));
      }
    });
  }

  private void extractOperationParameters(OperationDeclarer operationDeclarer, ComponentAst componentModel) {
    Optional<ComponentAst> optionalParametersComponentModel = componentModel.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETERS_IDENTIFIER)).findAny();
    if (optionalParametersComponentModel.isPresent()) {
      optionalParametersComponentModel.get().directChildrenStream()
          .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETER_IDENTIFIER))
          .forEach(param -> {
            final String role = param.getParameter(DEFAULT_GROUP_NAME, ROLE).getValue().getRight().toString();
            extractParameter(operationDeclarer, param, getRole(role));
          });
    }
  }

  private void extractProperty(ParameterizedDeclarer parameterizedDeclarer, ComponentAst param) {
    extractParameter(parameterizedDeclarer, param, BEHAVIOUR);
  }

  private void extractParameter(ParameterizedDeclarer parameterizedDeclarer, ComponentAst param, ParameterRole role) {
    final LayoutModel.LayoutModelBuilder layoutModelBuilder = builder();

    param.getParameter(DEFAULT_GROUP_NAME, PASSWORD).getValue().getValue()
        .filter(v -> (boolean) v)
        .ifPresent(value -> layoutModelBuilder.asPassword());

    param.getParameter(DEFAULT_GROUP_NAME, ORDER_ATTRIBUTE).getValue().getValue()
        .ifPresent(value -> layoutModelBuilder.order((int) value));
    param.getParameter(DEFAULT_GROUP_NAME, TAB_ATTRIBUTE).getValue().getValue()
        .ifPresent(value -> layoutModelBuilder.tabName((String) value));

    param.getParameter(DEFAULT_GROUP_NAME, TYPE_ATTRIBUTE).getValue().getValue()
        .ifPresent(receivedInputType -> {

          final DisplayModel displayModel = getDisplayModel(param);
          MetadataType parameterType = extractType((String) receivedInputType);

          ParameterDeclarer parameterDeclarer = getParameterDeclarer(parameterizedDeclarer, param);
          parameterDeclarer.describedAs(getDescription(param))
              .withLayout(layoutModelBuilder.build())
              .withDisplayModel(displayModel)
              .withRole(role)
              .ofType(parameterType);
        });
  }

  private DisplayModel getDisplayModel(ComponentAst componentModel) {
    final DisplayModel.DisplayModelBuilder displayModelBuilder = DisplayModel.builder();
    componentModel.getParameter(DEFAULT_GROUP_NAME, DISPLAY_NAME_ATTRIBUTE)
        .getValue().getValue().ifPresent(value -> displayModelBuilder.displayName((String) value));
    componentModel.getParameter(DEFAULT_GROUP_NAME, SUMMARY_ATTRIBUTE)
        .getValue().getValue().ifPresent(value -> displayModelBuilder.summary((String) value));
    componentModel.getParameter(DEFAULT_GROUP_NAME, EXAMPLE_ATTRIBUTE)
        .getValue().getValue().ifPresent(value -> displayModelBuilder.example((String) value));
    return displayModelBuilder.build();
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
   * @param param                 attributes to consume the values from
   * @return the {@link ParameterDeclarer}, being created as required or optional with a default value if applies.
   */
  private ParameterDeclarer getParameterDeclarer(ParameterizedDeclarer parameterizedDeclarer, ComponentAst param) {
    final String parameterName = param.getParameter(DEFAULT_GROUP_NAME, PARAMETER_NAME).getRawValue();
    final Optional<String> parameterDefaultValue = param.getParameter(DEFAULT_GROUP_NAME, PARAMETER_DEFAULT_VALUE).getValue()
        .mapLeft(expr -> "#[" + expr + "]")
        .getValue();
    final UseEnum use = UseEnum.valueOf(param.getParameter(DEFAULT_GROUP_NAME, ATTRIBUTE_USE).getValue().getRight().toString());
    if (UseEnum.REQUIRED.equals(use) && parameterDefaultValue.isPresent()) {
      throw new IllegalParameterModelDefinitionException(format("The parameter [%s] cannot have the %s attribute set to %s when it has a default value",
                                                                parameterName, ATTRIBUTE_USE, UseEnum.REQUIRED));
    }
    // Is required if either is marked as REQUIRED or it's marked as AUTO an doesn't have a default value
    boolean parameterRequired = UseEnum.REQUIRED.equals(use) || (UseEnum.AUTO.equals(use) && !parameterDefaultValue.isPresent());
    return parameterRequired ? parameterizedDeclarer.onDefaultParameterGroup().withRequiredParameter(parameterName)
        : parameterizedDeclarer.onDefaultParameterGroup().withOptionalParameter(parameterName)
            .defaultingTo(parameterDefaultValue.orElse(null));
  }

  private void extractOutputType(OutputDeclarer outputDeclarer, ComponentIdentifier componentIdentifier,
                                 ComponentAst operationModel, Optional<MetadataType> calculatedOutput) {
    Optional<ComponentAst> outputAttributesComponentModel = operationModel.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(componentIdentifier)).findFirst();
    outputAttributesComponentModel
        .ifPresent(outputComponentModel -> outputDeclarer.describedAs(getDescription(outputComponentModel)));

    MetadataType metadataType = getMetadataType(outputAttributesComponentModel, calculatedOutput);
    outputDeclarer.ofType(metadataType);
  }

  private MetadataType getMetadataType(Optional<ComponentAst> outputAttributesComponentModel,
                                       Optional<MetadataType> declarationMetadataType) {
    MetadataType metadataType;
    // the calculated metadata has precedence over the one configured in the xml
    if (declarationMetadataType.isPresent()) {
      metadataType = declarationMetadataType.get();
    } else {
      // if tye element is absent, it will default to the VOID type
      if (outputAttributesComponentModel.isPresent()) {
        String receivedOutputAttributeType =
            getStringParameter(outputAttributesComponentModel.get(), TYPE_ATTRIBUTE).orElse(null);
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

  private void declareErrorModels(Optional<ExtensionDeclarer> extensionDeclarer,
                                  OperationDeclarer operationDeclarer, XmlDslModel xmlDslModel, String operationName,
                                  ComponentAst operationModel) {
    Optional<ComponentAst> optionalErrorsComponentModel = operationModel.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(OPERATION_ERRORS_IDENTIFIER)).findAny();
    optionalErrorsComponentModel.ifPresent(componentModel -> componentModel.directChildrenStream()
        .filter(child -> child.getIdentifier().equals(OPERATION_ERROR_IDENTIFIER))
        .forEach(param -> {
          final String namespace = xmlDslModel.getPrefix().toUpperCase();
          final String errorType = (String) param.getParameter(DEFAULT_GROUP_NAME, ERROR_TYPE_ATTRIBUTE).getValue().getRight();

          if (errorType.contains(NAMESPACE_SEPARATOR)) {
            throw new IllegalModelDefinitionException(format("The operation [%s] cannot have an <error> [%s] that contains a reserved character [%s]",
                                                             operationName, errorType,
                                                             NAMESPACE_SEPARATOR));
          }
          final ErrorModel errorModel = ErrorModelBuilder.newError(errorType, namespace)
              .withParent(ErrorModelBuilder.newError(ANY).build())
              .build();
          operationDeclarer.withErrorModel(errorModel);
          extensionDeclarer.ifPresent(ext -> ext.withErrorModel(errorModel));
        }));
  }

}
