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

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.GenericInfo;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor.MuleTypeVisitor;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor.TypeIntrospectionResult;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link Type} implementation which uses {@link TypeMirror} and {@link TypeElement} to represent a Java Class.
 *
 * @since 4.1
 */
public class ASTType implements Type {

  private final TypeMirror typeMirror;
  private final List<GenericInfo> genericInfo;
  private LazyValue<List<MethodElement>> methods;
  final ProcessingEnvironment processingEnvironment;
  final TypeElement typeElement;
  ASTUtils astUtils;

  /**
   * Creates a new {@link ASTType} based on a {@link TypeMirror}.
   * @param typeMirror
   * @param processingEnvironment
   */
  public ASTType(TypeMirror typeMirror, ProcessingEnvironment processingEnvironment) {

    this.processingEnvironment = processingEnvironment;
    TypeIntrospectionResult accept = typeMirror.accept(new MuleTypeVisitor(processingEnvironment), builder());
    this.typeElement = accept.getConcreteType();
    this.genericInfo = toGenericInfo(accept, processingEnvironment);
    this.typeMirror = typeMirror;
    init();
  }

  public ASTType(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
    this.typeElement = typeElement;
    this.typeMirror = typeElement.asType();
    this.genericInfo = emptyList();
    init();
  }

  private void init() {
    astUtils = new ASTUtils(processingEnvironment);
    methods = new LazyValue<>(() -> getApiMethods(typeElement, processingEnvironment)
        .stream()
        .map(elem -> new MethodElementAST(elem, processingEnvironment))
        .collect(toList()));
  }

  /**
   * {@inheritDoc}
   */
  //TODO - This will should be removed once there is a AST Type Loader
  @Override
  public Class getDeclaringClass() {
    try {
      return Class.forName(processingEnvironment.getElementUtils().getBinaryName(typeElement).toString());
    } catch (Exception e) {
      return astUtils.getPrimitiveClass(this).orElseThrow(() -> new RuntimeException(e));
    }
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
  public List<GenericInfo> getGenerics() {
    return genericInfo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public java.lang.reflect.Type getReflectType() {
    return astUtils.getReflectType(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAssignableTo(Class<?> clazz) {
    return processingEnvironment.getTypeUtils()
        .isAssignable(processingEnvironment.getTypeUtils().erasure(typeMirror), getTypeMirrorFromClass(clazz));
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

  public List<MethodElement> getMethods() {
    return methods.get();
  }

  private List<GenericInfo> toGenericInfo(TypeIntrospectionResult result, ProcessingEnvironment pe) {
    List<GenericInfo> genericInfos = new ArrayList<>();
    for (TypeIntrospectionResult aResult : result.getGenerics()) {
      genericInfos.add(new GenericInfo(new ASTType(aResult.getConcreteType(), pe),
                                       aResult.getGenerics().isEmpty() ? emptyList() : toGenericInfo(aResult, pe)));
    }
    return genericInfos;
  }
}
