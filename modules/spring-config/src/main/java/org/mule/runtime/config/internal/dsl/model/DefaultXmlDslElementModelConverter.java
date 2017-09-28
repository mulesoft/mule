/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.internal.dsl.declaration.DefaultXmlArtifactDeclarationLoader.TRANSFORM_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler.DECLARED_PREFIX;
import static org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler.IS_CDATA;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECTION_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_PREFIX;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.XmlDslElementModelConverter;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Default implementation of {@link XmlDslElementModelConverter}
 *
 * @since 4.0
 */
public class DefaultXmlDslElementModelConverter implements XmlDslElementModelConverter {

  private static final List<String> INFRASTRUCTURE_NAMES = asList(CONFIG_ATTRIBUTE_NAME,
                                                                  NAME_ATTRIBUTE_NAME,
                                                                  POOLING_PROFILE_ELEMENT_IDENTIFIER,
                                                                  RECONNECTION_ELEMENT_IDENTIFIER,
                                                                  RECONNECT_FOREVER_ELEMENT_IDENTIFIER,
                                                                  REDELIVERY_POLICY_ELEMENT_IDENTIFIER,
                                                                  TLS_CONTEXT_ELEMENT_IDENTIFIER,
                                                                  TLS_PARAMETER_NAME,
                                                                  POOLING_PROFILE_PARAMETER_NAME,
                                                                  RECONNECTION_STRATEGY_PARAMETER_NAME,
                                                                  REDELIVERY_POLICY_PARAMETER_NAME,
                                                                  TARGET_PARAMETER_NAME);
  private static final String XMLNS_ATTRIBUTE_NAMESPACE = "http://www.w3.org/2000/xmlns/";
  private static final String XMLNS = "xmlns:";

  private final Document doc;

  public DefaultXmlDslElementModelConverter(Document owner) {
    this.doc = owner;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element asXml(DslElementModel elementModel) {
    Object model = elementModel.getModel();
    checkArgument(model instanceof ConfigurationModel || model instanceof ComponentModel || model instanceof MetadataType,
                  "The element must be either a MetadataType, ConfigurationModel or a ComponentModel");

    DslElementSyntax dsl = elementModel.getDsl();
    Element componentRoot = createElement(dsl, elementModel.getConfiguration());
    if (isEETransform(componentRoot)) {
      return populateEETransform(elementModel);
    }
    writeApplicationElement(componentRoot, elementModel, componentRoot);

    return componentRoot;
  }

  private String getPrefix(DslElementSyntax dsl, ComponentConfiguration configuration) {
    return configuration.getProperty(DECLARED_PREFIX).isPresent()
        ? configuration.getProperty(DECLARED_PREFIX).get().toString()
        : dsl.getPrefix();
  }

  private void writeApplicationElement(Element element, DslElementModel<?> elementModel, Element parentNode) {
    populateInfrastructureConfiguration(element, elementModel);

    if (elementModel.getContainedElements().isEmpty() && elementModel.getValue().isPresent()) {
      setTextContentElement(element, elementModel, parentNode);
      return;
    }

    elementModel.getContainedElements().stream()
        .filter(c -> !isInfrastructure(c))
        .forEach(inner -> {
          DslElementSyntax innerDsl = inner.getDsl();
          Reference<Boolean> configured = new Reference<>(false);
          if (innerDsl.supportsAttributeDeclaration() && inner.getValue().isPresent()) {
            getCustomizedValue(inner).ifPresent(value -> {
              configured.set(true);
              element.setAttribute(innerDsl.getAttributeName(), value);
            });
          }

          if (!configured.get() && innerDsl.supportsChildDeclaration() && inner.isExplicitInDsl()) {
            Element childElement = createElement(innerDsl, inner.getConfiguration());
            if (isEETransform(childElement)) {
              element.appendChild(populateEETransform(inner));
            } else {
              writeApplicationElement(childElement, inner, element);
            }
          }
        });

    if (parentNode != element) {
      parentNode.appendChild(element);
    }
  }

  private boolean isEETransform(Element parentNode) {
    return parentNode.getNamespaceURI().equals(EE_NAMESPACE)
        && parentNode.getNodeName().equals(EE_PREFIX + ":" + TRANSFORM_IDENTIFIER);
  }

  private Element createElement(DslElementSyntax dsl, Optional<ComponentConfiguration> configuration) {
    return configuration.isPresent()
        ? createElement(dsl.getElementName(), getPrefix(dsl, configuration.get()), dsl.getNamespace())
        : createElement(dsl);
  }

  private void setTextContentElement(Element element, DslElementModel<?> elementModel, Element parentNode) {
    getCustomizedValue(elementModel).ifPresent(value -> {
      DslElementSyntax dsl = elementModel.getDsl();
      if (dsl.supportsChildDeclaration() && !dsl.supportsAttributeDeclaration()) {
        if (elementModel.getConfiguration().map(c -> c.getProperty(IS_CDATA).isPresent()).orElse(false)) {
          element.appendChild(doc.createCDATASection(value));
        } else {
          element.setTextContent(value);
        }

        if (parentNode != element) {
          parentNode.appendChild(element);
        }
      } else {
        parentNode.setAttribute(dsl.getAttributeName(), value);
      }
    });
  }

  private Optional<String> getCustomizedValue(DslElementModel elementModel) {
    String value = (String) elementModel.getValue().get();
    if (elementModel.isExplicitInDsl()) {
      return Optional.of(value);
    }

    Optional<String> defaultValue = getDefaultValue(elementModel.getDsl().getAttributeName(), elementModel.getModel());
    if (!defaultValue.isPresent() || !defaultValue.get().equals(value)) {
      return Optional.of(value);
    }
    return empty();
  }

  private Optional<String> getDefaultValue(String name, Object model) {
    if (model instanceof ParameterModel) {
      return ExtensionModelUtils.getDefaultValue((ParameterModel) model);
    }

    return ExtensionModelUtils.getDefaultValue(name, (MetadataType) model);
  }

  private Element createElement(DslElementSyntax dsl) {
    return createElement(dsl.getElementName(), dsl.getPrefix(), dsl.getNamespace());
  }

  private Element createElement(DslElementSyntax dsl, String name) {
    return createElement(name, dsl.getPrefix(), dsl.getNamespace());
  }

  private Element createElement(String name, String prefix, String namespace) {
    if (!prefix.equals(CORE_PREFIX)) {
      addNamespaceDeclarationIfNeeded(prefix, namespace, buildSchemaLocation(prefix, namespace));
      return doc.createElementNS(namespace, prefix + ":" + name);
    } else {
      // core schema location will always be included
      doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/",
                                              "xmlns", CORE_NAMESPACE);
      return doc.createElementNS(CORE_NAMESPACE, name);
    }
  }

  private boolean isInfrastructure(DslElementModel elementModel) {
    Object model = elementModel.getModel();
    if (model instanceof ParameterModel) {
      return ExtensionModelUtils.isInfrastructure((ParameterModel) model);
    }

    return INFRASTRUCTURE_NAMES.contains(elementModel.getDsl().getAttributeName()) ||
        INFRASTRUCTURE_NAMES.contains(elementModel.getDsl().getElementName());
  }

  private void populateInfrastructureConfiguration(Element element, DslElementModel<?> elementModel) {
    elementModel.getContainedElements().stream()
        .filter(this::isInfrastructure)
        .forEach(e -> {
          if (e.getContainedElements().isEmpty() && e.getValue().isPresent()) {
            element.setAttribute(e.getDsl().getAttributeName(), (String) e.getValue().get());
          } else {
            Optional<ComponentConfiguration> config = e.getConfiguration();
            config.ifPresent(c -> {
              if (c.getIdentifier().getNamespace().contains(TLS_PREFIX)) {
                element.appendChild(createTLS(c));
              } else if (c.getIdentifier().getNamespace().contains(EE_PREFIX)) {
                element.appendChild(createEE(c));
              } else {
                element.appendChild(clone(c));
              }
            });
          }
        });

    elementModel.getConfiguration()
        .ifPresent(c -> of(NAME_ATTRIBUTE_NAME, CONFIG_ATTRIBUTE_NAME)
            .forEach(name -> {
              String value = c.getParameters().get(name);
              if (!isBlank(value)) {
                element.setAttribute(name, value);
              }
            }));
  }

  private Element populateEETransform(DslElementModel<?> elementModel) {
    Element transform = createElement(elementModel.getDsl());
    elementModel.getConfiguration()
        .ifPresent(c -> c.getParameters()
            .forEach((name, value) -> elementModel.findElement(name).filter(DslElementModel::isExplicitInDsl)
                .ifPresent(e -> transform.setAttribute(name, value))));

    // write set-payload and set-attributes
    elementModel.findElement(buildFromStringRepresentation("ee:set-payload"))
        .filter(DslElementModel::isExplicitInDsl)
        .ifPresent(message -> {
          Element messageElement = createElement(elementModel.getDsl(), "message");
          transform.appendChild(messageElement);

          elementModel.findElement(buildFromStringRepresentation("ee:set-payload"))
              .ifPresent(setPayload -> setPayload.getConfiguration()
                  .ifPresent(c -> messageElement.appendChild(createTransformTextElement(c))));

          elementModel.findElement(buildFromStringRepresentation("ee:set-attributes"))
              .ifPresent(setAttributes -> setAttributes.getConfiguration()
                  .ifPresent(c -> messageElement.appendChild(createTransformTextElement(c))));

        });

    // write set-variable
    elementModel.findElement(buildFromStringRepresentation("ee:set-variables"))
        .ifPresent(variables -> {
          Element variablesList = createElement(elementModel.getDsl(), "variables");
          transform.appendChild(variablesList);

          variables.getContainedElements()
              .forEach(variable -> variable.getConfiguration().ifPresent(c -> {
                Element var = createTransformTextElement((ComponentConfiguration) c);
                var.setAttribute("variableName", ((ComponentConfiguration) c).getParameters().get("variableName"));
                variablesList.appendChild(var);
              }));
        });

    return transform;
  }

  private Element createTLS(ComponentConfiguration config) {
    String namespaceURI = "http://www.mulesoft.org/schema/mule/tls";
    String tlsSchemaLocation = "http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd";

    addNamespaceDeclarationIfNeeded(TLS_PREFIX, namespaceURI, tlsSchemaLocation);

    Element nested = doc.createElementNS(namespaceURI, TLS_PREFIX + ":" + config.getIdentifier().getName());
    config.getParameters().forEach(nested::setAttribute);
    config.getNestedComponents().forEach(inner -> nested.appendChild(createTLS(inner)));
    return nested;
  }

  private Element createEE(ComponentConfiguration config) {
    String namespaceURI = EE_NAMESPACE;
    String eeSchemaLocation = buildSchemaLocation(EE_PREFIX, EE_NAMESPACE);

    addNamespaceDeclarationIfNeeded(EE_PREFIX, namespaceURI, eeSchemaLocation);

    Element nested = doc.createElementNS(namespaceURI, EE_PREFIX + ":" + config.getIdentifier().getName());
    config.getParameters().forEach(nested::setAttribute);
    config.getNestedComponents().forEach(inner -> nested.appendChild(clone(inner)));
    return nested;
  }

  private Element createTransformTextElement(ComponentConfiguration config) {
    String namespaceURI = EE_NAMESPACE;
    String eeSchemaLocation = buildSchemaLocation(EE_PREFIX, EE_NAMESPACE);

    addNamespaceDeclarationIfNeeded(EE_PREFIX, namespaceURI, eeSchemaLocation);

    Element nested = doc.createElementNS(namespaceURI, EE_PREFIX + ":" + config.getIdentifier().getName());
    config.getParameters().forEach(nested::setAttribute);
    config.getNestedComponents().stream()
        .filter(inner -> inner.getValue().isPresent())
        .forEach(inner -> nested.appendChild(doc.createCDATASection(inner.getValue().get())));
    return nested;
  }

  private void addNamespaceDeclarationIfNeeded(String prefix, String namespaceURI, String schemaLocation) {
    if (isBlank(doc.getDocumentElement().getAttributeNS(XMLNS_ATTRIBUTE_NAMESPACE, XMLNS + prefix))) {
      doc.getDocumentElement().setAttributeNS(XMLNS_ATTRIBUTE_NAMESPACE, XMLNS + prefix, namespaceURI);
      addSchemaLocationIfNeeded(namespaceURI, schemaLocation);
    }
  }

  private void addSchemaLocationIfNeeded(String namespaceURI, String schemaLocation) {
    Attr schemaLocationAttribute = doc.getDocumentElement().getAttributeNode("xsi:schemaLocation");
    if (schemaLocationAttribute != null && !schemaLocationAttribute.getValue().contains(namespaceURI)) {
      doc.getDocumentElement().setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                                              "xsi:schemaLocation",
                                              schemaLocationAttribute.getValue() + " " + namespaceURI + " " + schemaLocation);
    }
  }

  private Element clone(ComponentConfiguration config) {
    Element element = doc.createElement(config.getIdentifier().getName());
    config.getParameters().forEach(element::setAttribute);
    config.getNestedComponents().forEach(nested -> element.appendChild(clone(nested)));
    return element;
  }

}
