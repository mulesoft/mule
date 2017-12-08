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
import static javax.lang.model.element.ElementKind.METHOD;

import org.mule.runtime.api.util.Reference;
import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * {@link AnnotationValueFetcher} which works with the Java AST.
 *
 * @since 4.1
 */
public class ASTBasedValueFetcher<A extends Annotation> implements AnnotationValueFetcher<A> {

  private final Class<A> annotationClass;
  private final Element typeElement;
  private final ProcessingEnvironment processingEnvironment;

  ASTBasedValueFetcher(Class<A> annotationClass, Element annotatedElememt, ProcessingEnvironment processingEnvironment) {
    this.annotationClass = annotationClass;
    this.typeElement = annotatedElememt;
    this.processingEnvironment = processingEnvironment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getStringValue(Function<A, String> function) {
    return (String) getConstant(function).getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Type> getClassArrayValue(Function<A, Class[]> function) {
    AnnotationValue value = (AnnotationValue) getObjectValue(function);
    if (value != null) {
      List<AnnotationValue> array = (List<AnnotationValue>) value.getValue();
      return array.stream().map(attr -> ((DeclaredType) attr.getValue()))
          .map(declaredType -> new ASTType((TypeElement) declaredType.asElement(), processingEnvironment))
          .collect(toList());
    } else {
      return emptyList();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ASTType getClassValue(Function<A, Class> function) {
    return new ASTType((TypeElement) ((DeclaredType) ((AnnotationValue) getObjectValue(function)).getValue()).asElement(),
                       processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <N extends Number> N getNumberValue(Function<A, N> function) {
    return (N) getConstant(function).getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <E extends Enum> E getEnumValue(Function<A, E> function) {
    VariableElement value = (VariableElement) ((AnnotationValue) getObjectValue(function)).getValue();
    Class<? extends Enum> annotationClass;
    try {
      annotationClass = (Class<? extends Enum>) Class
          .forName(processingEnvironment.getElementUtils().getBinaryName((TypeElement) value.getEnclosingElement()).toString());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    return (E) Enum.valueOf(annotationClass, value.toString());
  }

  private AnnotationValue getConstant(Function function) {
    return (AnnotationValue) getObjectValue(function);
  }

  private Object getObjectValue(Function function) {
    return getObjectValue(annotationClass, typeElement, function, processingEnvironment);
  }

  /**
   * Retrieves the value of an annotation property.
   * <p>
   * The problem with Annotations in a AST environment is that the way to obtain Annotation values behaves different
   * between having a proper Annotation class and one when using the AST, because for example if there is an annotation
   * that has Class references, you will never be able to obtain the Class value, because that class doesn't exist already
   * As consequence the safe way to access annotation values in AST is knowing the property name, this mean soft references.
   * <p>
   * To prevent the soft reference, the user obtains a Function which receives an Annotation instance and later
   * makes usage of the annotation to communicate which property wants to retrieve.
   * <p>
   * E.g.: {@code getAnnotationValue(MetadataKeyPart.class).intValue(MetadataKeyPart::order);}
   * {@code getAnnotationValue(OfValues.class).classValue(OfValues::value);}
   * <p>
   * But this doesn't fix the issue, because asking for the class in a AST environment will make everything fail, the
   * key part is that the Annotation instance is not a real instance, is a proxy which intercepts the call with the
   * purpose of obtain the property names this method can safely return the property value.
   *
   * @param annotationClass        The annotation to look for
   * @param annotatedElement       The element is annotated with the annotation class
   * @param retrievalValueFunction Function to obtain the property name
   * @param processingEnvironment  AST Processing Environment
   * @param <T>                    The annotation type
   * @return The annotation property value
   */
  private <T> Object getObjectValue(Class<T> annotationClass, Element annotatedElement, Function retrievalValueFunction,
                                    ProcessingEnvironment processingEnvironment) {
    CountDownLatch latch = new CountDownLatch(1);
    Enhancer enhancer = new Enhancer();
    Reference<Object> reference = new Reference<>();
    enhancer.setSuperclass(annotationClass);
    enhancer.setCallback((MethodInterceptor) (o, method, objects, methodProxy) -> {
      //This is just to prevent to be called with a toString() by the IDE when debugging
      if (method.getName().equals("toString")) {
        return "string";
      }
      System.out.println(Thread.currentThread().toString());
      reference.set(null);

      getAnnotationFrom(annotationClass, annotatedElement, processingEnvironment)
          .ifPresent(annotation -> getAnnotationElementValue(annotation, method.getName())
              .ifPresent(reference::set));

      latch.countDown();
      return null;
    });
    retrievalValueFunction.apply(enhancer.create());
    return reference.get();
  }

  private static Optional<? extends AnnotationValue> getAnnotationElementValue(AnnotationMirror annotation, String name) {
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(name)) {
        return of(entry.getValue());
      }
    }

    for (Element element : annotation.getAnnotationType().asElement().getEnclosedElements()) {
      if (element.getKind().equals(METHOD)) {
        if (element.getSimpleName().toString().equals(name)) {
          return ofNullable(((ExecutableElement) element).getDefaultValue());
        }
      }
    }

    return empty();
  }

  private static Optional<AnnotationMirror> getAnnotationFrom(Class configurationClass, Element typeElement,
                                                              ProcessingEnvironment processingEnvironment) {
    TypeElement annotationTypeElement = processingEnvironment.getElementUtils().getTypeElement(configurationClass.getTypeName());
    for (AnnotationMirror annotationMirror : typeElement.getAnnotationMirrors()) {
      DeclaredType annotationType = annotationMirror.getAnnotationType();
      TypeMirror obj = annotationTypeElement.asType();
      if (annotationType.equals(obj)) {
        return of(annotationMirror);
      }
    }
    return empty();
  }
}
