/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.disjunction;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_EE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_ROOT_ELEMENT;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SOURCE_TYPE;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.extension.api.util.NameUtils.pluralize;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.util.NameValidationUtil.verifyStringDoesNotContainsReservedCharacters;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.api.dsl.processor.ConfigFile;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.internal.dsl.model.ComponentLocationVisitor;
import org.mule.runtime.config.internal.dsl.model.ComponentModelReader;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.config.CompositeConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesComponent;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationProperty;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.FileConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.GlobalPropertyConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.MapConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.PropertiesResolverConfigurationProperties;
import org.mule.runtime.config.internal.dsl.model.config.RuntimeConfigurationException;
import org.mule.runtime.config.internal.dsl.model.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModulesModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.ClassUtils;
import org.w3c.dom.Node;

/**
 * An {@code ApplicationModel} holds a representation of all the artifact configuration using an abstract model to represent any
 * configuration option.
 * <p/>
 * This model is represented by a set of {@link ComponentModel}. Each {@code ComponentModel} holds a piece of configuration and
 * may have children {@code ComponentModel}s as defined in the artifact configuration.
 * <p/>
 * Once the set of {@code ComponentModel} gets created from the application
 * {@link org.mule.runtime.config.api.dsl.processor.ConfigFile}s the {@code ApplicationModel} executes a set of common validations
 * dictated by the configuration semantics.
 *
 * @since 4.0
 */
public class ApplicationModel {

  // TODO MULE-9692 move this logic elsewhere. This are here just for the language rules and those should be processed elsewhere.
  public static final String POLICY_ROOT_ELEMENT = "policy";
  public static final String ERROR_MAPPING = "error-mapping";
  public static final String MAX_REDELIVERY_ATTEMPTS_ROLLBACK_ES_ATTRIBUTE = "maxRedeliveryAttempts";
  public static final String WHEN_CHOICE_ES_ATTRIBUTE = "when";
  public static final String TYPE_ES_ATTRIBUTE = "type";
  public static final String EXCEPTION_STRATEGY_REFERENCE_ELEMENT = "exception-strategy";
  public static final String PROPERTY_ELEMENT = "property";
  public static final String NAME_ATTRIBUTE = "name";
  public static final String REFERENCE_ATTRIBUTE = "ref";
  public static final String VALUE_ATTRIBUTE = "value";
  public static final String TRANSFORMER_REFERENCE_ELEMENT = "transformer";
  public static final String ANNOTATION_ELEMENT = "annotations";
  public static final String DATA_WEAVE = "weave";
  public static final String CUSTOM_TRANSFORMER = "custom-transformer";
  public static final String DESCRIPTION_ELEMENT = "description";
  public static final String PROPERTIES_ELEMENT = "properties";
  private static final String MODULE_OPERATION_CHAIN_ELEMENT = "module-operation-chain";

  public static final String REDELIVERY_POLICY_ELEMENT = "redelivery-policy";
  // TODO MULE-9638 Remove once all bean definitions parsers where migrated
  public static final String TEST_NAMESPACE = "test";
  public static final String DOC_NAMESPACE = "doc";
  public static final String SPRING_SECURITY_NAMESPACE = "ss";
  public static final String MULE_SECURITY_NAMESPACE = "mule-ss";
  public static final String MULE_XML_NAMESPACE = "mulexml";
  public static final String PGP_NAMESPACE = "pgp";
  public static final String XSL_NAMESPACE = "xsl";
  public static final String TRANSPORT_NAMESPACE = "transports";
  public static final String JMS_NAMESPACE = "jms";
  public static final String VM_NAMESPACE = "vm";
  public static final String HTTP_TRANSPORT_NAMESPACE = "http-transport";
  public static final String BATCH_NAMESPACE = "batch";
  public static final String PARSER_TEST_NAMESPACE = "parsers-test";
  public static final String GLOBAL_PROPERTY = "global-property";
  public static final String SECURITY_MANAGER = "security-manager";
  public static final String CONFIGURATION_PROPERTIES_ELEMENT = "configuration-properties";
  public static final String OBJECT_ELEMENT = "object";


  public static final ComponentIdentifier EXCEPTION_STRATEGY_REFERENCE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(EXCEPTION_STRATEGY_REFERENCE_ELEMENT)
          .build();
  public static final ComponentIdentifier ERROR_MAPPING_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ERROR_MAPPING).build();
  public static final ComponentIdentifier MULE_PROPERTY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(PROPERTY_ELEMENT).build();
  public static final ComponentIdentifier MULE_PROPERTIES_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(PROPERTIES_ELEMENT).build();
  public static final ComponentIdentifier ANNOTATIONS_ELEMENT_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ANNOTATION_ELEMENT).build();
  public static final ComponentIdentifier TRANSFORMER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(TRANSFORMER_REFERENCE_ELEMENT).build();
  public static final ComponentIdentifier CUSTOM_TRANSFORMER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(CUSTOM_TRANSFORMER).build();
  public static final ComponentIdentifier DOC_DESCRIPTION_IDENTIFIER =
      builder().namespace(DOC_NAMESPACE).name(DESCRIPTION_ELEMENT).build();
  public static final ComponentIdentifier DESCRIPTION_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(DESCRIPTION_ELEMENT).build();
  public static final ComponentIdentifier OBJECT_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(OBJECT_ELEMENT).build();
  public static final ComponentIdentifier REDELIVERY_POLICY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(REDELIVERY_POLICY_ELEMENT).build();
  public static final ComponentIdentifier GLOBAL_PROPERTY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(GLOBAL_PROPERTY).build();
  public static final ComponentIdentifier SECURITY_MANAGER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(SECURITY_MANAGER).build();
  public static final ComponentIdentifier CONFIGURATION_PROPERTIES =
      builder().namespace(CORE_PREFIX).name(CONFIGURATION_PROPERTIES_ELEMENT).build();
  public static final ComponentIdentifier MODULE_OPERATION_CHAIN =
      builder().namespace(CORE_PREFIX).name(MODULE_OPERATION_CHAIN_ELEMENT).build();


  // TODO MULE-13042 - remove this constants and their usages one this code gets migrated to use extension models.
  public static final String MUNIT_PREFIX = "munit";
  public static final ComponentIdentifier MUNIT_TEST_IDENTIFIER =
      builder().namespace(MUNIT_PREFIX).name("test").build();
  public static final ComponentIdentifier MUNIT_BEFORE_TEST_IDENTIFIER =
      builder().namespace(MUNIT_PREFIX).name("before-test").build();
  public static final ComponentIdentifier MUNIT_BEFORE_SUITE_IDENTIFIER =
      builder().namespace(MUNIT_PREFIX).name("before-suite").build();
  public static final ComponentIdentifier MUNIT_AFTER_TEST_IDENTIFIER =
      builder().namespace(MUNIT_PREFIX).name("after-test").build();
  public static final ComponentIdentifier MUNIT_AFTER_SUITE_IDENTIFIER =
      builder().namespace(MUNIT_PREFIX).name("after-suite").build();

  public static final String HTTP_POLICY = "http-policy";
  public static final ComponentIdentifier HTTP_PROXY_SOURCE_POLICY_IDENTIFIER =
      builder().namespace(HTTP_POLICY).name("source").build();
  public static final ComponentIdentifier HTTP_PROXY_OPERATION_IDENTIFIER =
      builder().namespace(HTTP_POLICY).name("operation").build();
  public static final ComponentIdentifier HTTP_PROXY_POLICY_IDENTIFIER =
      builder().namespace(HTTP_POLICY).name("proxy").build();

  public static final String CLASS_ATTRIBUTE = "class";

  private static ImmutableSet<ComponentIdentifier> ignoredNameValidationComponentList =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("flow-ref").build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("alias").build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("password-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("custom-security-provider")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("custom-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT)
              .name("secret-key-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("import").build())
          .add(builder().namespace(MULE_ROOT_ELEMENT)
              .name("string-to-byte-array-transformer")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("append-string-transformer")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("security-manager").build())
          .add(builder().namespace(TEST_NAMESPACE).name("queue").build())
          .add(builder().namespace(TEST_NAMESPACE).name("invocation-counter").build())
          .add(builder().namespace(SPRING_SECURITY_NAMESPACE).name("user").build())
          .add(builder().namespace(MULE_SECURITY_NAMESPACE)
              .name("delegate-security-provider")
              .build())
          .add(builder().namespace(MULE_SECURITY_NAMESPACE).name("security-manager")
              .build())
          .add(builder().namespace(MULE_XML_NAMESPACE).name("xslt-transformer").build())
          .add(builder().namespace(MULE_XML_NAMESPACE).name("alias").build())
          .add(builder().namespace(PGP_NAMESPACE).name("security-provider").build())
          .add(builder().namespace(PGP_NAMESPACE).name("keybased-encryption-strategy")
              .build())
          .add(builder().namespace(XSL_NAMESPACE).name("param").build())
          .add(builder().namespace(XSL_NAMESPACE).name("attribute").build())
          .add(builder().namespace(XSL_NAMESPACE).name("element").build())
          .add(builder().namespace(TRANSPORT_NAMESPACE).name("inbound-endpoint").build())
          .add(builder().namespace(TRANSPORT_NAMESPACE).name("outbound-endpoint").build())
          .add(builder().namespace(JMS_NAMESPACE).name("inbound-endpoint").build())
          .add(builder().namespace(VM_NAMESPACE).name("inbound-endpoint").build())
          .add(builder().namespace(HTTP_TRANSPORT_NAMESPACE).name("inbound-endpoint").build())
          .add(builder().namespace(HTTP_TRANSPORT_NAMESPACE).name("set-cookie").build())
          .add(builder().namespace(HTTP_TRANSPORT_NAMESPACE).name("header").build())
          .add(builder().namespace(HTTP_TRANSPORT_NAMESPACE)
              .name("http-response-to-object-transformer")
              .build())
          .add(builder().namespace(HTTP_TRANSPORT_NAMESPACE)
              .name("http-response-to-string-transformer")
              .build())
          .add(builder().namespace(HTTP_TRANSPORT_NAMESPACE)
              .name("message-to-http-response-transformer")
              .build())
          .add(builder().namespace(HTTP_TRANSPORT_NAMESPACE)
              .name("object-to-http-request-transformer")
              .build())
          .add(builder().namespace(BATCH_NAMESPACE).name("step").build())
          .add(builder().namespace(BATCH_NAMESPACE).name("execute").build())
          .add(builder().namespace(PARSER_TEST_NAMESPACE).name("child").build())
          .add(builder().namespace(PARSER_TEST_NAMESPACE).name("kid").build())
          .add(builder().namespace(DATA_WEAVE).name("reader-property").build())
          .build();

  private final Optional<ComponentBuildingDefinitionRegistry> componentBuildingDefinitionRegistry;
  private List<ComponentModel> muleComponentModels = new LinkedList<>();
  private PropertiesResolverConfigurationProperties configurationProperties;
  private ResourceProvider externalResourceProvider;

  /**
   * Creates an {code ApplicationModel} from a {@link ArtifactConfig}.
   * <p/>
   * A set of validations are applied that may make creation fail.
   *
   * @param artifactConfig the mule artifact configuration content.
   * @param artifactDeclaration an {@link ArtifactDeclaration}
   * @throws Exception when the application configuration has semantic errors.
   */
  public ApplicationModel(ArtifactConfig artifactConfig, ArtifactDeclaration artifactDeclaration,
                          ResourceProvider externalResourceProvider)
      throws Exception {
    this(artifactConfig, artifactDeclaration, emptySet(), emptyMap(), empty(), of(new ComponentBuildingDefinitionRegistry()),
         true, externalResourceProvider);
  }

  /**
   * Creates an {code ApplicationModel} from a {@link ArtifactConfig}.
   * <p/>
   * A set of validations are applied that may make creation fail.
   *
   * @param artifactConfig the mule artifact configuration content.
   * @param artifactDeclaration an {@link ArtifactDeclaration}
   * @param extensionModels Set of {@link ExtensionModel extensionModels} that will be used to type componentModels
   * @param parentConfigurationProperties the {@link ConfigurationProperties} of the parent artifact. For instance, application
   *        will receive the domain resolver.
   * @param componentBuildingDefinitionRegistry an optional {@link ComponentBuildingDefinitionRegistry} used to correlate items in
   *        this model to their definitions
   * @param runtimeMode true implies the mule application should behave as a runtime app (e.g.: smart connectors will be macro
   *        expanded) false implies the mule is being created from a tooling perspective.
   * @param externalResourceProvider the provider for configuration properties files and ${file::name.txt} placeholders
   * @throws Exception when the application configuration has semantic errors.
   */
  // TODO: MULE-9638 remove this optional
  public ApplicationModel(ArtifactConfig artifactConfig, ArtifactDeclaration artifactDeclaration,
                          Set<ExtensionModel> extensionModels,
                          Map<String, String> deploymentProperties,
                          Optional<ConfigurationProperties> parentConfigurationProperties,
                          Optional<ComponentBuildingDefinitionRegistry> componentBuildingDefinitionRegistry,
                          boolean runtimeMode, ResourceProvider externalResourceProvider)
      throws Exception {

    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
    this.externalResourceProvider = externalResourceProvider;
    createConfigurationAttributeResolver(artifactConfig, parentConfigurationProperties, deploymentProperties);
    convertConfigFileToComponentModel(artifactConfig);
    convertArtifactDeclarationToComponentModel(extensionModels, artifactDeclaration);
    resolveRegistrationNames();
    validateModel(componentBuildingDefinitionRegistry);
    createEffectiveModel();
    if (runtimeMode) {
      expandModules(extensionModels);
    }
    resolveComponentTypes();
    executeOnEveryMuleComponentTree(new ComponentLocationVisitor(new ExtensionModelHelper(extensionModels)));
  }

  private void createConfigurationAttributeResolver(ArtifactConfig artifactConfig,
                                                    Optional<ConfigurationProperties> parentConfigurationProperties,
                                                    Map<String, String> deploymentProperties) {

    SystemPropertiesConfigurationProvider systemPropertiesConfigurationProvider = new SystemPropertiesConfigurationProvider();
    ConfigurationPropertiesProvider globalPropertiesConfigurationAttributeProvider =
        createProviderFromGlobalProperties(artifactConfig);
    DefaultConfigurationPropertiesResolver localResolver =
        new DefaultConfigurationPropertiesResolver(of(new DefaultConfigurationPropertiesResolver(empty(),
                                                                                                 globalPropertiesConfigurationAttributeProvider)),
                                                   systemPropertiesConfigurationProvider);
    List<ConfigurationPropertiesProvider> configConfigurationPropertiesProviders =
        getConfigurationPropertiesProvidersFromComponents(artifactConfig, localResolver);
    FileConfigurationPropertiesProvider externalPropertiesConfigurationProvider =
        new FileConfigurationPropertiesProvider(externalResourceProvider, "External files");

    Optional<ConfigurationPropertiesResolver> parentConfigurationPropertiesResolver = of(localResolver);
    if (parentConfigurationProperties.isPresent()) {
      parentConfigurationPropertiesResolver =
          of(new DefaultConfigurationPropertiesResolver(empty(), new ConfigurationPropertiesProvider() {

            @Override
            public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
              return parentConfigurationProperties.get().resolveProperty(configurationAttributeKey)
                  .map(value -> new ConfigurationProperty(parentConfigurationProperties, configurationAttributeKey, value));
            }

            @Override
            public String getDescription() {
              return "Domain properties";
            }
          }));
    }

    if (!configConfigurationPropertiesProviders.isEmpty()) {
      CompositeConfigurationPropertiesProvider configurationAttributesProvider =
          new CompositeConfigurationPropertiesProvider(configConfigurationPropertiesProviders);
      parentConfigurationPropertiesResolver = of(new DefaultConfigurationPropertiesResolver(parentConfigurationPropertiesResolver,
                                                                                            configurationAttributesProvider));

    }
    DefaultConfigurationPropertiesResolver globalPropertiesConfigurationPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(parentConfigurationPropertiesResolver,
                                                   globalPropertiesConfigurationAttributeProvider);
    DefaultConfigurationPropertiesResolver systemPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(of(globalPropertiesConfigurationPropertiesResolver),
                                                   systemPropertiesConfigurationProvider);
    DefaultConfigurationPropertiesResolver externalPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(of(systemPropertiesResolver),
                                                   externalPropertiesConfigurationProvider);
    if (deploymentProperties.isEmpty()) {
      this.configurationProperties = new PropertiesResolverConfigurationProperties(externalPropertiesResolver);
    } else {
      this.configurationProperties =
          new PropertiesResolverConfigurationProperties(new DefaultConfigurationPropertiesResolver(of(externalPropertiesResolver),
                                                                                                   new MapConfigurationPropertiesProvider(deploymentProperties,
                                                                                                                                          "Deployment properties")));
    }
  }

  private List<ConfigurationPropertiesProvider> getConfigurationPropertiesProvidersFromComponents(ArtifactConfig artifactConfig,
                                                                                                  DefaultConfigurationPropertiesResolver localResolver) {
    List<ConfigurationPropertiesProvider> configConfigurationPropertiesProviders = new ArrayList<>();
    artifactConfig.getConfigFiles().stream()
        .forEach(configFile -> configFile.getConfigLines().stream()
            .forEach(configLine -> {
              for (ConfigLine componentConfigLine : configLine.getChildren()) {
                if (componentConfigLine.getNamespace() != null && componentConfigLine.getNamespace().equals(CORE_PREFIX)
                    && componentConfigLine.getIdentifier().equals(CONFIGURATION_PROPERTIES_ELEMENT)) {
                  String fileLocation = componentConfigLine.getConfigAttributes().get("file").getValue();
                  ConfigurationPropertiesComponent propertiesComponent =
                      new ConfigurationPropertiesComponent(localResolver.resolveValue(fileLocation).toString(),
                                                           externalResourceProvider);
                  DefaultComponentLocation.DefaultLocationPart locationPart =
                      new DefaultComponentLocation.DefaultLocationPart(CONFIGURATION_PROPERTIES_ELEMENT,
                                                                       of(TypedComponentIdentifier.builder()
                                                                           .type(UNKNOWN)
                                                                           .identifier(CONFIGURATION_PROPERTIES)
                                                                           .build()),
                                                                       of(configFile.getFilename()),
                                                                       of(configLine.getLineNumber()));
                  propertiesComponent.setAnnotations(ImmutableMap
                      .<QName, Object>builder().put(AbstractComponent.LOCATION_KEY,
                                                    new DefaultComponentLocation(of(CONFIGURATION_PROPERTIES_ELEMENT),
                                                                                 singletonList(locationPart)))
                      .build());
                  configConfigurationPropertiesProviders.add(propertiesComponent);
                  try {
                    propertiesComponent.initialise();
                  } catch (InitialisationException e) {
                    throw new MuleRuntimeException(e);
                  }
                }
              }
            }));
    return configConfigurationPropertiesProviders;
  }

  /**
   * Resolves the types of each component model when possible.
   */
  public void resolveComponentTypes() {
    checkState(componentBuildingDefinitionRegistry.isPresent(),
               "ApplicationModel was created without a " + ComponentBuildingDefinitionProvider.class.getName());
    executeOnEveryComponentTree(componentModel -> {
      Optional<ComponentBuildingDefinition<?>> buildingDefinition =
          componentBuildingDefinitionRegistry.get().getBuildingDefinition(componentModel.getIdentifier());
      buildingDefinition.map(definition -> {
        ObjectTypeVisitor typeDefinitionVisitor = new ObjectTypeVisitor(componentModel);
        definition.getTypeDefinition().visit(typeDefinitionVisitor);
        componentModel.setType(typeDefinitionVisitor.getType());
        return definition;
      }).orElseGet(() -> {
        String classParameter = componentModel.getParameters().get(CLASS_ATTRIBUTE);
        if (classParameter != null) {
          try {
            componentModel.setType(ClassUtils.getClass(classParameter));
          } catch (ClassNotFoundException e) {
            throw new RuntimeConfigurationException(I18nMessageFactory.createStaticMessage(String
                .format("Could not resolve class '%s' for component '%s'", classParameter,
                        componentModel.getComponentLocation())));
          }
        }
        return null;
      });
    });
  }

  /**
   * Creates the effective application model to be used to generate the runtime objects of the mule configuration.
   */
  private void createEffectiveModel() {
    processSourcesRedeliveryPolicy();
  }

  /**
   * Process from any message source the redelivery-policy to make it part of the final pipeline.
   */
  private void processSourcesRedeliveryPolicy() {
    executeOnEveryFlow(flowComponentModel -> {
      if (!flowComponentModel.getInnerComponents().isEmpty()) {
        ComponentModel possibleSourceComponent = flowComponentModel.getInnerComponents().get(0);
        possibleSourceComponent.getInnerComponents().stream()
            .filter(childComponent -> childComponent.getIdentifier().equals(REDELIVERY_POLICY_IDENTIFIER))
            .findAny()
            .ifPresent(redeliveryPolicyComponentModel -> {
              possibleSourceComponent.getInnerComponents().remove(redeliveryPolicyComponentModel);
              flowComponentModel.getInnerComponents().add(1, redeliveryPolicyComponentModel);
            });
      }
    });
  }

  private void convertArtifactDeclarationToComponentModel(Set<ExtensionModel> extensionModels,
                                                          ArtifactDeclaration artifactDeclaration) {
    if (artifactDeclaration != null && !extensionModels.isEmpty()) {
      DslElementModelFactory elementFactory = DslElementModelFactory
          .getDefault(DslResolvingContext.getDefault(extensionModels));

      ComponentModel rootComponent = new ComponentModel.Builder()
          .setIdentifier(ComponentIdentifier.builder()
              .namespace(CORE_PREFIX)
              .name(CORE_PREFIX)
              .build())
          .build();

      AtomicBoolean atLeastOneComponentAdded = new AtomicBoolean(false);

      artifactDeclaration.getGlobalElements().stream()
          .map(e -> elementFactory.create((ElementDeclaration) e))
          .filter(Optional::isPresent)
          .map(e -> e.get().getConfiguration())
          .forEach(config -> config
              .ifPresent(c -> {
                atLeastOneComponentAdded.set(true);
                ComponentModel componentModel = convertComponentConfiguration(c, true);
                componentModel.setParent(rootComponent);
                rootComponent.getInnerComponents().add(componentModel);
              }));

      if (atLeastOneComponentAdded.get()) {
        this.muleComponentModels.add(rootComponent);
      }
    }
  }

  private ComponentModel convertComponentConfiguration(ComponentConfiguration componentConfiguration, boolean isRoot) {
    ComponentModel.Builder builder = new ComponentModel.Builder()
        .setIdentifier(componentConfiguration.getIdentifier());
    if (isRoot) {
      builder.markAsRootComponent();
    }
    for (Map.Entry<String, String> parameter : componentConfiguration.getParameters().entrySet()) {
      builder.addParameter(parameter.getKey(), parameter.getValue(), false);
    }
    for (ComponentConfiguration childComponentConfiguration : componentConfiguration.getNestedComponents()) {
      builder.addChildComponentModel(convertComponentConfiguration(childComponentConfiguration, false));
    }

    componentConfiguration.getValue().ifPresent(builder::setTextContent);

    ComponentModel componentModel = builder.build();
    for (ComponentModel childComponent : componentModel.getInnerComponents()) {
      childComponent.setParent(componentModel);
    }
    return componentModel;
  }


  private ConfigurationPropertiesProvider createProviderFromGlobalProperties(ArtifactConfig artifactConfig) {
    final Map<String, ConfigurationProperty> globalProperties = new HashMap<>();

    artifactConfig.getConfigFiles().stream().forEach(configFile -> {
      configFile.getConfigLines().get(0).getChildren().stream().forEach(configLine -> {
        if (GLOBAL_PROPERTY.equals(configLine.getIdentifier())) {
          String key = configLine.getConfigAttributes().get("name").getValue();
          String rawValue = configLine.getConfigAttributes().get("value").getValue();
          globalProperties.put(key,
                               new ConfigurationProperty(format("global-property - file: %s - lineNumber %s",
                                                                configFile.getFilename(), configLine.getLineNumber()),
                                                         key, rawValue));
        }
      });
    });
    return new GlobalPropertyConfigurationPropertiesProvider(globalProperties);
  }

  /**
   * @param element element which was the source of the {@code ComponentModel}.
   * @return the {@code ComponentModel} created from the element.
   */
  // TODO MULE-9638: remove once the old parsing mechanism is not needed anymore
  public ComponentModel findComponentDefinitionModel(Node element) {
    return innerFindComponentDefinitionModel(element, muleComponentModels);
  }

  public Optional<ComponentModel> findComponentDefinitionModel(ComponentIdentifier componentIdentifier) {
    if (muleComponentModels.isEmpty()) {
      return empty();
    }
    return muleComponentModels.get(0).getInnerComponents().stream().filter(ComponentModel::isRoot)
        .filter(componentModel -> componentModel.getIdentifier().equals(componentIdentifier)).findFirst();
  }

  private void convertConfigFileToComponentModel(ArtifactConfig artifactConfig) {
    List<ConfigFile> configFiles = artifactConfig.getConfigFiles();
    ComponentModelReader componentModelReader =
        new ComponentModelReader(configurationProperties.getConfigurationPropertiesResolver());
    configFiles.stream().forEach(configFile -> {
      ComponentModel componentModel =
          componentModelReader.extractComponentDefinitionModel(configFile.getConfigLines().get(0), configFile.getFilename());
      if (muleComponentModels.isEmpty()) {
        muleComponentModels.add(componentModel);
      } else {
        // Only one componentModel as Root should be set, therefore componentModel is merged
        final ComponentModel rootComponentModel = muleComponentModels.get(0);
        muleComponentModels.set(0, new ComponentModel.Builder(rootComponentModel).merge(componentModel).build());
      }
    });

  }

  private void validateModel(Optional<ComponentBuildingDefinitionRegistry> componentBuildingDefinitionRegistry)
      throws ConfigurationException {
    if (muleComponentModels.isEmpty() || !isMuleConfigurationFile()) {
      return;
    }
    // TODO MULE-9692 all this validations will be moved to an entity that does the validation and allows to aggregate all
    // validations instead of failing fast.
    validateNameIsNotRepeated();
    validateNameHasValidCharacters();
    validateErrorMappings();
    validateExceptionStrategyWhenAttributeIsOnlyPresentInsideChoice();
    validateErrorHandlerStructure();
    validateParameterAndChildForSameAttributeAreNotDefinedTogether();
    if (componentBuildingDefinitionRegistry.isPresent()) {
      validateNamedTopLevelElementsHaveName(componentBuildingDefinitionRegistry.get());
    }
    validateSingleElementsExistence();
  }

  private void validateParameterAndChildForSameAttributeAreNotDefinedTogether() {
    executeOnEveryMuleComponentTree(componentModel -> {
      for (String parameterName : componentModel.getParameters().keySet()) {
        if (!componentModel.isParameterValueProvidedBySchema(parameterName)) {
          String mapChildName = hyphenize(pluralize(parameterName));
          String listOrPojoChildName = hyphenize(parameterName);
          Optional<ComponentModel> childOptional =
              findRelatedChildForParameter(componentModel.getInnerComponents(), mapChildName, listOrPojoChildName);
          if (childOptional.isPresent()) {
            throw new MuleRuntimeException(createStaticMessage(
                                                               format("Component %s has a child element %s which is used for the same purpose of the configuration parameter %s. "
                                                                   + "Only one must be used.", componentModel.getIdentifier(),
                                                                      childOptional.get().getIdentifier(),
                                                                      parameterName)));
          }
        }
      }
    });
  }

  private Optional<ComponentModel> findRelatedChildForParameter(List<ComponentModel> chilrenComponents, String... possibleNames) {
    Set<String> possibleNamesSet = new HashSet<>(asList(possibleNames));
    for (ComponentModel childrenComponent : chilrenComponents) {
      if (possibleNamesSet.contains(childrenComponent.getIdentifier().getName())) {
        return of(childrenComponent);
      }
    }
    return empty();
  }

  private void validateNameIsNotRepeated() {
    Map<String, ComponentModel> existingObjectsWithName = new HashMap<>();
    executeOnEveryMuleComponentTree(componentModel -> {
      String nameAttributeValue = componentModel.getNameAttribute();
      if (nameAttributeValue != null && !ignoredNameValidationComponentList.contains(componentModel.getIdentifier())) {
        if (existingObjectsWithName.containsKey(nameAttributeValue)) {
          throw new MuleRuntimeException(createStaticMessage(
                                                             "Two configuration elements have been defined with the same global name. Global name [%s] must be unique. Clashing components are %s and %s",
                                                             nameAttributeValue,
                                                             existingObjectsWithName.get(nameAttributeValue).getIdentifier(),
                                                             componentModel.getIdentifier()));
        }
        existingObjectsWithName.put(nameAttributeValue, componentModel);
      }
    });
  }

  private void validateNameHasValidCharacters() {
    executeOnEveryRootElement(componentModel -> {
      String nameAttributeValue = componentModel.getNameAttribute();
      if (nameAttributeValue != null) {
        verifyStringDoesNotContainsReservedCharacters(nameAttributeValue);
      }
    });
  }

  private boolean isMuleConfigurationFile() {
    final ComponentIdentifier rootIdentifier = muleComponentModels.get(0).getIdentifier();
    return rootIdentifier.equals(MULE_IDENTIFIER)
        || rootIdentifier.equals(MULE_DOMAIN_IDENTIFIER)
        || rootIdentifier.equals(MULE_EE_DOMAIN_IDENTIFIER);
  }

  private void validateErrorMappings() {
    executeOnEveryComponentTree(componentModel -> {
      List<ComponentModel> errorMappings = componentModel.getInnerComponents().stream()
          .filter(c -> c.getIdentifier().equals(ERROR_MAPPING_IDENTIFIER)).collect(toList());
      if (!errorMappings.isEmpty()) {
        List<ComponentModel> anyMappings = errorMappings.stream().filter(this::isErrorMappingWithSourceAny).collect(toList());
        if (anyMappings.size() > 1) {
          throw new MuleRuntimeException(createStaticMessage("Only one mapping for 'ANY' or an empty source type is allowed."));
        } else if (anyMappings.size() == 1 && !isErrorMappingWithSourceAny(errorMappings.get(errorMappings.size() - 1))) {
          throw new MuleRuntimeException(createStaticMessage("Only the last error mapping can have 'ANY' or an empty source type."));
        }
        List<String> sources = errorMappings.stream().map(model -> model.getParameters().get(SOURCE_TYPE)).collect(toList());
        List<String> distinctSources = sources.stream().distinct().collect(toList());
        if (sources.size() != distinctSources.size()) {
          throw new MuleRuntimeException(createStaticMessage(format("Repeated source types are not allowed. Offending types are '%s'.",
                                                                    on("', '").join(disjunction(sources, distinctSources)))));
        }
      }
    });
  }

  private boolean isErrorMappingWithSourceAny(ComponentModel model) {
    String sourceType = model.getParameters().get(SOURCE_TYPE);
    return sourceType == null || sourceType.equals(ANY_IDENTIFIER);
  }

  private void validateErrorHandlerStructure() {
    executeOnEveryMuleComponentTree(component -> {
      if (component.getIdentifier().equals(ERROR_HANDLER_IDENTIFIER)) {
        validateExceptionStrategiesHaveWhenAttribute(component);
        validateNoMoreThanOneRollbackExceptionStrategyWithRedelivery(component);
      }
    });
  }

  private void validateNoMoreThanOneRollbackExceptionStrategyWithRedelivery(ComponentModel component) {
    if (component.getInnerComponents().stream().filter(exceptionStrategyComponent -> {
      return exceptionStrategyComponent.getParameters().get(MAX_REDELIVERY_ATTEMPTS_ROLLBACK_ES_ATTRIBUTE) != null;
    }).count() > 1) {
      throw new MuleRuntimeException(createStaticMessage(
                                                         "Only one on-error-propagate within a error-handler can handle message redelivery. Remove one of the maxRedeliveryAttempts attributes"));
    }
  }

  private void validateExceptionStrategiesHaveWhenAttribute(ComponentModel component) {
    List<ComponentModel> innerComponents = component.getInnerComponents();
    for (int i = 0; i < innerComponents.size() - 1; i++) {
      Map<String, String> parameters = innerComponents.get(i).getParameters();
      if (parameters.get(WHEN_CHOICE_ES_ATTRIBUTE) == null && parameters.get(TYPE_ES_ATTRIBUTE) == null) {
        throw new MuleRuntimeException(createStaticMessage(
                                                           "Every handler (except for the last one) within an 'error-handler' must specify a 'when' or 'type' attribute."));
      }
    }
  }

  private void validateExceptionStrategyWhenAttributeIsOnlyPresentInsideChoice() {
    executeOnEveryMuleComponentTree(component -> {
      if (component.getIdentifier().getName().endsWith(EXCEPTION_STRATEGY_REFERENCE_ELEMENT)) {
        Node componentNode = XmlCustomAttributeHandler.from(component).getNode();
        if (component.getParameters().get(WHEN_CHOICE_ES_ATTRIBUTE) != null
            && !componentNode.getParentNode().getLocalName().equals(ERROR_HANDLER)
            && !componentNode.getParentNode().getLocalName().equals(MULE_ROOT_ELEMENT)) {
          throw new MuleRuntimeException(
                                         createStaticMessage("Only handlers within an error-handler can have when attribute specified"));
        }
      }
    });
  }

  private void validateNamedTopLevelElementsHaveName(ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry)
      throws ConfigurationException {
    try {
      List<ComponentModel> topLevelComponents = muleComponentModels.get(0).getInnerComponents();
      topLevelComponents.stream().forEach(topLevelComponent -> {
        final ComponentIdentifier identifier = topLevelComponent.getIdentifier();
        componentBuildingDefinitionRegistry.getBuildingDefinition(identifier).filter(ComponentBuildingDefinition::isNamed)
            .ifPresent(buildingDefinition -> {
              if (isBlank(topLevelComponent.getNameAttribute())) {
                throw new MuleRuntimeException(createStaticMessage(format("Global element %s:%s does not provide a name attribute.",
                                                                          identifier.getNamespace(), identifier.getName())));
              }
            });
      });
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }

  public void executeOnEveryComponentTree(final Consumer<ComponentModel> task) {
    for (ComponentModel componentModel : muleComponentModels) {
      executeOnComponentTree(componentModel, task);
    }
  }

  public void executeOnEveryMuleComponentTree(final Consumer<ComponentModel> task) {
    for (ComponentModel componentModel : muleComponentModels) {
      executeOnComponentTree(componentModel, task);
    }
  }


  public void executeOnEveryRootElement(final Consumer<ComponentModel> task) {
    for (ComponentModel muleComponentModel : muleComponentModels) {
      for (ComponentModel componentModel : muleComponentModel.getInnerComponents()) {
        task.accept(componentModel);
      }
    }
  }

  public void executeOnEveryFlow(final Consumer<ComponentModel> task) {
    executeOnEveryRootElement(componentModel -> {
      if (FLOW_IDENTIFIER.equals(componentModel.getIdentifier())) {
        task.accept(componentModel);
      }
    });
  }

  private void executeOnComponentTree(final ComponentModel component, final Consumer<ComponentModel> task)
      throws MuleRuntimeException {
    task.accept(component);
    component.getInnerComponents().forEach((innerComponent) -> {
      executeOnComponentTree(innerComponent, task);
    });
  }

  private ComponentModel innerFindComponentDefinitionModel(Node element, List<ComponentModel> componentModels) {
    for (ComponentModel componentModel : componentModels) {
      if (XmlCustomAttributeHandler.from(componentModel).getNode().equals(element)) {
        return componentModel;
      }
      ComponentModel childComponentModel = innerFindComponentDefinitionModel(element, componentModel.getInnerComponents());
      if (childComponentModel != null) {
        return childComponentModel;
      }
    }
    return null;
  }

  private void validateSingleElementsExistence() {
    validateSingleElementExistence(MUNIT_AFTER_SUITE_IDENTIFIER);
    validateSingleElementExistence(MUNIT_AFTER_SUITE_IDENTIFIER);
    validateSingleElementExistence(MUNIT_BEFORE_TEST_IDENTIFIER);
    validateSingleElementExistence(MUNIT_AFTER_TEST_IDENTIFIER);
  }

  private void validateSingleElementExistence(ComponentIdentifier componentIdentifier) {
    Map<String, Map<ComponentIdentifier, ComponentModel>> existingComponentsPerFile = new HashMap<>();

    executeOnEveryMuleComponentTree(componentModel -> {
      String configFileName = componentModel.getConfigFileName().get();
      ComponentIdentifier identifier = componentModel.getIdentifier();

      if (componentIdentifier.getNamespace().equals(identifier.getNamespace())
          && componentIdentifier.getName().equals(identifier.getName())) {

        if (existingComponentsPerFile.containsKey(configFileName)
            && existingComponentsPerFile.get(configFileName).containsKey(identifier)) {
          throw new MuleRuntimeException(createStaticMessage(
                                                             "Two configuration elements %s have been defined. Element [%s] must be unique. Clashing components are %s and %s",
                                                             identifier.getNamespace() + ":" + identifier.getName(),
                                                             identifier.getNamespace() + ":" + identifier.getName(),
                                                             componentModel.getNameAttribute(),
                                                             existingComponentsPerFile.get(configFileName).get(identifier)
                                                                 .getNameAttribute()));
        }
        Map<ComponentIdentifier, ComponentModel> existingComponentWithName = new HashMap<>();
        existingComponentWithName.put(identifier, componentModel);
        existingComponentsPerFile.put(configFileName, existingComponentWithName);
      }

    });
  }

  /**
   * TODO MULE-9688: When the model it's made immutable we will also provide the parent component for navigation and this will not
   * be needed anymore.
   *
   * @return the root component model
   */
  public ComponentModel getRootComponentModel() {
    return muleComponentModels.get(0);
  }

  /**
   * Find a named component configuration.
   *
   * @param name the expected value for the name attribute configuration.
   * @return the component if present, if not, an empty {@link Optional}
   */
  public Optional<ComponentModel> findTopLevelNamedComponent(String name) {
    Optional<ComponentModel> requestedComponentModelOptional = empty();
    for (ComponentModel muleComponentModel : muleComponentModels) {
      requestedComponentModelOptional = muleComponentModel.getInnerComponents().stream()
          .filter(componentModel -> name.equals(componentModel.getNameAttribute()))
          .findAny();
      if (requestedComponentModelOptional.isPresent()) {
        break;
      }
    }
    return requestedComponentModelOptional;
  }

  /**
   * Find a named component configuration.
   *
   * @param name the expected value for the name attribute configuration.
   * @return the component if present, if not, an empty {@link Optional}
   */
  // TODO MULE-11355: Make the ComponentModel haven an ComponentConfiguration internally
  public Optional<ComponentConfiguration> findTopLevelNamedElement(String name) {
    Optional<ComponentConfiguration> requestedElement = empty();
    for (ComponentModel muleComponentModel : muleComponentModels) {
      requestedElement = muleComponentModel.getInnerComponents().stream()
          .filter(componentModel -> name.equals(componentModel.getNameAttribute()))
          .map(ComponentModel::getConfiguration)
          .findAny();
      if (requestedElement.isPresent()) {
        break;
      }
    }
    return requestedElement;
  }

  /**
   * We force the current instance of {@link ApplicationModel} to be highly cohesive with {@link MacroExpansionModulesModel} as
   * it's responsibility of this object to properly initialize and expand every global element/operation into the concrete set of
   * message processors
   *
   * @param extensionModels Set of {@link ExtensionModel extensionModels} that will be used to check if the element has to be
   *        expanded.
   */
  private void expandModules(Set<ExtensionModel> extensionModels) {
    new MacroExpansionModulesModel(this, extensionModels).expand();
  }

  /**
   * @return the attributes resolver for this artifact.
   */
  public ConfigurationProperties getConfigurationProperties() {
    return configurationProperties;
  }

  private void resolveRegistrationNames() {
    if (componentBuildingDefinitionRegistry.isPresent()) {
      ComponentBuildingDefinitionRegistry definitionRegistry = componentBuildingDefinitionRegistry.get();

      this.executeOnEveryRootElement(componentModel -> {
        Optional<ComponentBuildingDefinition<?>> buildingDefinition =
            definitionRegistry.getBuildingDefinition(componentModel.getIdentifier());
        buildingDefinition.ifPresent(definition -> {
          if (definition.getRegistrationName() != null) {
            componentModel.setParameter(ApplicationModel.NAME_ATTRIBUTE, definition.getRegistrationName());
          }
        });
      });
    }
  }
}

