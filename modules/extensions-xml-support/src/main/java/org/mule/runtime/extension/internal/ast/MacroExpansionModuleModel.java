/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.ast;

import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_DYNAMIC_CONFIG_REF_PROPERTY;
import static org.mule.runtime.ast.api.ComponentGenerationInformation.EMPTY_GENERATION_INFO;
import static org.mule.runtime.ast.api.ComponentMetadataAst.EMPTY_METADATA;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsNamespace;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyComponentTreeRecursively;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyRecursively;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_ELEMENT;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.SUBFLOW_ELEMENT;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.runtime.extension.internal.ast.XmlSdkImplicitConfig.IMPLICIT_CONFIG_NAME_SUFFIX;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

import static java.lang.Boolean.getBoolean;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.LocationPart;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.AstTraversalDirection;
import org.mule.runtime.ast.api.util.BaseComponentAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;
import org.mule.runtime.extension.internal.ast.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.extension.internal.ast.property.OperationComponentModelModelProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * A {@link MacroExpansionModuleModel} works tightly with an {@link ArtifactAst} to go over all the registered
 * {@link ExtensionModel}s that are XML based (see {@link XmlExtensionModelProperty}) looking for code to macro expand.
 * <p/>
 * For every occurrence that happens, it will expand the operations.
 * <p/>
 * This object works by handling {@link ComponentAst}s directly, consuming the {@link GlobalElementComponentModelModelProperty}
 * for the "config" elements while the {@link OperationComponentModelModelProperty} for the operations (aka: {@link Processor}s in
 * the XML file).
 *
 * @since 4.0
 */
public class MacroExpansionModuleModel {

  private static final Logger LOGGER = getLogger(MacroExpansionModuleModel.class);

  /**
   * literal that represents the name of the global element for any given module. If the module's name is math, then the value of
   * this field will name the global element as <math:config ../>
   */
  public static final String MODULE_CONFIG_GLOBAL_ELEMENT_NAME = "config";

  /**
   * literal that represents the name of the connection element for any given module. If the module's name is github, then the
   * value of this field will name the global element as <github:connection ../>. As an example, think of the following snippet:
   *
   * <code>
   *    <github:config configParameter="someFood" ...>
   *      <github:connection username="myUsername" .../>
   *    </github:config>
   * </code>
   */
  public static final String MODULE_CONNECTION_GLOBAL_ELEMENT_NAME = "connection";

  public static final String MODULE_OPERATION_CONFIG_REF = "config-ref";

  /**
   * Reserved prefix in a <module/> to define a reference an operation of the same module (no circular dependencies allowed)
   */
  public static final String TNS_PREFIX = "tns";

  public static final String DEFAULT_GLOBAL_ELEMENTS = "_defaultGlobalElements";

  /**
   * Used when the <module/> contains global elements without <property/>ies to be expanded, thus the macro expansion will take
   * care of the default global elements macro expanding them ONCE, and replacing the {@link #MODULE_OPERATION_CONFIG_REF} in the
   * <operation/>'s <body/> accordingly.
   */
  private static final String DEFAULT_CONFIG_GLOBAL_ELEMENT_SUFFIX = "%s-default-config-global-element-suffix";
  private static final String IMPLICIT_CONFIG_WARNING =
      "An implicit config is being used for extension %s, this is not fully supported for this extension." +
          " All operation usages of this extension should have a reference to an explicit configuration.";

  private final ArtifactAst applicationModel;
  private final ExtensionModel extensionModel;
  private final Optional<FeatureFlaggingService> featureFlaggingService;

  private final boolean isDynamicConfigRefEnabled = getBoolean(ENABLE_DYNAMIC_CONFIG_REF_PROPERTY);

  /**
   * From a mutable {@code applicationModel}, it will store it to apply changes when the {@link #expand()} method is executed.
   *
   * @param applicationModel to modify given the usages of elements that belong to the {@link ExtensionModel}s contained in the
   *                         {@code extensions} map.
   * @param extensionModel   the {@link ExtensionModel}s to macro expand in the parameterized {@link ArtifactAst}
   */
  MacroExpansionModuleModel(ArtifactAst applicationModel, ExtensionModel extensionModel,
                            Optional<FeatureFlaggingService> featureFlaggingService) {
    this.applicationModel = applicationModel;
    this.extensionModel = extensionModel;
    this.featureFlaggingService = featureFlaggingService;
  }

  public ArtifactAst expand() {
    final List<ComponentAst> moduleGlobalElements = getModuleGlobalElements();
    final Set<String> moduleGlobalElementsNames =
        moduleGlobalElements.stream().map(ComponentAst::getComponentId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toSet());

    return expand(moduleGlobalElements, moduleGlobalElementsNames);
  }

  private ArtifactAst expand(List<ComponentAst> moduleComponentModels, Set<String> moduleGlobalElementsNames) {
    final ArtifactAst expandedArtifactAst;

    if (shouldAddImplicitConfiguration()) {
      LOGGER.warn(format(IMPLICIT_CONFIG_WARNING, extensionModel.getName()));

      if (featureFlaggingService.map(ffs -> !ffs.isEnabled(DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION)).orElse(false)) {
        expandedArtifactAst = copyRecursively(applicationModel, identity(),
                                              () -> singletonList(new XmlSdkImplicitConfig(extensionModel)), comp -> false);
      } else {
        expandedArtifactAst = applicationModel;
      }
    } else {
      expandedArtifactAst = applicationModel;
    }

    return copyRecursively(expandedArtifactAst, comp -> {

      if (comp.getIdentifier().getNamespace().equals(extensionModel.getXmlDslModel().getPrefix())) {
        if (comp.getModel(OperationModel.class).isPresent()) {
          return comp.getModel(OperationModel.class)
              .map(operationModel -> expandOperation(expandedArtifactAst, comp, operationModel, moduleGlobalElementsNames,
                                                     empty()))
              .orElse(comp);
        } else if (comp.getModel(ConfigurationModel.class).isPresent()) {
          return comp.getModel(ConfigurationModel.class)
              .map(configurationModel -> expandGlobalElement(moduleComponentModels, moduleGlobalElementsNames, comp,
                                                             configurationModel))
              .orElse(comp);
        }
      }

      return comp;
    },
                           () -> macroExpandDefaultGlobalElements(moduleGlobalElementsNames)
                               .map(Collections::singletonList)
                               .orElse(emptyList()),
                           comp -> false);
  }

  private boolean shouldAddImplicitConfiguration() {
    return existOperationThatUsesImplicitConfiguration() && hasXmlSdkPropertiesWithDefaultValues();
  }

  private boolean existOperationThatUsesImplicitConfiguration() {
    return applicationModel.topLevelComponentsStream()
        .filter(componentAst -> componentAst.getIdentifier().getName().equals(FLOW_ELEMENT)
            || componentAst.getIdentifier().getName().equals(SUBFLOW_ELEMENT))
        .flatMap(ComponentAst::directChildrenStream)
        .anyMatch(this::existOperationThatUsesImplicitConfiguration);
  }

  private boolean existOperationThatUsesImplicitConfiguration(ComponentAst componentAst) {
    if (componentAst.getIdentifier().getNamespace().equals(extensionModel.getXmlDslModel().getPrefix()) &&
        componentAst.getModel(OperationModel.class).isPresent() &&
        !componentAst.getParameters().contains(MODULE_OPERATION_CONFIG_REF)) {
      return true;
    }

    for (ComponentAst childComponentAst : componentAst.directChildren()) {
      if (existOperationThatUsesImplicitConfiguration(childComponentAst)) {
        return true;
      }
    }

    return false;
  }

  private boolean hasXmlSdkPropertiesWithDefaultValues() {
    return extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME)
        .map(ParameterizedModel::getParameterGroupModels)
        .orElse(emptyList())
        .stream()
        .filter(parameterGroupModel -> parameterGroupModel.getName().equals(DEFAULT_GROUP_NAME))
        .findFirst()
        .map(ParameterGroupModel::getParameterModels)
        .orElse(emptyList())
        .stream()
        .noneMatch(parameterModel -> parameterModel.isRequired() && !parameterModel.getName().equals("name"));
  }

  private Optional<String> defaultGlobalElementName() {
    Optional<String> defaultElementName = empty();
    if (extensionModel.getConfigurationModels().isEmpty()
        && extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent()) {
      defaultElementName = extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class)
          .map(globalElementComponentModelModelProperty -> format(DEFAULT_CONFIG_GLOBAL_ELEMENT_SUFFIX,
                                                                  extensionModel.getName()));
    }
    return defaultElementName;
  }

  private Optional<ComponentAst> macroExpandDefaultGlobalElements(Set<String> moduleGlobalElementsNames) {
    return defaultGlobalElementName()
        .map(defaultGlobalElementSuffix -> {
          // scenario where it will macro expand the default elements of a <module/>
          List<ComponentAst> globalElements =
              extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).get().getGlobalElements();

          final List<ComponentAst> mappedGlobalElements = globalElements
              .stream()
              .map(globalElement -> copyComponentTreeRecursively(globalElement,
                                                                 comp -> new MacroExpandedComponentAst(comp,
                                                                                                       resolveMacroExpandedLocation(defaultGlobalElementSuffix,
                                                                                                                                    globalElement,
                                                                                                                                    comp),
                                                                                                       moduleGlobalElementsNames,
                                                                                                       defaultGlobalElementSuffix,
                                                                                                       comp.directChildrenStream()
                                                                                                           .collect(toList()))))
              .collect(toList());

          return new BaseComponentAst() {

            private final ComponentIdentifier identifier = ComponentIdentifier.builder()
                .namespaceUri(extensionModel.getXmlDslModel().getNamespace())
                .namespace(extensionModel.getXmlDslModel().getPrefix())
                .name(DEFAULT_GLOBAL_ELEMENTS).build();

            @Override
            public Stream<ComponentAst> recursiveStream(AstTraversalDirection direction) {
              return Stream.concat(Stream.of(this),
                                   mappedGlobalElements.stream()
                                       .flatMap(g -> g.recursiveStream(direction)));
            }

            @Override
            public Spliterator<ComponentAst> recursiveSpliterator(AstTraversalDirection direction) {
              return recursiveStream(direction).spliterator();
            }

            @Override
            public List<ComponentAst> directChildren() {
              return mappedGlobalElements;
            }

            @Override
            public Collection<ComponentParameterAst> getParameters() {
              return emptySet();
            }

            @Override
            public ComponentParameterAst getParameter(String groupName, String paramName) {
              throw new NoSuchElementException(DEFAULT_GLOBAL_ELEMENTS + " does not have parameters");
            }

            @Override
            public ExtensionModel getExtensionModel() {
              return extensionModel;
            }

            @Override
            public <M> Optional<M> getModel(Class<M> modelClass) {
              return empty();
            }

            @Override
            public MetadataType getType() {
              return null;
            }

            @Override
            public Map<String, Object> getAnnotations() {
              return emptyMap();
            }

            @Override
            public ComponentMetadataAst getMetadata() {
              return EMPTY_METADATA;
            }

            @Override
            public ComponentGenerationInformation getGenerationInformation() {
              return EMPTY_GENERATION_INFO;
            }

            @Override
            public ComponentLocation getLocation() {
              return from(DEFAULT_GLOBAL_ELEMENTS);
            }

            @Override
            public ComponentIdentifier getIdentifier() {
              return identifier;
            }

            @Override
            public ComponentType getComponentType() {
              return null;
            }

            @Override
            public Optional<String> getComponentId() {
              return empty();
            }
          };
        });
  }

  private ComponentAst expandGlobalElement(List<ComponentAst> moduleComponentModels, Set<String> moduleGlobalElementsNames,
                                           ComponentAst comp, ConfigurationModel configurationModel) {
    Map<String, Object> propertiesMap = comp.getParameters()
        .stream()
        .filter(paramAst -> paramAst.getValue().isRight())
        .collect(toMap(paramAst -> paramAst.getModel().getName(),
                       paramAst -> paramAst.getValue().getRight()));
    Map<String, String> connectionPropertiesMap =
        extractConnectionProperties(comp, configurationModel);
    propertiesMap.putAll(connectionPropertiesMap);
    final Map<String, Object> literalsParameters = getLiteralParameters(propertiesMap, emptyMap());

    final String defaultGlobalElementSuffix = comp.getComponentId().orElse("");

    final List<ComponentAst> mappedModuleComponentModels = moduleComponentModels.stream()
        .map(nestedComp -> copyComponentTreeRecursively(nestedComp,
                                                        c -> new MacroExpandedComponentAst(c,
                                                                                           resolveMacroExpandedLocation(defaultGlobalElementSuffix,
                                                                                                                        nestedComp,
                                                                                                                        c),
                                                                                           moduleGlobalElementsNames,
                                                                                           defaultGlobalElementSuffix,
                                                                                           literalsParameters,
                                                                                           c.directChildrenStream()
                                                                                               .collect(toList()))))
        .collect(toList());

    return new BaseComponentAstDecorator(comp) {

      @Override
      public Stream<ComponentAst> directChildrenStream() {
        return mappedModuleComponentModels.stream();
      }
    };
  }

  private Optional<ConfigurationModel> getConfigurationModel() {
    return extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME);
  }

  private ComponentLocation resolveMacroExpandedLocation(final String defaultGlobalElementSuffix,
                                                         ComponentAst macroExpandedComp,
                                                         ComponentAst innerComp) {
    final List<DefaultLocationPart> parts = new ArrayList<>();

    final LocationPart firstPart = macroExpandedComp.getLocation().getParts().get(0);
    parts.add(new DefaultLocationPart(macroExpandedComp.getComponentId()
        .map(id -> id.concat("-").concat(defaultGlobalElementSuffix))
        .orElse(defaultGlobalElementSuffix),
                                      firstPart.getPartIdentifier(),
                                      firstPart.getFileName(),
                                      firstPart.getLine(),
                                      firstPart.getColumn()));

    innerComp.getLocation().getParts()
        .stream()
        .skip(2)
        .map(lp -> (DefaultLocationPart) lp)
        .forEach(parts::add);

    return new DefaultComponentLocation(innerComp.getComponentId(), parts);
  }

  private List<ComponentAst> getModuleGlobalElements() {
    return extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class)
        .map(GlobalElementComponentModelModelProperty::getGlobalElements)
        .orElse(emptyList());
  }

  /**
   * Takes a one liner call to any given message processor, expand it to creating a "module-operation-chain" scope which has the
   * set of properties, the set of parameters and the list of message processors to execute.
   *
   * @param operationRefModel         message processor that will be replaced by a scope element named "module-operation-chain".
   * @param operationModel            operation that provides both the <parameter/>s and content of the <body/>
   * @param moduleGlobalElementsNames collection with the global components names (such as <http:config name="a"../>, <file:config
   *                                  name="b"../>, <file:matcher name="c"../> and so on) that are contained within the <module/>
   *                                  that will be macro expanded
   * @param configRefParentTnsName    parent reference to the global element if exists (it might not be global elements in the
   *                                  current module). Useful when replacing {@link #TNS_PREFIX} operations, as the references to
   *                                  the global elements will be those of the rootest element of the operations consumed by the
   *                                  app.
   * @return a new component model that represents the old placeholder but expanded with the content of the <body/>
   */
  private ComponentAst expandOperation(ArtifactAst expandedArtifactAst, ComponentAst operationRefModel,
                                       OperationModel operationModel, Set<String> moduleGlobalElementsNames,
                                       Optional<String> configRefParentTnsName) {
    final OperationComponentModelModelProperty operationComponentModelModelProperty =
        operationModel.getModelProperty(OperationComponentModelModelProperty.class).get();
    final ComponentAst operationModuleComponentModel = operationComponentModelModelProperty
        .getBodyComponentModel();
    Optional<String> configRefName = referencesOperationsWithinModule(operationRefModel)
        ? configRefParentTnsName
        : getConfigRefName(operationRefModel);

    final Optional<String> configRef =
        !configRefName.isPresent() && extensionModel.getConfigurationModel(MODULE_CONFIG_GLOBAL_ELEMENT_NAME).isPresent()
            && (shouldAddImplicitConfiguration() && featureFlaggingService
                .map(ffs -> !ffs.isEnabled(DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION)).orElse(false))
                    ? of(format(IMPLICIT_CONFIG_NAME_SUFFIX, extensionModel.getName()))
                    : configRefName;

    Map<String, String> propertiesMap = extractProperties(expandedArtifactAst, configRef);
    Map<String, String> parametersMap = operationRefModel.getParameters().stream()
        .filter(paramAst -> paramAst.getResolvedRawValue() != null)
        .collect(toMap(paramAst -> paramAst.getModel().getName(), paramAst -> paramAst.getResolvedRawValue()));

    final Map<String, Object> literalParameters = getLiteralParameters(propertiesMap, parametersMap);

    List<ComponentAst> processorChainChildren = operationModuleComponentModel.directChildrenStream()
        .map(bodyProcessor -> copyComponentTreeRecursively(bodyProcessor,
                                                           operationChildModel -> lookForTNSOperation(operationChildModel)
                                                               .map(tnsOperation -> expandOperation(expandedArtifactAst,
                                                                                                    operationChildModel,
                                                                                                    tnsOperation,
                                                                                                    moduleGlobalElementsNames,
                                                                                                    configRef))
                                                               .orElseGet(() -> new MacroExpandedComponentAst(operationChildModel,
                                                                                                              operationChildModel
                                                                                                                  .getLocation(),
                                                                                                              moduleGlobalElementsNames,
                                                                                                              configRef
                                                                                                                  .orElse(""),
                                                                                                              literalParameters,
                                                                                                              operationChildModel
                                                                                                                  .directChildrenStream()
                                                                                                                  .collect(toList())))))
        .collect(toList());

    return new BaseComponentAstDecorator(operationRefModel) {

      @Override
      public Stream<ComponentAst> directChildrenStream() {
        return concat(super.directChildrenStream(), processorChainChildren.stream());
      }
    };
  }

  /**
   * Looks for the value of the {@link #MODULE_OPERATION_CONFIG_REF} in the current <operation/>, if not found then tries to
   * fallback to the default global element name. See {@link #defaultGlobalElementName()} method.
   *
   * @param operationRefModel <operaton/> to lookup the expected string reference, if exists.
   * @return the suffix needed to be used when macro expanding elements, or {@link Optional#empty()} otherwise.
   */
  private Optional<String> getConfigRefName(ComponentAst operationRefModel) {
    ComponentParameterAst parameterAst = operationRefModel.getParameter(DEFAULT_GROUP_NAME, MODULE_OPERATION_CONFIG_REF);
    if (parameterAst != null && parameterAst.getResolvedRawValue() != null) {
      return of(parameterAst.getResolvedRawValue());
    } else {
      return defaultGlobalElementName();
    }
  }

  /**
   * @param propertiesMap <property>s that are feed in the current usage of the <module/>
   * @param parametersMap <param>s that are feed in the current usage of the <module/>
   * @return a {@link Map} of <property>s and <parameter>s that could be replaced by their literal values
   */
  private Map<String, Object> getLiteralParameters(Map<String, ?> propertiesMap, Map<String, String> parametersMap) {
    final Map<String, Object> literalParameters = propertiesMap.entrySet().stream()
        .filter(entry -> !isExpression(entry.getValue()))
        .collect(toMap(e -> getReplaceableExpression(e.getKey(), VARS),
                       Map.Entry::getValue));

    literalParameters.putAll(parametersMap.entrySet().stream()
        .filter(entry -> !isExpression(entry.getValue()))
        .collect(toMap(e -> getReplaceableExpression(e.getKey(), VARS),
                       Map.Entry::getValue)));
    return literalParameters;
  }

  /**
   * Assembly an expression to validate if the macro expansion of the current <module> can be directly replaced by the literals
   * value
   *
   * @param name   of the parameter (either a <property> or a <parameter>)
   * @param prefix binding to append for the expression to be replaced in the <module>'s code
   * @return the expression that access a variable through a direct binding (aka: a "static expression", as it doesn't use the
   *         {@link CoreEvent})
   */
  private String getReplaceableExpression(String name, String prefix) {
    return "#[" + prefix + "." + name + "]";
  }

  /**
   * Extracts the properties of the current <module/> if applies (it might not have a configuration in it)
   *
   * @param configRefName current <operation/> to macro expand, from which the config-ref attribute's value will be extracted.
   * @return a map with the name and values of the <module/>'s properties.
   */
  private Map<String, String> extractProperties(ArtifactAst expandedArtifactAst, Optional<String> configRefName) {
    Map<String, String> valuesMap = new HashMap<>();
    configRefName
        .filter(configParameter -> shouldExtractPropertiesFrom(configParameter))
        .ifPresent(configParameter -> {
          ComponentAst configRefComponentModel = getComponentAst(expandedArtifactAst, configParameter);
          // as configParameter != null, a ConfigurationModel must exist
          final ConfigurationModel configurationModel = getConfigurationModel().get();
          configRefComponentModel.getParameters().stream()
              .filter(paramAst -> paramAst.getResolvedRawValue() != null)
              .forEach(paramAst -> valuesMap.put(paramAst.getModel().getName(), paramAst.getResolvedRawValue()));
          valuesMap.putAll(extractConnectionProperties(configRefComponentModel, configurationModel));
        });
    return valuesMap;
  }

  private boolean shouldExtractPropertiesFrom(String configParameter) {
    if (isDynamicConfigRefEnabled && isExpression(configParameter)) {
      // if the config-ref is an expression we can't macro-expand the properties, they'll need to be resolved in runtime
      return false;
    }

    return defaultGlobalElementName().map(defaultGlobalElementName -> !defaultGlobalElementName.equals(configParameter))
        .orElse(true);
  }

  private ComponentAst getComponentAst(ArtifactAst applicationModel, String configParameter) {
    // look for the global element which "name" attribute maps to "configParameter" value
    // or a nested element to a config that was added by the macroexpansion of another module before
    return applicationModel
        .filteredComponents(equalsNamespace(extensionModel.getXmlDslModel().getPrefix()))
        .filter(componentModel -> componentModel.getModel(ConfigurationModel.class).isPresent()
            && configParameter.equals(componentModel.getComponentId().orElse(null)))
        .findFirst()
        .orElseGet(() -> { // Else look for the element in the parent ast (e.g. the domain of the app)
          Optional<ArtifactAst> parent = applicationModel.getParent();
          if (parent.isPresent()) {
            return getComponentAst(parent.get(), configParameter);
          } else {
            throw new IllegalArgumentException(format("There's no <%s:config> named [%s] in the current mule app nor in its domain",
                                                      extensionModel.getXmlDslModel().getPrefix(), configParameter));
          }
        });
  }

  /**
   * If the current {@link ExtensionModel} does have a {@link ConnectionProviderModel}, then it will check if the current XML does
   * contain a child of it under the connection name (see
   * {@link ModuleOperationMessageProcessorChain#MODULE_CONNECTION_GLOBAL_ELEMENT_NAME}.
   *
   * @param configRefComponentModel root element of the current XML config (global element of the parameterized operation)
   * @param configurationModel      configuration model of the current element
   * @return a map of properties to be added in the macro expanded <operation/>
   */
  private Map<String, String> extractConnectionProperties(ComponentAst configRefComponentModel,
                                                          ConfigurationModel configurationModel) {
    return configurationModel.getConnectionProviderModel(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME)
        .flatMap(connectionProviderModel -> configRefComponentModel.directChildrenStream()
            .filter(componentModel -> MODULE_CONNECTION_GLOBAL_ELEMENT_NAME
                .equals(componentModel.getIdentifier().getName()))
            .findFirst()
            .map(connectionComponentModel -> connectionComponentModel.getParameters().stream()
                .filter(paramAst -> paramAst.getResolvedRawValue() != null)
                .collect(toMap(paramAst -> paramAst.getModel().getName(),
                               paramAst -> paramAst.getResolvedRawValue()))))
        .orElse(emptyMap());
  }

  /**
   * True if an <operation/> A calls an <operation/> B defined in the same <module/> by using <tns:B .../>
   *
   * @param operationComponentModel operation that might or might not be referencing operations of the same module.
   * @return true if it's an <operation/> reference in the same <module/>, false otherwise
   */
  private boolean referencesOperationsWithinModule(ComponentAst operationComponentModel) {
    return TNS_PREFIX.equals(operationComponentModel.getIdentifier().getNamespace());
  }

  /**
   * Looks for an operation exposed in the current {@link ExtensionModel} that's being targeted by other operation through the
   * {@link #TNS_PREFIX} prefix.
   *
   * @param componentModel to check whether targets a <module/>'s operation or not.
   * @return an {@link OperationModel} if the parameterized {@code componentModel} targets an <operation/> of the same module by
   *         using the {@link #TNS_PREFIX} prefix.
   */
  private Optional<OperationModel> lookForTNSOperation(ComponentAst componentModel) {
    if (referencesOperationsWithinModule(componentModel)) {
      return componentModel.getModel(OperationModel.class);
    } else {
      return empty();
    }
  }

}
