/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.component.Component.NS_MULE_PARSER_METADATA;
import static org.mule.runtime.api.util.NameUtils.toCamelCase;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ON_ERROR_CONTINE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ON_ERROR_PROPAGATE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.ERROR_MAPPING_IDENTIFIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.AstTraversalDirection;
import org.mule.runtime.config.internal.model.type.MetadataTypeModelAdapter;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.internal.component.config.InternalComponentConfiguration;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.namespace.QName;

/**
 * An {@code ComponentModel} represents the user configuration of a component (flow, config, message processor, etc) defined in an
 * artifact configuration file.
 * <p/>
 * Every {@code ComponentModel} represents the configuration of a core configuration or an extension configuration. Which
 * configuration element this object represents is identified by a {@link ComponentIdentifier} that can be retrieved using
 * {@code #getName}.
 * <p/>
 * It may have simple configuration parameters which are retrieve by using {@code #getParameterGroups} or complex parameters which
 * are retrieved using {@code #getInnerComponents}.
 * <p/>
 * There's a set of configuration attributes or custom attributes that may not be mapped directly to the object that runs on
 * runtime but may be hold by a {@code ComponentModel}. Those attributes are retrieved by using {@code #getCustomAttributes}.
 *
 * @since 4.0
 */
public class ComponentModel implements ComponentAst {

  public static String COMPONENT_MODEL_KEY = "ComponentModel";

  public static final String SOURCE_TYPE = "sourceType";
  public static final String TARGET_TYPE = "targetType";

  private ComponentIdentifier identifier;
  private String componentId;
  private final Map<String, String> parameters = new HashMap<>();
  private final Map<String, ComponentParameterAst> parameterAstsByName = new HashMap<>();
  private List<ComponentParameterAst> parameterAsts;
  private final AtomicBoolean parameterAstsPopulated = new AtomicBoolean(false);
  private final Set<String> schemaValueParameter = new HashSet<>();
  // TODO MULE-9638 This must go away from component model once it's immutable.
  private final List<ComponentModel> innerComponents = new ArrayList<>();
  private String textContent;
  private ComponentLocation componentLocation;
  private TypedComponentIdentifier.ComponentType componentType;
  private org.mule.runtime.api.meta.model.ComponentModel componentModel;
  private NestableElementModel nestableElementModel;
  private ConfigurationModel configurationModel;
  private ConnectionProviderModel connectionProviderModel;
  private MetadataTypeModelAdapter metadataTypeModelAdapter;

  private ComponentMetadataAst componentMetadata;

  /**
   * @return the line number in which the component was defined in the configuration file. It may be empty if the component was
   * created pragmatically.
   */
  @Deprecated
  public Optional<Integer> getLineNumber() {
    return componentMetadata.getStartLine().isPresent() ? of(componentMetadata.getStartLine().getAsInt()) : empty();
  }

  /**
   * @return the start column in which the component was defined in the configuration file. It may be empty if the component was
   * created pragmatically.
   */
  @Deprecated
  public Optional<Integer> getStartColumn() {
    return componentMetadata.getStartColumn().isPresent() ? of(componentMetadata.getStartColumn().getAsInt()) : empty();
  }

  /**
   * @return the config file name in which the component was defined. It may be empty if the component was created pragmatically.
   */
  @Deprecated
  public Optional<String> getConfigFileName() {
    return componentMetadata.getFileName();
  }

  /**
   * @return the configuration identifier.
   */
  @Override
  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * @return a {@code java.util.Map} with the simple parameters of the configuration.
   */
  public Map<String, String> getRawParameters() {
    return unmodifiableMap(parameters);
  }

  /**
   * @return a {@code java.util.List} of all the child {@code ComponentModel}s
   */
  public List<ComponentModel> getInnerComponents() {
    return innerComponents;
  }

  /**
   * @return a {@code java.util.Map} with all the custom attributes.
   */
  @Deprecated
  public Map<String, Object> getCustomAttributes() {
    Map<String, Object> attrs = new HashMap<>();

    attrs.putAll(componentMetadata.getParserAttributes());
    componentMetadata.getDocAttributes()
        .forEach((k, v) -> attrs.put("{" + NS_MULE_DOCUMENTATION + "}" + k, v));

    return attrs;
  }

  @Override
  public ComponentLocation getLocation() {
    return getComponentLocation();
  }

  @Override
  public Optional<String> getComponentId() {
    if (getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
      return of(OBJECT_MULE_CONFIGURATION);
    } else if (getModel(ConstructModel.class)
        .map(cm -> cm.getName().equals("object"))
        .orElse(getIdentifier().equals(ON_ERROR_CONTINE_IDENTIFIER)
            || getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER))) {
      return ofNullable(getRawParameters().get(NAME_ATTRIBUTE_NAME));
    } else if (getIdentifier().equals(ERROR_HANDLER_IDENTIFIER) && getRawParameterValue("ref").isPresent()) {
      return empty();
    } else if (getModel(ParameterizedModel.class).isPresent()) {
      populateParameterAsts();
      return ofNullable(componentId);
    } else {
      // fallback for dsl elements that do not have an extension model declaration
      return ofNullable(getRawParameters().get(NAME_ATTRIBUTE_NAME));
    }
  }

  @Override
  public Optional<String> getRawParameterValue(String paramName) {
    if (paramName.equals(BODY_RAW_PARAM_NAME)) {
      return ofNullable(getTextContent());
    } else {
      return ofNullable(getRawParameters().get(paramName));
    }
  }

  /**
   * @param parameterName name of the configuration parameter.
   * @param value value contained by the configuration parameter.
   */
  public void setParameter(ParameterModel parameterModel, ComponentParameterAst value) {
    parameterAstsPopulated.set(false);

    this.parameters.put(parameterModel.getName(), value.getRawValue());
    this.parameterAstsByName.put(parameterModel.getName(), value);
  }

  /**
   * @param paramName the name of the parameter to get AST for.
   * @return the AST of the parameter if present, or {@link Optional#empty()} if not present.
   */
  @Override
  public ComponentParameterAst getParameter(String paramName) {
    populateParameterAsts();
    return parameterAstsByName.get(paramName);
  }

  @Override
  public Collection<ComponentParameterAst> getParameters() {
    populateParameterAsts();
    return parameterAsts;
  }

  private void populateParameterAsts() {
    if (!parameterAstsPopulated.compareAndSet(false, true)) {
      return;
    }

    if (!getModel(ParameterizedModel.class).isPresent()) {
      throw new IllegalStateException("Model for '" + this.toString() + "' (a '"
          + getModel(NamedObject.class).map(NamedObject::getName) + ")' is not parameterizable.");
    }

    getModel(ParameterizedModel.class)
        .ifPresent(parameterizedModel -> {
          getModel(SourceModel.class)
              // For sources, we need to account for the case where parameters in the callbacks may have colliding names.
              // This logic ensures that the parameter fetching logic is consistent with the logic that handles this scenario in
              // previous implementations.
              .map(sourceModel -> Stream
                  .concat(parameterizedModel.getParameterGroupModels().stream(),
                          Stream.concat(sourceModel.getSuccessCallback().map(cb -> cb.getParameterGroupModels().stream())
                              .orElse(Stream.empty()),
                                        sourceModel.getErrorCallback().map(cb -> cb.getParameterGroupModels().stream())
                                            .orElse(Stream.empty()))))
              .orElse(parameterizedModel.getParameterGroupModels().stream())
              .forEach(pg -> {
                if (pg.isShowInDsl()) {
                  final Optional<ComponentAst> paramGroupComp = directChildrenStream()
                      .filter(comp -> pg.getName().equals(toCamelCase(comp.getIdentifier().getName(), "-")))
                      .findAny();

                  if (paramGroupComp.isPresent()) {
                    pg.getParameterModels()
                        .forEach(paramModel -> populateParameterAst(paramGroupComp.get()
                            .getRawParameterValue(paramModel.getName()), paramModel));
                  } else {
                    pg.getParameterModels()
                        .forEach(paramModel -> populateParameterAst(empty(), paramModel));
                  }
                } else {
                  pg.getParameterModels().forEach(paramModel -> {
                    if (ERROR_MAPPINGS_PARAMETER_NAME.equals(paramModel.getName())) {
                      final List<ErrorMapping> errorMappings = directChildrenStream()
                          .filter(child -> ERROR_MAPPING_IDENTIFIER.equals(child.getIdentifier()))
                          .map(child -> new ErrorMapping(child.getRawParameterValue(SOURCE_TYPE).orElse(ANY_IDENTIFIER),
                                                         child.getRawParameterValue(TARGET_TYPE).orElse(null)))
                          .collect(toList());
                      parameterAstsByName.put(paramModel.getName(),
                                              new DefaultComponentParameterAst(errorMappings, () -> paramModel, null));
                    } else {
                      final ComponentParameterAst computedParam =
                          populateParameterAst(this.getRawParameterValue(paramModel.getName()), paramModel);
                      if (paramModel.isComponentId()) {
                        componentId = (String) computedParam.getValue().getRight();
                      }
                    }
                  });
                }

              });

          // Keep parameter order defined on parameterized model
          this.parameterAsts = this.parameterAstsByName.values().stream().filter(param -> param.getValue().getValue().isPresent())
              .sorted(new Comparator<ComponentParameterAst>() {

                final List<String> params = parameterizedModel.getAllParameterModels()
                    .stream()
                    .map(NamedObject::getName)
                    .collect(toList());

                @Override
                public int compare(ComponentParameterAst o1, ComponentParameterAst o2) {
                  return Integer.compare(params.indexOf(o1.getModel().getName()), params.indexOf(o2.getModel().getName()));
                }
              }).collect(toList());
        });
  }

  private ComponentParameterAst populateParameterAst(Optional<String> rawValue, ParameterModel paramModel) {
    return parameterAstsByName.computeIfAbsent(paramModel.getName(),
                                               paramNameKey -> rawValue
                                                   .map(rawParamValue -> new DefaultComponentParameterAst(rawParamValue,
                                                                                                          () -> paramModel))
                                                   .orElseGet(() -> new DefaultComponentParameterAst(null,
                                                                                                     () -> paramModel)));
  }

  @Override
  public Stream<ComponentAst> recursiveStream(AstTraversalDirection direction) {
    return StreamSupport.stream(recursiveSpliterator(direction), false);
  }

  @Override
  public Spliterator<ComponentAst> recursiveSpliterator(AstTraversalDirection direction) {
    return direction.recursiveSpliterator(this);
  }

  @Override
  public Stream<ComponentAst> directChildrenStream() {
    return getInnerComponents().stream().map(cm -> (ComponentAst) cm);
  }

  @Override
  public Spliterator<ComponentAst> directChildrenSpliterator() {
    return directChildrenStream().spliterator();
  }


  /**
   * @param componentType the {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType} of the object to be
   *                      created when processing this {@code ComponentModel}.
   */
  public void setComponentType(TypedComponentIdentifier.ComponentType componentType) {
    this.componentType = componentType;
  }

  @Override
  public TypedComponentIdentifier.ComponentType getComponentType() {
    return componentType != null ? componentType : TypedComponentIdentifier.ComponentType.UNKNOWN;
  }

  @Override
  public <M> Optional<M> getModel(Class<M> modelClass) {
    if (componentModel != null) {
      if (modelClass.isAssignableFrom(componentModel.getClass())) {
        return Optional.of((M) componentModel);
      }
    }

    if (configurationModel != null) {
      if (modelClass.isAssignableFrom(configurationModel.getClass())) {
        return Optional.of((M) configurationModel);
      }
    }

    if (connectionProviderModel != null) {
      if (modelClass.isAssignableFrom(connectionProviderModel.getClass())) {
        return Optional.of((M) connectionProviderModel);
      }
    }

    if (nestableElementModel != null) {
      if (modelClass.isAssignableFrom(nestableElementModel.getClass())) {
        return Optional.of((M) nestableElementModel);
      }
    }

    if (metadataTypeModelAdapter != null) {
      if (modelClass.isAssignableFrom(metadataTypeModelAdapter.getClass())) {
        return Optional.of((M) metadataTypeModelAdapter);
      }
    }

    return empty();
  }

  public void setComponentModel(org.mule.runtime.api.meta.model.ComponentModel model) {
    this.componentModel = model;
  }

  public void setNestableElementModel(NestableElementModel nestableElementModel) {
    this.nestableElementModel = nestableElementModel;
  }

  public void setConfigurationModel(ConfigurationModel model) {
    this.configurationModel = model;
  }

  public void setConnectionProviderModel(ConnectionProviderModel connectionProviderModel) {
    this.connectionProviderModel = connectionProviderModel;
  }

  public void setMetadataTypeModelAdapter(MetadataTypeModelAdapter metadataTypeModelAdapter) {
    this.metadataTypeModelAdapter = metadataTypeModelAdapter;
  }

  /**
   * @return the value of the name attribute.
   * @deprecated Use {@link ComponentAst#getComponentId()} instead.
   */
  @Deprecated
  public String getNameAttribute() {
    if (this instanceof ComponentAst) {
      return getComponentId()
          .orElseGet(() -> parameters.get(ApplicationModel.NAME_ATTRIBUTE));
    } else {
      return parameters.get(ApplicationModel.NAME_ATTRIBUTE);
    }
  }

  /**
   * @return content of the configuration element.
   */
  public String getTextContent() {
    return textContent;
  }

  /**
   * @param componentLocation the location of the component in the configuration.
   */
  public void setComponentLocation(ComponentLocation componentLocation) {
    this.componentLocation = componentLocation;
  }

  /**
   * @return the location of the component in the configuration.
   */
  public ComponentLocation getComponentLocation() {
    return componentLocation;
  }

  /**
   * @param parameterName configuration parameter name
   * @return true if the value provided for the configuration parameter was get from the DSL schema, false if it was explicitly
   * defined in the config
   */
  public boolean isParameterValueProvidedBySchema(String parameterName) {
    return this.schemaValueParameter.contains(parameterName);
  }

  /**
   * Executes the task on every inner component associated to this componentModel.
   *
   * @param task to be executed on inner components.
   */
  public void executedOnEveryInnerComponent(final Consumer<ComponentModel> task) {
    for (ComponentModel componentModel : innerComponents) {
      task.accept(componentModel);
      componentModel.executedOnEveryInnerComponent(task);
    }
  }

  /**
   * @return the source code associated with this component.
   */
  @Deprecated
  public String getSourceCode() {
    return componentMetadata.getSourceCode().orElse(null);
  }

  @Override
  public ComponentMetadataAst getMetadata() {
    return componentMetadata;
  }

  /**
   * Builder to create instances of {@code ComponentModel}.
   */
  public static class Builder {

    private final ComponentModel model = new ComponentModel();
    private ComponentModel root;

    private final org.mule.runtime.ast.api.ComponentMetadataAst.Builder metadataBuilder = ComponentMetadataAst.builder();

    /**
     * Default constructor for this builder.
     */
    public Builder() {}

    /**
     * Creates an instance of the Builder which will allow to merge other root component models to the given one. The root
     * component model provided here will be modified instead of cloned.
     *
     * @param root {@link ComponentModel} to be used as root. It will be modified.
     */
    public Builder(ComponentModel root) {
      this.root = root;
    }

    /**
     * @param identifier identifier for the configuration element this object represents.
     * @return the builder.
     */
    public Builder setIdentifier(ComponentIdentifier identifier) {
      this.model.identifier = identifier;
      return this;
    }

    /**
     * @param parameterName   name of the configuration parameter.
     * @param value           value contained by the configuration parameter.
     * @param valueFromSchema
     * @return the builder.
     */
    public Builder addParameter(String parameterName, String value, boolean valueFromSchema) {
      this.model.parameters.put(parameterName, value);
      if (valueFromSchema) {
        this.model.schemaValueParameter.add(parameterName);
      }
      return this;
    }

    /**
     * Adds a new complex child object to this {@code ComponentModel}.
     *
     * @param componentModel child {@code ComponentModel} declared in the configuration.
     * @return the builder.
     */
    public Builder addChildComponentModel(ComponentModel componentModel) {
      this.model.innerComponents.add(componentModel);
      return this;
    }

    /**
     * Sets the inner content of the configuration element.
     *
     * @param textContent inner text content from the configuration.
     * @return the builder.
     */
    public Builder setTextContent(String textContent) {
      this.model.textContent = textContent;
      return this;
    }

    /**
     * Adds a custom attribute to the {@code ComponentModel}. This custom attribute is meant to hold metadata of the configuration
     * and not to be used to instantiate the runtime object that corresponds to this configuration.
     *
     * @param name  custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(String name, Object value) {
      return addCustomAttribute(QName.valueOf(name), value);
    }

    /**
     * Adds a custom attribute to the {@code ComponentModel}. This custom attribute is meant to hold metadata of the configuration
     * and not to be used to instantiate the runtime object that corresponds to this configuration.
     *
     * @param name  custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(final QName qname, Object value) {
      if (isEmpty(qname.getNamespaceURI()) || NS_MULE_PARSER_METADATA.equals(qname.getNamespaceURI())) {
        this.metadataBuilder.putParserAttribute(qname.getLocalPart(), value);
      } else {
        this.metadataBuilder.putDocAttribute(qname.toString(), value.toString());
        if (NS_MULE_DOCUMENTATION.equals(qname.getNamespaceURI())) {
          // This is added for compatibility, since in previous versions the doc attributes were looked up without the namespace.
          this.metadataBuilder.putDocAttribute(qname.getLocalPart(), value.toString());
        }
      }
      return this;
    }

    /**
     * @param configFileName the config file name in which this component was defined.
     * @return the builder.
     */
    public Builder setConfigFileName(String configFileName) {
      this.metadataBuilder.setFileName(configFileName);
      return this;
    }

    /**
     * @param lineNumber the line number within the config file in which this component was defined.
     * @return the builder.
     */
    public Builder setLineNumber(int lineNumber) {
      this.metadataBuilder.setStartLine(lineNumber);
      this.metadataBuilder.setEndLine(lineNumber);
      return this;
    }

    /**
     * @param startColumn the start column within the config file in which this component was defined.
     * @return the builder.
     */
    public Builder setStartColumn(int startColumn) {
      this.metadataBuilder.setStartColumn(startColumn);
      this.metadataBuilder.setEndColumn(startColumn);
      return this;
    }

    /**
     * @param sourceCode the source code associated with this component.
     * @return the builder.
     */
    public Builder setSourceCode(String sourceCode) {
      this.metadataBuilder.setSourceCode(sourceCode);
      return this;
    }

    /**
     * @return a {@code ComponentModel} created based on the supplied parameters.
     */
    public ComponentModel build() {
      if (root != null) {
        return root;
      }
      checkState(model.identifier != null, "An identifier must be provided");
      model.componentMetadata = metadataBuilder.build();
      return model;
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ComponentModel that = (ComponentModel) o;

    if (!Objects.equals(componentLocation, that.componentLocation)) {
      return false;
    }
    if (!identifier.equals(that.identifier)) {
      return false;
    }
    return parameters.equals(that.parameters);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(componentLocation);
    result = 31 * result + identifier.hashCode();
    result = 31 * result + parameters.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return getComponentId().map(n -> "" + n + "(" + getIdentifier().toString() + ")").orElse(getIdentifier().toString())
        + (getLocation() != null ? (" @ " + getLocation().getLocation()) : "");
  }

}
