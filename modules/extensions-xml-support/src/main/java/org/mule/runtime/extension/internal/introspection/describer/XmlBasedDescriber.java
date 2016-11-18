/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.introspection.describer;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.*;
import static org.mule.runtime.dsl.api.component.ComponentIdentifier.Builder;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.model.ComponentModelReader;
import org.mule.runtime.config.spring.dsl.model.extension.xml.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.spring.dsl.model.extension.xml.OperationComponentModelModelProperty;
import org.mule.runtime.config.spring.dsl.model.extension.xml.XmlExtensionModelProperty;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.Describer;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.w3c.dom.Document;

/**
 * Implementation of {@link Describer} which generates a {@link ExtensionDeclarer} by scanning an XML on from a file
 * provided in the constructor.
 *
 * @since 4.0
 */
public class XmlBasedDescriber implements Describer {

  /**
   * The ID which represents {@code this} {@link Describer} that will be used to execute the lookup when reading the descriptor file.
   * @see org.mule.runtime.module.deployment.internal.plugin.json.MulePluginJsonDescriber#getExtensionModelDescriptor()
   */
  public static final String DESCRIBER_ID = "xml-based";

  private static final String PARAMETER_NAME = "name";
  private static final String PARAMETER_DEFAULT_VALUE = "defaultValue";
  private static final String TYPE_ATTRIBUTE = "type";
  private static final String MODULE_NAME = "name";
  private static final String MODULE_NAMESPACE_ATTRIBUTE = "namespace";
  private static final String MODULE_NAMESPACE_NAME = "module";
  protected static final String CONFIG_NAME = "config";

  private static final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private static final Map<String, MetadataType> types = ImmutableMap.<String, MetadataType>builder()
      .put("string", typeLoader.load(String.class))
      .put("boolean", typeLoader.load(Boolean.class))
      .put("datetime", typeLoader.load(Calendar.class))
      .put("date", typeLoader.load(Date.class))
      .put("integer", typeLoader.load(Integer.class))
      .put("time", typeLoader.load(LocalTime.class))
      .build();

  private static final ComponentIdentifier OPERATION_IDENTIFIER =
      new Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("operation").build();
  private static final ComponentIdentifier OPERATION_PROPERTY_IDENTIFIER =
      new Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("property").build();
  private static final ComponentIdentifier OPERATION_PARAMETERS_IDENTIFIER =
      new Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("parameters").build();
  private static final ComponentIdentifier OPERATION_PARAMETER_IDENTIFIER =
      new Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("parameter").build();
  private static final ComponentIdentifier OPERATION_BODY_IDENTIFIER =
      new Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("body").build();
  private static final ComponentIdentifier OPERATION_OUTPUT_IDENTIFIER =
      new Builder().withNamespace(MODULE_NAMESPACE_NAME).withName("output").build();
  private static final ComponentIdentifier MODULE_IDENTIFIER =
      new Builder().withNamespace(MODULE_NAMESPACE_NAME).withName(MODULE_NAMESPACE_NAME)
          .build();
  private static final String SEPARATOR = "/";
  public static final String XSD_SUFFIX = ".xsd";

  private final String modulePath;

  /**
   * @param modulePath relative path to a file that will be loaded from the current {@link ClassLoader}. Non null.
   */
  public XmlBasedDescriber(String modulePath) {
    checkArgument(!isEmpty(modulePath), "modulePath must not be empty");
    this.modulePath = modulePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionDeclarer describe(DescribingContext context) {
    // We will assume the context classLoader of the current thread will be the one defined for the plugin (which is not filtered and will allow us to access any resource in it
    URL resource = currentThread().getContextClassLoader().getResource(modulePath);
    if (resource == null) {
      throw new IllegalArgumentException(format("There's no reachable XML in the path '%s'", modulePath));
    }

    XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader = new XmlConfigurationDocumentLoader();

    Document moduleDocument = getModuleDocument(xmlConfigurationDocumentLoader, resource);
    XmlApplicationParser xmlApplicationParser = new XmlApplicationParser(new SpiServiceRegistry());
    Optional<ConfigLine> parseModule = xmlApplicationParser.parse(moduleDocument.getDocumentElement());
    if (!parseModule.isPresent()) {
      // This happens in org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser.configLineFromElement()
      throw new IllegalArgumentException(format("There was an issue trying to read the stream of '%s'", resource.getFile()));
    }
    ComponentModelReader componentModelReader = new ComponentModelReader(new Properties());
    ComponentModel componentModel =
        componentModelReader.extractComponentDefinitionModel(parseModule.get(), resource.getFile());

    ExtensionDeclarer declarer = context.getExtensionDeclarer();
    loadModuleExtension(declarer, componentModel);
    return declarer;
  }

  private Document getModuleDocument(XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader, URL resource) {
    try {
      return xmlConfigurationDocumentLoader.loadDocument(empty(), resource.openStream());
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("There was an issue reading the stream for the resource %s",
                                                                resource.getFile())));
    }
  }

  private void loadModuleExtension(ExtensionDeclarer declarer, ComponentModel moduleModel) {
    if (!moduleModel.getIdentifier().equals(MODULE_IDENTIFIER)) {
      throw new IllegalArgumentException(format("The root element of a module must be '%s', but found '%s'",
                                                MODULE_IDENTIFIER.toString(), moduleModel.getIdentifier().toString()));
    }
    String name = moduleModel.getParameters().get(MODULE_NAME);
    String namespace = moduleModel.getParameters().get(MODULE_NAMESPACE_ATTRIBUTE);

    String version = "4.0"; // TODO(fernandezlautaro): MULE-11010 add 'from version' to smart extensions
    declarer.named(name)
        .describedAs("Some description")
        .fromVendor("MuleSoft") // TODO(fernandezlautaro): MULE-11010 add 'vendor' to smart extensions
        .onVersion(version)
        .withMinMuleVersion(new MuleVersion("4.0.0")) //this one should be taken from the pom.xml
        .withCategory(Category.COMMUNITY) // TODO(fernandezlautaro): MULE-11010 add 'category' to smart extensions
        .withXmlDsl(XmlDslModel.builder()
            .setSchemaVersion(version)
            .setNamespace(name)
            .setNamespaceUri(namespace)
            .setSchemaLocation(namespace.concat("current" + SEPARATOR).concat(name).concat(XSD_SUFFIX))
            .setXsdFileName(name.concat(XSD_SUFFIX))
            .build());
    declarer.withModelProperty(new XmlExtensionModelProperty());
    loadPropertiesFrom(declarer, moduleModel);
    loadOperationsFrom(declarer, moduleModel);
  }

  private List<ComponentModel> extractGlobalElementsFrom(ComponentModel moduleModel) {
    return moduleModel.getInnerComponents().stream()
        .filter(child -> !child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER)
            && !child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .collect(Collectors.toList());
  }

  private void loadPropertiesFrom(ExtensionDeclarer declarer, ComponentModel moduleModel) {

    List<ComponentModel> globalElementsComponentModel = extractGlobalElementsFrom(moduleModel);

    List<ComponentModel> properties = moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PROPERTY_IDENTIFIER))
        .collect(Collectors.toList());

    if (!properties.isEmpty() || !globalElementsComponentModel.isEmpty()) {
      ConfigurationDeclarer configurationDeclarer = declarer.withConfig(CONFIG_NAME);
      configurationDeclarer.withModelProperty(new GlobalElementComponentModelModelProperty(globalElementsComponentModel));

      properties.stream().forEach(param -> extractParameter(configurationDeclarer, param));
    }
  }

  private void loadOperationsFrom(ExtensionDeclarer declarer, ComponentModel moduleModel) {
    moduleModel.getInnerComponents().stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_IDENTIFIER))
        .forEach(operationModel -> extractOperationExtension(declarer, operationModel));
  }

  private void extractOperationExtension(ExtensionDeclarer declarer, ComponentModel operationModel) {

    String operationName = operationModel.getNameAttribute();
    OperationDeclarer operationDeclarer = declarer.withOperation(operationName);
    ComponentModel bodyComponentModel = operationModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_BODY_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("The operation '%s' is missing the <body> statement",
                                                               operationName)));

    operationDeclarer.withModelProperty(new OperationComponentModelModelProperty(bodyComponentModel));

    extractOperationParameters(operationDeclarer, operationModel);
    extractOutputType(operationDeclarer, operationModel);
  }

  private void extractOperationParameters(OperationDeclarer operationDeclarer, ComponentModel componentModel) {
    Optional<ComponentModel> optionalParametersComponentModel = componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETERS_IDENTIFIER)).findAny();
    if (optionalParametersComponentModel.isPresent()) {
      optionalParametersComponentModel.get().getInnerComponents()
          .stream()
          .filter(child -> child.getIdentifier().equals(OPERATION_PARAMETER_IDENTIFIER))
          .forEach(param -> extractParameter(operationDeclarer, param));
    }
  }

  private void extractParameter(ParameterizedDeclarer parameterizedDeclarer, ComponentModel param) {
    Map<String, String> parameters = param.getParameters();
    String parameterName = parameters.get(PARAMETER_NAME);
    String parameterDefaultValue = parameters.get(PARAMETER_DEFAULT_VALUE);
    MetadataType parameterType = extractParameterType(parameters.get(TYPE_ATTRIBUTE));


    ParameterDeclarer parameterDeclarer =
        parameterDefaultValue == null ? parameterizedDeclarer.withRequiredParameter(parameterName)
            : parameterizedDeclarer.withOptionalParameter(parameterName).defaultingTo(parameterDefaultValue);
    parameterDeclarer.ofType(parameterType);
  }

  private MetadataType extractParameterType(String type) {
    Optional<MetadataType> metadataType = extractType(type);

    if (!metadataType.isPresent()) {
      throw new IllegalArgumentException(String.format(
                                                       "should not have reach here, supported types for <parameter>(simple) are string, boolean, datetime, date, number or time for now. Type obtained [%s]",
                                                       type));
    }
    return metadataType.get();
  }

  private void extractOutputType(OperationDeclarer operationDeclarer, ComponentModel componentModel) {
    ComponentModel outputComponentModel = componentModel.getInnerComponents()
        .stream()
        .filter(child -> child.getIdentifier().equals(OPERATION_OUTPUT_IDENTIFIER)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Having an operation without <output> is not supported"));

    String type = outputComponentModel.getParameters().get(TYPE_ATTRIBUTE);
    Optional<MetadataType> metadataType = extractType(type);

    if (!metadataType.isPresent()) {
      if ("void".equals(type)) {
        metadataType = of(typeLoader.load(Void.class));
      } else {
        throw new IllegalArgumentException(String.format(
                                                         "should not have reach here, supported types for <parameter>(simple) are string, boolean, datetime, date, number or time for now. Type obtained [%s]",
                                                         type));
      }
    }
    operationDeclarer.withOutput().ofType(metadataType.get());
    operationDeclarer.withOutputAttributes().ofType(typeLoader.load(Void.class));
  }

  private Optional<MetadataType> extractType(String type) {
    return Optional.ofNullable(types.get(type));
  }
}
