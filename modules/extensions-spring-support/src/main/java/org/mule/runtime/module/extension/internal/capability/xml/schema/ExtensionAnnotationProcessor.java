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
import static org.mule.runtime.core.api.util.collection.Collectors.toImmutableList;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;

import org.apache.commons.lang3.StringUtils;

/**
 * Annotation processing class that uses the {@link Processor} API to introspect and extract information from the extension
 * source code.
 *
 * @since 3.7.0
 */
public final class ExtensionAnnotationProcessor {

  private static final String PARAM = "@param";
  private static final String VALUE = "value";
  private static final char AT_CHAR = '@';
  private static final char NEW_LINE_CHAR = '\n';
  private static final char SPACE_CHAR = ' ';
  private static final char CLOSING_BRACKET_CHAR = '}';
  private static final char OPENING_BRACKET_CHAR = '{';

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

  public TypeElement getSuperclassElement(Element element) {
    if (element instanceof Symbol.ClassSymbol) {
      return (TypeElement) ((Symbol.ClassSymbol) element).getSuperclass().asElement();
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
    final StringBuilder parsedComment = new StringBuilder();
    final Map<String, String> parameters = new HashMap<>();
    parseJavaDoc(processingEnv, element, new JavadocParseHandler() {

      @Override
      public void onParam(String param) {
        parseMethodParameter(parameters, param);
      }

      @Override
      public void onBodyLine(String bodyLine) {
        parsedComment.append(bodyLine).append(NEW_LINE_CHAR);
      }
    });

    parseOperationParameterGroups(processingEnv, (MethodSymbol) element, parameters);
    return new MethodDocumentation(stripTags(parsedComment.toString()), parameters);
  }

  /**
   * Traverses the arguments of {@code methodElement} and for each argument annotated with {@link ParameterGroup} it invokes
   * {@link #getOperationParameterGroupDocumentation(TypeElement, Map, ProcessingEnvironment)}
   *
   * @param processingEnv the current {@link ProcessingEnvironment}
   * @param methodElement the operation method being processed
   * @param parameterDocs a {@link Map} which keys are attribute names and values are their documentation
   */
  private void parseOperationParameterGroups(ProcessingEnvironment processingEnv, MethodSymbol methodElement,
                                             Map<String, String> parameterDocs) {
    for (VarSymbol parameterSymbol : methodElement.getParameters()) {
      for (Attribute.Compound compound : parameterSymbol.getAnnotationMirrors()) {
        DeclaredType annotationType = compound.getAnnotationType();
        if (annotationType != null) {
          Class annotationClass = classFor((TypeElement) compound.getAnnotationType().asElement(), processingEnv).get();
          if (ParameterGroup.class.isAssignableFrom(annotationClass)) {
            try {
              getOperationParameterGroupDocumentation((TypeElement) processingEnv.getTypeUtils()
                  .asElement(parameterSymbol.asType()), parameterDocs, processingEnv);
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
    final StringBuilder parsedComment = new StringBuilder();
    parseJavaDoc(processingEnv, element, new JavadocParseHandler() {

      @Override
      public void onParam(String param) {}

      @Override
      public void onBodyLine(String bodyLine) {
        parsedComment.append(bodyLine).append(NEW_LINE_CHAR);
      }
    });

    return stripTags(parsedComment.toString());
  }

  private static String stripTags(String comment) {
    StringBuilder builder = new StringBuilder();
    boolean insideTag = false;
    comment = comment.trim();

    final int length = comment.length();
    for (int i = 0; i < length; i++) {
      if (comment.charAt(i) == OPENING_BRACKET_CHAR) {
        int nextCharIndex = i + 1;
        if (nextCharIndex < length && comment.charAt(nextCharIndex) == AT_CHAR) {
          while (comment.charAt(nextCharIndex) != SPACE_CHAR && comment.charAt(nextCharIndex) != CLOSING_BRACKET_CHAR) {
            nextCharIndex = i++;
          }
          insideTag = true;
          i = nextCharIndex;
          continue;
        }
      } else if (comment.charAt(i) == CLOSING_BRACKET_CHAR && insideTag) {
        insideTag = false;
        continue;
      }

      builder.append(comment.charAt(i));
    }

    String strippedComments = builder.toString().trim();
    while (strippedComments.length() > 0 && strippedComments.charAt(strippedComments.length() - 1) == NEW_LINE_CHAR) {
      strippedComments = StringUtils.chomp(strippedComments);
    }

    return strippedComments;
  }

  private void parseJavaDoc(ProcessingEnvironment processingEnv, Element element, JavadocParseHandler handler) {
    String comment = extractJavadoc(processingEnv, element);

    StringTokenizer st = new StringTokenizer(comment, "\n\r");
    while (st.hasMoreTokens()) {
      String nextToken = st.nextToken().trim();
      if (nextToken.isEmpty()) {
        continue;
      }
      if (nextToken.startsWith(PARAM)) {
        handler.onParam(nextToken);
      } else if (!(nextToken.charAt(0) == AT_CHAR)) {
        handler.onBodyLine(nextToken);
      }
    }
  }

  private void parseMethodParameter(Map<String, String> parameters, String param) {
    param = param.replaceFirst(PARAM, StringUtils.EMPTY).trim();
    int descriptionIndex = param.indexOf(" ");
    String paramName;
    String description;
    if (descriptionIndex != -1) {
      paramName = param.substring(0, descriptionIndex).trim();
      description = param.substring(descriptionIndex).trim();
    } else {
      paramName = param;
      description = "";
    }

    parameters.put(paramName, stripTags(description));
  }

  private String extractJavadoc(ProcessingEnvironment processingEnv, Element element) {
    String comment = processingEnv.getElementUtils().getDocComment(element);

    if (StringUtils.isBlank(comment)) {
      return StringUtils.EMPTY;
    }

    return comment.trim();
  }

  private interface JavadocParseHandler {

    void onParam(String param);

    void onBodyLine(String bodyLine);
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
