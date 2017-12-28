/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor.TypeIntrospectionResult.builder;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.ast.api.ASTTypeLoader;
import org.mule.metadata.ast.internal.ClassInformationAnnotationFactory;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor.MuleTypeVisitor;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor.TypeIntrospectionResult;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * {@link Type} implementation which uses {@link TypeMirror} and {@link TypeElement} to represent a Java Class.
 *
 * @since 4.1
 */
public class ASTType implements Type {

  private final TypeMirror typeMirror;
  private final List<TypeGeneric> typeGenerics;
  private LazyValue<List<OperationElement>> methods;
  private LazyValue<ClassInformationAnnotation> classInformation;
  final ProcessingEnvironment processingEnvironment;
  final TypeElement typeElement;
  ASTUtils astUtils;
  private ASTTypeLoader typeLoader;

  /**
   * Creates a new {@link ASTType} based on a {@link TypeMirror}.
   * @param typeMirror
   * @param processingEnvironment
   */
  public ASTType(TypeMirror typeMirror, ProcessingEnvironment processingEnvironment) {

    this.processingEnvironment = processingEnvironment;
    TypeIntrospectionResult accept = typeMirror.accept(new MuleTypeVisitor(processingEnvironment), builder());
    this.typeElement = accept.getConcreteType();
    this.typeGenerics = toTypeGenerics(accept, processingEnvironment);
    this.typeMirror = typeMirror;
    init();
  }

  public ASTType(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
    this.typeElement = typeElement;
    this.typeMirror = typeElement.asType();
    this.typeGenerics = emptyList();
    init();
  }

  private void init() {
    typeLoader =
        new ASTTypeLoader(processingEnvironment, Collections.singletonList(new ExtensionTypeHandler(processingEnvironment)));
    astUtils = new ASTUtils(processingEnvironment);
    methods = new LazyValue<>(() -> getApiMethods(typeElement, processingEnvironment)
        .stream()
        .map(elem -> new OperationElementAST(elem, processingEnvironment))
        .collect(toList()));
    classInformation = new LazyValue<>(() -> ClassInformationAnnotationFactory.fromTypeMirror(typeMirror, processingEnvironment));
  }

  /**
   * {@inheritDoc}
   */
  //TODO - This will should be removed once there is a AST Type Loader
  @Override
  public Optional<Class<?>> getDeclaringClass() {
    Class<?> declaringClass;
    try {
      declaringClass = Class.forName(processingEnvironment.getElementUtils().getBinaryName(typeElement).toString());
    } catch (Exception e) {
      declaringClass = astUtils.getPrimitiveClass(this).orElse(null);
    }
    return Optional.ofNullable(declaringClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return typeElement.getSimpleName().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FieldElement> getFields() {
    return IntrospectionUtils.getFields(typeElement, processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FieldElement> getAnnotatedFields(Class<? extends Annotation>... annotations) {
    return getFields().stream()
        .filter(elem -> Stream.of(annotations)
            .anyMatch(annotation -> elem.getAnnotation(annotation).isPresent()))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(typeElement.getAnnotation(annotationClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    if (this.isAnnotatedWith(annotationClass)) {
      return of(astUtils.fromAnnotation(annotationClass, typeElement));
    } else {
      return empty();
    }
  }

  /**
   * {@inheritDoc}
   */
  public TypeElement getTypeElement() {
    return typeElement;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<TypeGeneric> getGenerics() {
    return typeGenerics;
  }

  @Override
  public MetadataType asMetadataType() {
    return typeLoader.load(typeMirror).orElseThrow(RuntimeException::new);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAssignableTo(Class<?> clazz) {
    Types types = processingEnvironment.getTypeUtils();
    return types
        .isAssignable(types.erasure(typeMirror), types.erasure(getTypeMirrorFromClass(clazz)));
  }

  @Override
  public boolean isAssignableTo(Type type) {
    if (type instanceof ASTType) {
      return processingEnvironment.getTypeUtils()
          .isAssignable(processingEnvironment.getTypeUtils().erasure(typeMirror),
                        processingEnvironment.getTypeUtils().erasure(((ASTType) type).typeMirror));
    } else if (type.getDeclaringClass().isPresent()) {
      return processingEnvironment.getTypeUtils()
          .isAssignable(processingEnvironment.getTypeUtils().erasure(typeMirror),
                        getTypeMirrorFromClass(type.getDeclaringClass().get()));
    }
    return false;
  }

  @Override
  public boolean isSameType(Type type) {
    if (type instanceof ASTType) {
      TypeMirror givenType = processingEnvironment.getTypeUtils().erasure(((ASTType) type).typeMirror);
      TypeMirror actualType = processingEnvironment.getTypeUtils().erasure(typeMirror);
      return processingEnvironment.getTypeUtils().isSameType(actualType, givenType);
    } else if (type.getDeclaringClass().isPresent()) {
      return isSameType(type.getDeclaringClass().get());
    }
    return false;
  }

  @Override
  public boolean isSameType(Class<?> clazz) {
    Types types = processingEnvironment.getTypeUtils();
    return types
        .isSameType(types.erasure(typeMirror), types.erasure(getTypeMirrorFromClass(clazz)));
  }

  @Override
  public boolean isInstantiable() {
    return getClassInformation().isInstantiable();
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public boolean isAssignableFrom(Class<?> clazz) {
    TypeMirror typeMirror = getTypeMirrorFromClass(clazz);
    return processingEnvironment.getTypeUtils()
        .isAssignable(typeMirror, processingEnvironment.getTypeUtils().erasure(this.typeMirror));
  }

  @Override
  public boolean isAssignableFrom(Type type) {
    return type.isAssignableTo(this);
  }

  private TypeMirror getTypeMirrorFromClass(Class<?> clazz) {
    return astUtils.getPrimitiveTypeMirror(clazz)
        .orElseGet(() -> processingEnvironment.getElementUtils()
            .getTypeElement(clazz.getName())
            .asType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTypeName() {
    return typeElement == null ? typeMirror.toString() : typeElement.toString();
  }

  @Override
  public ClassInformationAnnotation getClassInformation() {
    return classInformation.get();
  }

  @Override
  public boolean isAnyType() {
    //TODO REVIEW
    return typeMirror instanceof WildcardType;
  }

  public List<OperationElement> getMethods() {
    return methods.get();
  }

  private List<TypeGeneric> toTypeGenerics(TypeIntrospectionResult result, ProcessingEnvironment pe) {
    List<TypeGeneric> typeGenerics = new ArrayList<>();
    for (TypeIntrospectionResult aResult : result.getGenerics()) {
      typeGenerics.add(new TypeGeneric(new ASTType(aResult.getConcreteTypeMirror(), pe),
                                       aResult.getGenerics().isEmpty() ? emptyList() : toTypeGenerics(aResult, pe)));
    }
    return typeGenerics;
  }

  /**
   *
   * @param interfaceClass The {@link Class} with generics
   * @return
   */
  public List<org.mule.runtime.module.extension.internal.loader.java.type.Type> getInterfaceGenerics(Class interfaceClass) {
    return IntrospectionUtils
        .getInterfaceGenerics(typeMirror, processingEnvironment.getElementUtils().getTypeElement(interfaceClass.getName()),
                              processingEnvironment)
        .stream()
        .map(typeMirror -> new ASTType(typeMirror, processingEnvironment))
        .collect(toList());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ASTType type = (ASTType) o;

    return new EqualsBuilder()
        .append(typeMirror, type.typeMirror)
        .append(typeElement, type.typeElement)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(typeMirror)
        .append(typeElement)
        .toHashCode();
  }
}
