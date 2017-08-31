/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.CONFIG_ATTRIBUTE_DESCRIPTION;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.MULE_ABSTRACT_MESSAGE_SOURCE;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.MULE_ABSTRACT_OPERATOR;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.MULE_ABSTRACT_VALIDATOR;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.MULE_MESSAGE_PROCESSOR_TYPE;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.SUBSTITUTABLE_NAME;
import static org.mule.runtime.config.spring.internal.dsl.SchemaConstants.UNBOUNDED;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisitor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.stereotype.MuleStereotypeFactory;
import org.mule.runtime.extension.internal.property.QNameModelProperty;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NamedGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Base builder delegation class to generate an XSD schema that describes an executable {@link ComponentModel}
 *
 * @since 4.0.0
 */
abstract class ExecutableTypeSchemaDelegate {

  protected final SchemaBuilder builder;
  protected final ObjectFactory objectFactory = new ObjectFactory();
  private final Map<String, TopLevelElement> substitutionGroups = new LinkedHashMap<>();
  private final DslSyntaxResolver dsl;

  ExecutableTypeSchemaDelegate(SchemaBuilder builder) {
    this.builder = builder;
    this.dsl = builder.getDslResolver();
  }

  protected ExtensionType createExecutableType(String name, QName base, DslElementSyntax dslSyntax) {
    TopLevelComplexType complexType = new TopLevelComplexType();
    complexType.setName(name);

    ComplexContent complexContent = new ComplexContent();
    complexType.setComplexContent(complexContent);
    final ExtensionType complexContentExtension = new ExtensionType();
    complexContentExtension.setBase(base);
    complexContent.setExtension(complexContentExtension);

    if (dslSyntax.requiresConfig()) {
      Attribute configAttr =
          builder.createAttribute(CONFIG_ATTRIBUTE_NAME, CONFIG_ATTRIBUTE_DESCRIPTION, true, SUBSTITUTABLE_NAME);
      complexContentExtension.getAttributeOrAttributeGroup().add(configAttr);
    }

    this.builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(complexType);
    return complexContentExtension;
  }

  protected void registerParameterGroup(ExtensionType type, ParameterGroupModel group) {
    if (group.isShowInDsl()) {
      builder.addInlineParameterGroup(group, type.getSequence());
    } else {
      registerParameters(type, group.getParameterModels());
    }
  }

  protected ExtensionType registerParameters(ExtensionType type, List<ParameterModel> parameterModels) {
    List<TopLevelElement> childElements = new LinkedList<>();
    parameterModels.forEach(parameter -> {
      DslElementSyntax paramDsl = dsl.resolve(parameter);
      MetadataType parameterType = parameter.getType();

      boolean shouldDeclare = true;
      if (parameter.getModelProperty(QNameModelProperty.class).isPresent()
          && !parameter.getDslConfiguration().allowsReferences()) {
        shouldDeclare = false;
      }

      if (shouldDeclare) {
        this.builder.declareAsParameter(parameterType, type, parameter, paramDsl, childElements);
      }
    });

    appendToSequence(type, childElements);

    return type;
  }

  protected ExtensionType registerNestedComponents(ExtensionType type, List<? extends NestableElementModel> nestedComponents) {
    List<TopLevelElement> childElements = new LinkedList<>();
    nestedComponents.forEach(component -> component.accept(new NestableElementModelVisitor() {

      @Override
      public void visit(NestedComponentModel component) {}

      @Override
      public void visit(NestedChainModel component) {
        childElements.add(generateNestedProcessorElement(dsl.resolve(component), component));
      }

      @Override
      public void visit(NestedRouteModel component) {}
    }));

    appendToSequence(type, childElements);
    return type;
  }

  private void appendToSequence(ExtensionType type, List<TopLevelElement> childElements) {
    if (!childElements.isEmpty()) {
      initialiseSequence(type);
      builder.addParameterToSequence(childElements, type.getSequence());
    }
  }


  protected void initialiseSequence(ExtensionType type) {
    if (type.getSequence() == null) {
      ExplicitGroup sequence = new ExplicitGroup();
      sequence.setMinOccurs(ZERO);
      sequence.setMaxOccurs(MAX_ONE);
      type.setSequence(sequence);
    }
  }

  private TopLevelElement generateNestedProcessorElement(DslElementSyntax paramDsl, NestedChainModel chainModel) {
    TopLevelElement collectionElement = new TopLevelElement();
    collectionElement.setName(paramDsl.getElementName());
    collectionElement.setMinOccurs(chainModel.isRequired() ? ONE : ZERO);
    collectionElement.setAnnotation(builder.createDocAnnotation(EMPTY));

    LocalComplexType complexType = new LocalComplexType();
    final ExplicitGroup choice = new ExplicitGroup();
    chainModel.getAllowedStereotypes().forEach(stereotype -> {
      // We need this to support both message-processor and mixed-content-message-processor
      if (stereotype.equals(MuleStereotypeFactory.processor())) {
        NamedGroup group = builder.createGroup(MULE_MESSAGE_PROCESSOR_TYPE, true);
        choice.getParticle().add(objectFactory.createGroup(group));
      } else {
        TopLevelElement localAbstractElementRef = builder.createRefElement(getSubstitutionGroup(stereotype), true);
        choice.getParticle().add(objectFactory.createElement(localAbstractElementRef));
      }
    });

    ExplicitGroup sequence = new ExplicitGroup();
    sequence.setMinOccurs(ONE);
    sequence.setMaxOccurs(UNBOUNDED);
    sequence.getParticle().add(objectFactory.createChoice(choice));
    complexType.setSequence(sequence);
    collectionElement.setComplexType(complexType);
    return collectionElement;
  }

  protected QName getSubstitutionGroup(StereotypeModel stereotypeDefinition) {
    if (stereotypeDefinition.equals(MuleStereotypeFactory.processor())) {
      return MULE_ABSTRACT_OPERATOR;
    }

    if (stereotypeDefinition.equals(MuleStereotypeFactory.source())) {
      return MULE_ABSTRACT_MESSAGE_SOURCE;
    }

    if (stereotypeDefinition.equals(MuleStereotypeFactory.validator())) {
      return MULE_ABSTRACT_VALIDATOR;
    }

    return new QName(builder.getSchema().getTargetNamespace(), registerExtensibleElement(stereotypeDefinition));
  }

  private String registerExtensibleElement(StereotypeModel stereotypeModel) {
    final String name = hyphenize(stereotypeModel.getName()).toLowerCase();

    TopLevelElement group = substitutionGroups.get(name);
    if (group == null) {
      // register abstract element to serve as substitution
      final TopLevelElement element = new TopLevelElement();
      element.setName(name);
      element.setAbstract(true);

      stereotypeModel.getParent()
          .ifPresent(parent -> element.setSubstitutionGroup(getSubstitutionGroup(parent)));

      builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(element);
      substitutionGroups.put(name, element);
    }

    return name;
  }
}
