/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.component.Component.NS_MULE_PARSER_METADATA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.resolveComponentType;
import static org.mule.runtime.config.internal.model.MetadataTypeModelAdapter.createMetadataTypeModelAdapterWithSterotype;
import static org.mule.runtime.config.internal.model.MetadataTypeModelAdapter.createParameterizedTypeModelAdapter;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper.ExtensionWalkerModelDelegate;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.internal.component.config.InternalComponentConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

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
public abstract class ComponentModel {

  public static String COMPONENT_MODEL_KEY = "ComponentModel";

  private boolean root = false;
  private ComponentIdentifier identifier;
  private final Map<String, String> parameters = new HashMap<>();
  private final Set<String> schemaValueParameter = new HashSet<>();
  // TODO MULE-9638 This must go away from component model once it's immutable.
  private ComponentModel parent;
  private final List<ComponentModel> innerComponents = new ArrayList<>();
  private String textContent;
  private DefaultComponentLocation componentLocation;
  private TypedComponentIdentifier.ComponentType componentType;
  private org.mule.runtime.api.meta.model.ComponentModel componentModel;
  private NestableElementModel nestableElementModel;
  private ConfigurationModel configurationModel;
  private ConnectionProviderModel connectionProviderModel;
  private MetadataTypeModelAdapter metadataTypeModelAdapter;

  private ComponentMetadataAst componentMetadata;

  private Object objectInstance;
  private Class<?> type;

  /**
   * @return the line number in which the component was defined in the configuration file. It may be empty if the component was
   *         created pragmatically.
   */
  @Deprecated
  public Optional<Integer> getLineNumber() {
    return componentMetadata.getStartLine().isPresent() ? of(componentMetadata.getStartLine().getAsInt()) : empty();
  }

  /**
   * @return the start column in which the component was defined in the configuration file. It may be empty if the component was
   *         created pragmatically.
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
  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * @return a {@code java.util.Map} with the simple parameters of the configuration.
   */
  public Map<String, String> getParameters() {
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

  /**
   * @return true if the {@code ComponentModel} is a top level configuration element, false otherwise.
   */
  public boolean isRoot() {
    return root;
  }

  /**
   * Marked as true if it's a top level configuration.
   */
  public void setRoot(boolean root) {
    this.root = root;
  }

  /**
   * @param parameterName name of the configuration parameter.
   * @param value value contained by the configuration parameter.
   */
  public void setParameter(ParameterModel parameterModel, ComponentParameterAst value) {
    this.parameters.put(parameterModel.getName(), value.getRawValue());
  }

  /**
   * @return the type of the object to be created when processing this {@code ComponentModel}.
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * @param type the type of the object to be created when processing this {@code ComponentModel}.
   */
  public void setType(Class<?> type) {
    this.type = type;
  }

  /**
   * @param componentType the {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType} of the object to be
   *        created when processing this {@code ComponentModel}.
   */
  public void setComponentType(TypedComponentIdentifier.ComponentType componentType) {
    this.componentType = componentType;
  }

  public TypedComponentIdentifier.ComponentType getComponentType() {
    return componentType != null ? componentType : TypedComponentIdentifier.ComponentType.UNKNOWN;
  }

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

  public void resolveTypedComponentIdentifier(ExtensionModelHelper extensionModelHelper) {
    executeOnComponentTree(this, componentModel -> {
      componentModel.doResolveTypedComponentIdentifier(extensionModelHelper);
    });
  }

  private void doResolveTypedComponentIdentifier(ExtensionModelHelper extensionModelHelper) {
    extensionModelHelper.walkToComponent(getIdentifier(), new ExtensionWalkerModelDelegate() {

      @Override
      public void onConfiguration(ConfigurationModel model) {
        setConfigurationModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onConnectionProvider(ConnectionProviderModel model) {
        setConnectionProviderModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onOperation(OperationModel model) {
        setComponentModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onSource(SourceModel model) {
        setComponentModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onConstruct(ConstructModel model) {
        setComponentModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onNestableElement(NestableElementModel model) {
        setNestableElementModel(model);
        if (model instanceof ParameterizedModel) {
          onParameterizedModel((ParameterizedModel) model);
        }
      }

      private void onParameterizedModel(ParameterizedModel model) {
        handleNestedParameters(extensionModelHelper, model);

        ((ComponentAst) ComponentModel.this).recursiveStream()
            .map(childComp -> (ComponentModel) childComp)
            .forEach(childComp -> childComp
                .setMetadataTypeModelAdapter(findParameterModel(extensionModelHelper, model, childComp)));
      }

      private void handleNestedParameters(ExtensionModelHelper extensionModelHelper, ParameterizedModel model) {
        Set<ComponentAst> paramChildren = new HashSet<>();

        ((ComponentAst) ComponentModel.this).recursiveStream()
            .forEach(childComp -> {
              extensionModelHelper.findParameterModel(childComp.getIdentifier(), model)
                  .filter(paramModel -> paramModel.getDslConfiguration().allowsInlineDefinition())
                  .ifPresent(paramModel -> {
                    paramChildren.add(childComp);

                    if (paramModel.getExpressionSupport() == NOT_SUPPORTED) {
                      // TODO MULE-17710 do a recursive navigation to set the field metadata on the inner ComponentAsts of
                      // childComp
                      setParameter(paramModel, new DefaultComponentParameterAst(childComp,
                                                                                () -> paramModel, childComp.getMetadata()));
                    } else {
                      setParameter(paramModel, new DefaultComponentParameterAst(((ComponentModel) childComp).getTextContent(),
                                                                                () -> paramModel, childComp.getMetadata()));
                    }
                  });
            });

        // TODO MULE-17711 When these are removed, the ast parameters may need to be traversed with recursive/direct spliterators
        // ComponentModel.this.innerComponents.removeAll(paramChildren);
      }

      private MetadataTypeModelAdapter findParameterModel(ExtensionModelHelper extensionModelHelper, ParameterizedModel model,
                                                          ComponentModel childComp) {

        return childComp.getModel(MetadataTypeModelAdapter.class)
            .orElseGet(() -> {
              final Optional<? extends MetadataType> childMetadataType =
                  extensionModelHelper.findMetadataType(childComp.getType());
              return childMetadataType
                  .flatMap(type -> createMetadataTypeModelAdapterWithSterotype(type, extensionModelHelper))
                  .orElseGet(() -> childMetadataType
                      .map(type -> createParameterizedTypeModelAdapter(type, extensionModelHelper))
                      .orElse(null));
            });
      };

    });

    // Last resort to try to find a matching metadata type for this component
    if (!getModel(HasStereotypeModel.class).isPresent()) {
      extensionModelHelper.findMetadataType(getType())
          .flatMap(type -> createMetadataTypeModelAdapterWithSterotype(type, extensionModelHelper))
          .ifPresent(this::setMetadataTypeModelAdapter);
    }

    setComponentType(resolveComponentType((ComponentAst) this, extensionModelHelper));
  }

  private void executeOnComponentTree(final ComponentModel component, final Consumer<ComponentModel> task)
      throws MuleRuntimeException {
    task.accept(component);
    component.getInnerComponents().forEach((innerComponent) -> {
      executeOnComponentTree(innerComponent, task);
    });
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
   */
  public String getNameAttribute() {
    return parameters.get(ApplicationModel.NAME_ATTRIBUTE);
  }

  /**
   * @return true if this {@code ComponentModel} represents a {@code org.mule.runtime.core.api.processor.MessageProcessor} scope.
   */
  public boolean isScope() {
    return Router.class.isAssignableFrom(type);
  }

  public void setParent(ComponentModel parent) {
    this.parent = parent;
  }

  /**
   * @return the parent component model in the configuration.
   */
  public ComponentModel getParent() {
    return parent;
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
  public void setComponentLocation(DefaultComponentLocation componentLocation) {
    this.componentLocation = componentLocation;
  }

  /**
   * @return the location of the component in the configuration.
   */
  public DefaultComponentLocation getComponentLocation() {
    return componentLocation;
  }

  /**
   * @return the object instance already created for this model
   */
  public Object getObjectInstance() {
    return objectInstance;
  }

  /**
   * Setter used for components that should be created eagerly without going through spring. This is the case of models
   * contributing to IoC {@link org.mule.runtime.api.ioc.ObjectProvider} interface that require to be created before the
   * application components so they can be referenced.
   *
   * @param objectInstance the object instance created from this model.
   */
  public void setObjectInstance(Object objectInstance) {
    this.objectInstance = objectInstance;
  }

  /**
   * @param parameterName configuration parameter name
   * @return true if the value provided for the configuration parameter was get from the DSL schema, false if it was explicitly
   *         defined in the config
   */
  public boolean isParameterValueProvidedBySchema(String parameterName) {
    return this.schemaValueParameter.contains(parameterName);
  }

  // TODO MULE-11355: Make the ComponentModel haven an ComponentConfiguration internally
  @Deprecated
  public ComponentConfiguration getConfiguration() {
    InternalComponentConfiguration.Builder builder = InternalComponentConfiguration.builder()
        .withIdentifier(this.getIdentifier())
        .withValue(textContent);

    parameters.entrySet().forEach(e -> builder.withParameter(e.getKey(), e.getValue()));
    innerComponents.forEach(i -> builder.withNestedComponent(i.getConfiguration()));
    getMetadata().getParserAttributes().forEach(builder::withProperty);
    builder.withComponentLocation(this.componentLocation);
    builder.withProperty(COMPONENT_MODEL_KEY, this);

    return builder.build();
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

  public ComponentMetadataAst getMetadata() {
    return componentMetadata;
  }

  /**
   * Builder to create instances of {@code ComponentModel}.
   */
  public static class Builder {

    private final ComponentModel model = new SpringComponentModel();
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
      checkIsNotBuildingFromRootComponentModel("identifier");
      this.model.identifier = identifier;
      return this;
    }

    /**
     * @param parameterName name of the configuration parameter.
     * @param value value contained by the configuration parameter.
     * @param valueFromSchema
     * @return the builder.
     */
    public Builder addParameter(String parameterName, String value, boolean valueFromSchema) {
      checkIsNotBuildingFromRootComponentModel("parameters");
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
      checkIsNotBuildingFromRootComponentModel("innerComponents");
      this.model.innerComponents.add(componentModel);
      componentModel.setParent(model);
      return this;
    }

    /**
     * Sets the inner content of the configuration element.
     *
     * @param textContent inner text content from the configuration.
     * @return the builder.
     */
    public Builder setTextContent(String textContent) {
      checkIsNotBuildingFromRootComponentModel("textComponent");
      this.model.textContent = textContent;
      return this;
    }

    /**
     * When invoked the created {@code ComponentModel} will be marked us a top level configuration.
     *
     * @return the builder.
     */
    public Builder markAsRootComponent() {
      checkIsNotBuildingFromRootComponentModel("root");
      this.model.root = true;
      return this;
    }

    /**
     * Adds a custom attribute to the {@code ComponentModel}. This custom attribute is meant to hold metadata of the configuration
     * and not to be used to instantiate the runtime object that corresponds to this configuration.
     *
     * @param name custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(String name, Object value) {
      checkIsNotBuildingFromRootComponentModel("customAttributes");
      return addCustomAttribute(QName.valueOf(name), value);
    }

    /**
     * Adds a custom attribute to the {@code ComponentModel}. This custom attribute is meant to hold metadata of the configuration
     * and not to be used to instantiate the runtime object that corresponds to this configuration.
     *
     * @param name custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(final QName qname, Object value) {
      checkIsNotBuildingFromRootComponentModel("customAttributes");
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
      checkIsNotBuildingFromRootComponentModel("configFileName");
      this.metadataBuilder.setFileName(configFileName);
      return this;
    }

    /**
     * @param lineNumber the line number within the config file in which this component was defined.
     * @return the builder.
     */
    public Builder setLineNumber(int lineNumber) {
      checkIsNotBuildingFromRootComponentModel("lineNumber");
      this.metadataBuilder.setStartLine(lineNumber);
      this.metadataBuilder.setEndLine(lineNumber);
      return this;
    }

    /**
     * @param startColumn the start column within the config file in which this component was defined.
     * @return the builder.
     */
    public Builder setStartColumn(int startColumn) {
      checkIsNotBuildingFromRootComponentModel("startColumn");
      this.metadataBuilder.setStartColumn(startColumn);
      this.metadataBuilder.setEndColumn(startColumn);
      return this;
    }

    /**
     * @param sourceCode the source code associated with this component.
     * @return the builder.
     */
    public Builder setSourceCode(String sourceCode) {
      checkIsNotBuildingFromRootComponentModel("sourceCode");
      this.metadataBuilder.setSourceCode(sourceCode);
      return this;
    }

    /**
     * Given the following root component it will merge its customAttributes, parameters and schemaValueParameters to the root
     * component model.
     *
     * @param otherRootComponentModel another component model created as root to be merged.
     * @return the builder.
     */
    public Builder merge(ComponentModel otherRootComponentModel) {
      ((ComponentAst) otherRootComponentModel).getMetadata().getParserAttributes()
          .forEach((k, v) -> this.metadataBuilder.putParserAttribute(k, v));
      ((ComponentAst) otherRootComponentModel).getMetadata().getDocAttributes()
          .forEach((k, v) -> this.metadataBuilder.putDocAttribute(k, v));
      this.root.parameters.putAll(otherRootComponentModel.parameters);
      this.root.schemaValueParameter.addAll(otherRootComponentModel.schemaValueParameter);

      this.root.innerComponents.addAll(otherRootComponentModel.innerComponents);
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

    private void checkIsNotBuildingFromRootComponentModel(String parameter) {
      checkState(root == null,
                 format("%s cannot be modified when builder has been constructed from a root component", parameter));
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

    if (root != that.root) {
      return false;
    }
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
    int result = (root ? 1 : 0);
    result = 31 * result + Objects.hashCode(componentLocation);
    result = 31 * result + identifier.hashCode();
    result = 31 * result + parameters.hashCode();
    return result;
  }

}
