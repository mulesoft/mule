/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.lang.String.format;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.CONFIG_ATTRIBUTE_DESCRIPTION;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.GROUP_SUFFIX;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MAX_ONE;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_ABSTRACT_OPERATOR;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_MESSAGE_PROCESSOR_TYPE;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.OPERATION_SUBSTITUTION_GROUP_SUFFIX;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.SUBSTITUTABLE_NAME;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.UNBOUNDED;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.internal.property.QNameModelProperty;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ComplexContent;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.GroupRef;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.LocalComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.NamedGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ObjectFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelComplexType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.loader.java.property.TypeRestrictionModelProperty;

import javax.xml.namespace.QName;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base builder delegation class to generate an XSD schema that describes an executable {@link ComponentModel}
 *
 * @since 4.0.0
 */
abstract class ExecutableTypeSchemaDelegate {

  protected final SchemaBuilder builder;
  protected final ObjectFactory objectFactory = new ObjectFactory();
  private final Map<String, NamedGroup> substitutionGroups = new LinkedHashMap<>();
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

  protected ExtensionType registerParameters(ExtensionType type, List<ParameterModel> parameterModels) {
    List<TopLevelElement> childElements = new LinkedList<>();
    parameterModels.forEach(parameter -> {
      DslElementSyntax paramDsl = dsl.resolve(parameter);
      MetadataType parameterType = parameter.getType();

      if (isOperation(parameterType)) {
        String maxOccurs = parameterType instanceof ArrayType ? UNBOUNDED : MAX_ONE;
        childElements.add(generateNestedProcessorElement(paramDsl, parameter, maxOccurs));
      } else {
        boolean shouldDeclare = true;
        if (parameter.getModelProperty(QNameModelProperty.class).isPresent()
            && !parameter.getDslConfiguration().allowsReferences()) {
          shouldDeclare = false;
        }

        if (shouldDeclare) {
          this.builder.declareAsParameter(parameterType, type, parameter, paramDsl, childElements);
        }
      }
    });

    if (!childElements.isEmpty()) {
      if (type.getSequence() == null) {
        final ExplicitGroup all = new ExplicitGroup();
        all.setMinOccurs(ZERO);
        all.setMaxOccurs(MAX_ONE);
        builder.addParameterToSequence(childElements, all);
        type.setSequence(all);

      } else {
        builder.addParameterToSequence(childElements, type.getSequence());
      }
    }

    return type;
  }

  protected void initialiseSequence(ExtensionType operationType) {
    if (operationType.getSequence() == null) {
      ExplicitGroup sequence = new ExplicitGroup();
      sequence.setMinOccurs(ZERO);
      sequence.setMaxOccurs(MAX_ONE);
      operationType.setSequence(sequence);
    }
  }

  private TopLevelElement generateNestedProcessorElement(DslElementSyntax paramDsl, ParameterModel parameterModel,
                                                         String maxOccurs) {
    LocalComplexType collectionComplexType = new LocalComplexType();
    GroupRef group = generateNestedProcessorGroup(parameterModel, maxOccurs);
    collectionComplexType.setGroup(group);
    collectionComplexType.setAnnotation(builder.createDocAnnotation(parameterModel.getDescription()));

    TopLevelElement collectionElement = new TopLevelElement();
    collectionElement.setName(paramDsl.getElementName());
    collectionElement.setMinOccurs(parameterModel.isRequired() ? ONE : ZERO);
    collectionElement.setMaxOccurs(maxOccurs);
    collectionElement.setComplexType(collectionComplexType);
    collectionElement.setAnnotation(builder.createDocAnnotation(EMPTY));

    return collectionElement;
  }

  private GroupRef generateNestedProcessorGroup(ParameterModel parameterModel, String maxOccurs) {
    QName ref = MULE_MESSAGE_PROCESSOR_TYPE;
    TypeRestrictionModelProperty restrictionCapability =
        parameterModel.getModelProperty(TypeRestrictionModelProperty.class).orElse(null);
    if (restrictionCapability != null) {
      ref = getSubstitutionGroup(restrictionCapability.getType());
      ref = new QName(ref.getNamespaceURI(), getGroupName(ref.getLocalPart()), ref.getPrefix());
    }

    GroupRef group = new GroupRef();
    group.setRef(ref);
    group.setMinOccurs(parameterModel.isRequired() ? ONE : ZERO);
    group.setMaxOccurs(maxOccurs);

    return group;
  }

  private boolean isOperation(MetadataType type) {
    if (!type.getMetadataFormat().equals(JAVA)) {
      return false;
    }
    Reference<Boolean> isOperation = new Reference<>(false);
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (NestedProcessor.class.isAssignableFrom(getType(objectType))) {
          isOperation.set(true);
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }
    });

    return isOperation.get();
  }

  protected QName getSubstitutionGroup(Class<?> type) {
    return new QName(builder.getSchema().getTargetNamespace(), registerExtensibleElement(type));
  }

  private String registerExtensibleElement(Class<?> type) {
    Extensible extensible = type.getAnnotation(Extensible.class);
    checkArgument(extensible != null, format("Type %s is not extensible", type.getName()));

    String name = extensible.alias();
    if (StringUtils.isBlank(name)) {
      name = type.getName() + OPERATION_SUBSTITUTION_GROUP_SUFFIX;
    }

    NamedGroup group = substitutionGroups.get(name);
    if (group == null) {
      // register abstract element to serve as substitution
      TopLevelElement element = new TopLevelElement();
      element.setName(name);
      element.setAbstract(true);
      element.setSubstitutionGroup(MULE_ABSTRACT_OPERATOR);
      builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(element);

      group = new NamedGroup();
      group.setName(getGroupName(name));
      builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(group);

      substitutionGroups.put(name, group);

      element = new TopLevelElement();
      element.setRef(new QName(builder.getSchema().getTargetNamespace(), name));
      group.getChoice().getParticle().add(objectFactory.createElement(element));
    }

    return name;
  }

  private String getGroupName(String name) {
    return name + GROUP_SUFFIX;
  }
}
