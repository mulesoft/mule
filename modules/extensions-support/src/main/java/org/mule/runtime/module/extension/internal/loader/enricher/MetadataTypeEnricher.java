/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.stream.Collectors.toMap;

import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.TypeBuilder;
import org.mule.metadata.api.builder.WithAnnotation;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.AttributeFieldType;
import org.mule.metadata.api.model.AttributeKeyType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.FunctionType;
import org.mule.metadata.api.model.LocalDateTimeType;
import org.mule.metadata.api.model.LocalTimeType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NothingType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectKeyType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.PeriodType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.TimeType;
import org.mule.metadata.api.model.TimeZoneType;
import org.mule.metadata.api.model.TupleType;
import org.mule.metadata.api.model.TypeParameterType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.model.impl.DefaultAttributeFieldType;
import org.mule.metadata.api.model.impl.DefaultAttributeKeyType;
import org.mule.metadata.api.model.impl.DefaultFunctionType;
import org.mule.metadata.api.model.impl.DefaultObjectFieldType;
import org.mule.metadata.api.model.impl.DefaultObjectKeyType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultUnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Gives the capability to enrich a {@link MetadataType} with more {@link TypeAnnotation}s.
 *
 * @since 1.1
 */
class MetadataTypeEnricher {

  public MetadataType enrich(MetadataType target, Set<TypeAnnotation> annotations) {
    TypeEnricherVisitor visitor = new TypeEnricherVisitor(target, annotations);
    target.accept(visitor);
    return visitor.type;
  }

  private static class TypeEnricherVisitor extends MetadataTypeVisitor {

    private MetadataType type;
    private final MetadataType target;
    private final BaseTypeBuilder typeBuilder;
    private final Set<TypeAnnotation> annotations;

    private TypeEnricherVisitor(MetadataType target, Set<TypeAnnotation> annotations) {
      this.target = target;
      this.annotations = annotations;
      this.typeBuilder = BaseTypeBuilder.create(target.getMetadataFormat());
    }

    @Override
    protected void defaultVisit(MetadataType metadataType) {
      type = metadataType;
    }

    @Override
    public void visitAnyType(AnyType anyType) {
      type = withNewAnnotations(typeBuilder.anyType());
    }

    @Override
    public void visitArrayType(ArrayType arrayType) {
      type = withNewAnnotations(typeBuilder.arrayType().of(arrayType.getType()));
    }

    @Override
    public void visitBinaryType(BinaryType binaryType) {
      type = withNewAnnotations(typeBuilder.binaryType());
    }

    @Override
    public void visitBoolean(BooleanType booleanType) {
      type = withNewAnnotations(typeBuilder.booleanType());
    }

    @Override
    public void visitDateTime(DateTimeType dateTimeType) {
      type = withNewAnnotations(typeBuilder.dateTimeType());
    }

    @Override
    public void visitDate(DateType dateType) {
      type = withNewAnnotations(typeBuilder.dateType());
    }

    @Override
    public void visitNumber(NumberType numberType) {
      withNewAnnotations(typeBuilder.numberType());
    }

    @Override
    public void visitObject(ObjectType objectType) {
      type = new DefaultObjectType(objectType.getFields(),
                                   objectType.isOrdered(),
                                   objectType.getOpenRestriction().orElse(null),
                                   objectType.getMetadataFormat(),
                                   getAllTypeAnnotationsMap());
    }

    @Override
    public void visitString(StringType stringType) {
      type = withNewAnnotations(typeBuilder.stringType());
    }

    @Override
    public void visitTime(TimeType timeType) {
      type = withNewAnnotations(typeBuilder.timeType());
    }

    @Override
    public void visitTuple(TupleType tupleType) {
      type = withNewAnnotations(typeBuilder.tupleType());
    }

    @Override
    public void visitUnion(UnionType unionType) {
      type = new DefaultUnionType(unionType.getTypes(), unionType.getMetadataFormat(), getAllTypeAnnotationsMap());
    }

    @Override
    public void visitObjectKey(ObjectKeyType type) {
      type = new DefaultObjectKeyType(Optional.ofNullable(type.getName()),
                                      Optional.ofNullable(type.getPattern()),
                                      type.getAttributes(),
                                      type.getMetadataFormat(),
                                      getAllTypeAnnotationsMap());
    }

    @Override
    public void visitAttributeKey(AttributeKeyType type) {
      type = new DefaultAttributeKeyType(Optional.ofNullable(type.getName()),
                                         Optional.ofNullable(type.getPattern()),
                                         type.getMetadataFormat(),
                                         getAllTypeAnnotationsMap());
    }

    @Override
    public void visitAttributeField(AttributeFieldType type) {
      type = new DefaultAttributeFieldType(type.getKey(), type.getValue(), type.isRequired(),
                                           type.getMetadataFormat(), getAllTypeAnnotationsMap());
    }

    @Override
    public void visitObjectField(ObjectFieldType type) {
      type = new DefaultObjectFieldType(type.getKey(), type.getValue(), type.isRequired(), type.isRepeated(),
                                        type.getMetadataFormat(), getAllTypeAnnotationsMap());
    }

    @Override
    public void visitNothing(NothingType nothingType) {
      type = withNewAnnotations(typeBuilder.nothingType());
    }

    @Override
    public void visitFunction(FunctionType type) {
      type = new DefaultFunctionType(type.getMetadataFormat(), getAllTypeAnnotationsMap(),
                                     type.getReturnType(), type.getParameters());
    }

    @Override
    public void visitLocalDateTime(LocalDateTimeType localDateTimeType) {
      type = withNewAnnotations(typeBuilder.localDateTimeType());
    }

    @Override
    public void visitLocalTime(LocalTimeType localTimeType) {
      type = withNewAnnotations(typeBuilder.localTimeType());
    }

    @Override
    public void visitPeriod(PeriodType periodType) {
      type = withNewAnnotations(typeBuilder.localTimeType());
    }

    @Override
    public void visitTimeZone(TimeZoneType timeZoneType) {
      type = withNewAnnotations(typeBuilder.timeZoneType());
    }

    @Override
    public void visitTypeParameter(TypeParameterType defaultTypeParameter) {
      type = withNewAnnotations(typeBuilder.typeParameter(defaultTypeParameter.getName()));
    }

    private <T extends TypeBuilder & WithAnnotation> MetadataType withNewAnnotations(T builder) {
      Set<TypeAnnotation> all = getAllTypeAnnotations();
      for (TypeAnnotation typeAnnotation : all) {
        builder.with(typeAnnotation);
      }
      return builder.build();
    }

    private Map<Class<? extends TypeAnnotation>, TypeAnnotation> getAllTypeAnnotationsMap() {
      return getAllTypeAnnotations().stream().collect(toMap(TypeAnnotation::getClass, a -> a));
    }

    private Set<TypeAnnotation> getAllTypeAnnotations() {
      Set<TypeAnnotation> all = new HashSet<>(annotations);
      all.addAll(target.getAnnotations());
      return all;
    }
  }
}
