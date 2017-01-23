/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.mule.runtime.api.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.api.dsl.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.api.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.ComponentIdentifier;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Default implementation of {@link XmlDslElementModelConverter}
 *
 * @since 1.0
 */
public class DefaultXmlDslElementModelConverter implements XmlDslElementModelConverter {

  private static final List<String> INFRASTRUCTURE_NAMES = asList(CONFIG_ATTRIBUTE_NAME,
                                                                  NAME_ATTRIBUTE_NAME,
                                                                  POOLING_PROFILE_ELEMENT_IDENTIFIER,
                                                                  RECONNECT_ELEMENT_IDENTIFIER,
                                                                  RECONNECT_FOREVER_ELEMENT_IDENTIFIER,
                                                                  REDELIVERY_POLICY_ELEMENT_IDENTIFIER,
                                                                  TLS_CONTEXT_ELEMENT_IDENTIFIER,
                                                                  TLS_PARAMETER_NAME,
                                                                  POOLING_PROFILE_PARAMETER_NAME,
                                                                  RECONNECTION_STRATEGY_PARAMETER_NAME,
                                                                  REDELIVERY_POLICY_PARAMETER_NAME,
                                                                  TARGET_PARAMETER_NAME);

  private final Document doc;

  DefaultXmlDslElementModelConverter(Document owner) {
    this.doc = owner;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element asXml(DslElementModel elementModel) {
    Object model = elementModel.getModel();
    checkArgument(model instanceof ConfigurationModel || model instanceof ComponentModel,
                  "The element must be either a ConfigurationModel or a ComponentModel");

    DslElementSyntax dsl = elementModel.getDsl();
    Element componentRoot = createElement(dsl);
    if (!elementModel.getConfiguration().isPresent()) {
      writeDslStructure(componentRoot, dsl);

    } else {
      writeApplicationElement(componentRoot, elementModel, componentRoot);
    }

    return componentRoot;
  }

  private Optional<String> getExplicitParameter(String name, ComponentConfiguration configuration, Object model) {
    return getParameter(name, configuration)
        .filter(value -> getDefaultValue(name, model)
            .map(def -> !value.equals(def)).orElse(true));
  }

  private Optional<String> getDefaultValue(String name, Object model) {
    if (model instanceof ParameterModel) {
      return ExtensionModelUtils.getDefaultValue((ParameterModel) model);
    }

    return ExtensionModelUtils.getDefaultValue(name, (MetadataType) model);
  }

  private Optional<String> getParameter(String name, ComponentConfiguration configuration) {
    String value = configuration.getParameters().get(name);
    if (value != null && !value.trim().isEmpty()) {
      return Optional.of(value);
    }
    return empty();
  }

  private void writeApplicationElement(Element element, DslElementModel<?> elementModel, Element parentNode) {

    if (!elementModel.getConfiguration().isPresent()) {
      return;
    }

    ComponentConfiguration config = elementModel.getConfiguration().get();

    populateInfrastructureConfiguration(element, config);

    if (elementModel.getContainedElements().isEmpty() && hasPopulatedParameters(config)) {
      populateTypeBasedOnDsl(element, config, elementModel.getDsl(), elementModel.getModel(), parentNode);
    }

    if (isTextContent(config)) {
      addTextContentParameter(element, elementModel.getDsl().getAttributeName(), elementModel.getModel(), parentNode, config);
      return;
    }

    elementModel.getContainedElements().stream()
        .filter(c -> !isInfrastructure(c.getDsl().getAttributeName()) && !isInfrastructure(c.getDsl().getElementName()))
        .forEach(inner -> {

          DslElementSyntax innerDsl = inner.getDsl();
          Reference<Boolean> configured = new Reference<>(false);
          if (innerDsl.supportsAttributeDeclaration()) {
            getExplicitParameter(innerDsl.getAttributeName(), config, inner.getModel())
                .ifPresent(value -> {
                  configured.set(true);
                  element.setAttribute(innerDsl.getAttributeName(), value);
                });
          }

          if (!configured.get() && innerDsl.supportsChildDeclaration()) {
            Element childElement = createElement(innerDsl);
            writeApplicationElement(childElement, inner, element);
          }
        });

    if (parentNode != element) {
      parentNode.appendChild(element);
    }
  }

  private void addTextContentParameter(Element element, String name, Object model, Element parentNode,
                                       ComponentConfiguration config) {
    Optional<String> defaultValue = getDefaultValue(name, model);
    if (!defaultValue.isPresent() || !defaultValue.get().equals(config.getValue().get())) {
      element.setTextContent(config.getValue().get());
      if (parentNode != element) {
        parentNode.appendChild(element);
      }
    }
  }

  private boolean hasPopulatedParameters(ComponentConfiguration config) {
    return !(config.getParameters().isEmpty() && config.getNestedComponents().isEmpty());
  }

  private void populateTypeBasedOnDsl(Element element, ComponentConfiguration config, DslElementSyntax dsl,
                                      Object model, Element parentNode) {

    final MetadataType type = model instanceof ObjectType ? (MetadataType) model : ((Typed) model).getType();

    dsl.getAttributes()
        .forEach(attr -> getExplicitParameter(attr.getAttributeName(), config, type)
            .ifPresent(value -> element.setAttribute(attr.getAttributeName(), value)));

    if (isTextContent(config)) {
      addTextContentParameter(element, dsl.getAttributeName(), type, parentNode, config);
      return;
    }

    config.getNestedComponents().forEach(component -> {
      ComponentIdentifier identifier = component.getIdentifier();
      Optional<DslElementSyntax> childDsl = findMatchingDsl(dsl.getChilds(), identifier);
      if (!childDsl.isPresent()) {
        childDsl = findMatchingDsl(dsl.getGenerics().values(), identifier);
      }

      childDsl.ifPresent(innerDsl -> {
        Element childElement = createElement(innerDsl);
        populateTypeBasedOnDsl(childElement, component, innerDsl, type, element);
      });
    });

    if (parentNode != element) {
      parentNode.appendChild(element);
    }
  }

  private boolean isTextContent(ComponentConfiguration config) {
    return config.getNestedComponents().isEmpty() && config.getValue().isPresent();
  }

  private Optional<DslElementSyntax> findMatchingDsl(Collection<DslElementSyntax> dsl, ComponentIdentifier identifier) {
    return dsl.stream()
        .filter(child -> (child.supportsTopLevelDeclaration() || child.supportsChildDeclaration()) &&
            ComponentIdentifier.builder()
                .withName(child.getElementName())
                .withNamespace(child.getNamespaceUri()).build()
                .equals(identifier))
        .findFirst();
  }

  private Element writeDslStructure(Element element, DslElementSyntax dsl) {
    dsl.getAttributes().forEach(a -> element.setAttribute(a.getAttributeName(), ""));
    dsl.getChilds().forEach(current -> {
      Element childElement = createElement(current);
      element.appendChild(writeDslStructure(childElement, current));
    });

    dsl.getGenerics().forEach((type, generic) -> {
      Element childElement = createElement(generic);
      element.appendChild(writeDslStructure(childElement, generic));
    });

    return element;
  }

  private Element createElement(DslElementSyntax dsl) {
    doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/",
                                            "xmlns:" + dsl.getNamespace(), dsl.getNamespaceUri());

    return doc.createElementNS(dsl.getNamespaceUri(), dsl.getNamespace() + ":" + dsl.getElementName());
  }

  private boolean isInfrastructure(String name) {
    return INFRASTRUCTURE_NAMES.contains(name);
  }

  private void populateInfrastructureConfiguration(Element element, ComponentConfiguration configuration) {
    configuration.getParameters().entrySet().stream()
        .filter(attribute -> INFRASTRUCTURE_NAMES.contains(attribute.getKey()))
        .forEach(attribute -> element.setAttribute(attribute.getKey(), attribute.getValue()));

    configuration.getNestedComponents().stream()
        .filter(c -> INFRASTRUCTURE_NAMES.contains(c.getIdentifier().getName()))
        .forEach(c -> {
          Element nested;
          if (c.getIdentifier().getNamespace().contains("tls")) {
            doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tls",
                                                    "http://www.mulesoft.org/schema/mule/tls");
            nested = createTLS(c);
          } else {
            nested = clone(c);
          }
          element.appendChild(nested);
        });
  }

  private Element createTLS(ComponentConfiguration config) {
    Element nested;
    nested = doc.createElementNS("http://www.mulesoft.org/schema/mule/tls",
                                 "tls:" + config.getIdentifier().getName());
    config.getParameters().forEach(nested::setAttribute);
    config.getNestedComponents().forEach(inner -> nested.appendChild(createTLS(inner)));
    return nested;
  }

  private Element clone(ComponentConfiguration config) {
    Element element = doc.createElement(config.getIdentifier().getName());
    config.getParameters().forEach(element::setAttribute);
    config.getNestedComponents().forEach(nested -> element.appendChild(clone(nested)));
    return element;
  }

}
