/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.doc.JavaDocReader.parseJavaDoc;

import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.doc.JavaDocModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

/**
 * Annotation processing class that uses the {@link Processor} API to introspect and extract information from the extension
 * source code.
 *
 * @since 3.7.0
 */
public final class ExtensionAnnotationProcessor {

  private static final String VALUE = "value";

  /**
   * Returns the {@link Class} object that is associated to the {@code typeElement}
   *
   * @param typeElement           a {@link TypeElement} which represents a {@link Class}
   * @param processingEnvironment the current {@link ProcessingEnvironment}
   * @param <T>                   the generic type of the returned {@link Class}
   * @return the {@link Class} represented by {@code typeElement}
   */
  public <T> Optional<Class<T>> classFor(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    try {
      return of(loadClass(getClassName(typeElement, processingEnvironment), typeElement.getClass()));
    } catch (ClassNotFoundException e) {
      return empty();
    }
  }

  /**
   * Returns the name of the class represented by the {@code typeElement}
   * @param typeElement a {@link TypeElement}
   * @param processingEnvironment the {@link ProcessingEnvironment}
   * @return A class name
   */
  public String getClassName(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    return processingEnvironment.getElementUtils().getBinaryName(typeElement).toString();
  }

  /**
   * Returns the {@link TypeElement}s in the {@code roundEnvironment} which are annotated with {@code annotationType}
   *
   * @param annotationType   the type of the {@link Annotation}
   * @param roundEnvironment the current {@link RoundEnvironment}
   * @return a {@link Set} with the {@link TypeElement}s annotated with {@code annotationType}
   */
  public Set<TypeElement> getTypeElementsAnnotatedWith(Class<? extends Annotation> annotationType,
                                                       RoundEnvironment roundEnvironment) {
    return ElementFilter.typesIn(roundEnvironment.getElementsAnnotatedWith(annotationType));
  }

  public List<TypeElement> getAnnotationClassesValue(Element element, Class<? extends Annotation> annotation,
                                                     Class[] valueClasses) {
    List<AnnotationValue> annotationValues = getAnnotationValue(element, annotation);
    if (annotation == null) {
      return emptyList();
    }
    return Stream.of(valueClasses)
        .map(c -> (TypeElement) getElementForClass(annotationValues, c))
        .collect(toImmutableList());
  }

  public <T> T getAnnotationFromType(ProcessingEnvironment processingEnvironment, TypeElement rootElement,
                                     Class<? extends Annotation> annotationClass) {
    return (T) classFor(rootElement, processingEnvironment).get().getAnnotation(annotationClass);
  }

  public Element getElementForClass(List<AnnotationValue> annotationValues, Class<?> clazz) {
    return annotationValues.stream().map(e -> ((DeclaredType) e.getValue()).asElement())
        .filter(e -> e.getSimpleName().toString().equals(clazz.getSimpleName())).findFirst().orElse(null);
  }

  public Map<String, VariableElement> getFieldsAnnotatedWith(TypeElement element, Class<? extends Annotation> annotation) {
    if (element == null) {
      return emptyMap();
    }
    ImmutableMap.Builder<String, VariableElement> builder = ImmutableMap.builder();
    TypeElement superClass = getSuperclassElement(element);
    builder.putAll(getFieldsAnnotatedWith(superClass, annotation));
    builder.putAll(collectAnnotatedElements(fieldsIn(element.getEnclosedElements()), annotation));
    return builder.build();
  }

  private TypeElement getSuperclassElement(Element element) {
    if (element instanceof TypeElement) {
      TypeMirror superclass = ((TypeElement) element).getSuperclass();
      if (superclass instanceof DeclaredType) {
        return ((TypeElement) ((DeclaredType) superclass).asElement());
      }
    }
    return null;
  }

  private <T extends Element> Map<String, T> collectAnnotatedElements(Iterable<T> elements, Class<? extends Annotation> clazz) {
    ImmutableMap.Builder<String, T> fields = ImmutableMap.builder();
    elements.forEach(e -> {
      if (e.getAnnotation(clazz) != null) {
        fields.put(e.getSimpleName().toString(), e);
      }
    });
    return fields.build();
  }

  public MethodDocumentation getMethodDocumentation(ProcessingEnvironment processingEnv, Element element) {
    JavaDocModel javadocModel = parseJavaDoc(processingEnv, element);
    parseOperationParameterGroups(processingEnv, (ExecutableElement) element, javadocModel.getParameters());
    return new MethodDocumentation(javadocModel.getBody(), javadocModel.getParameters());
  }

  /**
   * Traverses the arguments of {@code method} and for each argument annotated with {@link ParameterGroup} it invokes
   * {@link #getOperationParameterGroupDocumentation(TypeElement, Map, ProcessingEnvironment)}
   *
   * @param env the current {@link ProcessingEnvironment}
   * @param method the operation method being processed
   * @param docs a {@link Map} which keys are attribute names and values are their documentation
   */
  private void parseOperationParameterGroups(ProcessingEnvironment env, ExecutableElement method, Map<String, String> docs) {
    for (VariableElement variable : method.getParameters()) {
      for (AnnotationMirror compound : variable.getAnnotationMirrors()) {
        DeclaredType annotationType = compound.getAnnotationType();
        if (annotationType != null) {
          Class annotationClass = classFor((TypeElement) compound.getAnnotationType().asElement(), env).get();
          if (ParameterGroup.class.isAssignableFrom(annotationClass)) {
            try {
              getOperationParameterGroupDocumentation((TypeElement) env.getTypeUtils().asElement(variable.asType()), docs, env);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    }
  }

  /**
   * Extracts the documentation of the parameters in a group described by {@code groupElement}. The obtained docs are added to
   * {@code parameterDocs}
   *
   * @param groupElement          a {@link TypeElement} representing the parameter group
   * @param parameterDocs         a {@link Map} which keys are attribute names and values are their documentation
   * @param processingEnvironment the current {@link ProcessingEnvironment}
   */
  private void getOperationParameterGroupDocumentation(TypeElement groupElement, final Map<String, String> parameterDocs,
                                                       ProcessingEnvironment processingEnvironment) {
    getFieldsAnnotatedWith(groupElement, Parameter.class)
        .forEach((key, value) -> parameterDocs.put(key, getJavaDocSummary(processingEnvironment, value)));

    getFieldsAnnotatedWith(groupElement, ParameterGroup.class)
        .values()
        .forEach(field -> getOperationParameterGroupDocumentation((TypeElement) processingEnvironment.getTypeUtils()
            .asElement(field.asType()), parameterDocs, processingEnvironment));
  }

  public String getJavaDocSummary(ProcessingEnvironment processingEnv, Element element) {
    return parseJavaDoc(processingEnv, element).getBody();
  }

  /**
   * Returns the content of a field for a given annotation.
   */
  public <T> T getAnnotationValue(Element rootElement, Class<? extends Annotation> anAnnotation) {
    if (rootElement.getAnnotation(anAnnotation) != null) {
      final String fullQualifiedAnnotationName = anAnnotation.getName();
      final Reference<T> annotationFieldValue = new Reference<>();
      rootElement.getAnnotationMirrors()
          .stream()
          .filter(annotationMirror -> fullQualifiedAnnotationName.equals(annotationMirror.getAnnotationType().toString()))
          .forEach(annotationMirror -> annotationMirror.getElementValues()
              .entrySet()
              .stream()
              .filter(entry -> VALUE.equals(entry.getKey().getSimpleName().toString()))
              .findFirst()
              .ifPresent(entry -> annotationFieldValue.set((T) entry.getValue().getValue())));

      return annotationFieldValue.get();
    } else {
      return null;
    }
  }
}
