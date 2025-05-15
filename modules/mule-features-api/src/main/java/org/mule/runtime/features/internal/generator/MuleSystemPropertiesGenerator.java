/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.features.internal.generator;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

class MuleSystemPropertiesGenerator extends AbstractClassGenerator {

  private static final String PACKAGE_NAME = "org.mule.runtime.features.api";
  private static final String MULE_SYSTEM_PROPERTIES_CLASS_NAME = "MuleSystemProperties";

  private final List<MuleSystemPropertyDeclaration> originalPropertiesFromMuleApi;

  public MuleSystemPropertiesGenerator(File outputDir) throws ClassNotFoundException {
    super(outputDir);
    this.originalPropertiesFromMuleApi = getOriginalPropertiesFromMuleApi();
  }

  @Override
  protected String getPackageName() {
    return PACKAGE_NAME;
  }

  @Override
  protected Set<String> getImports() {
    return collectImports(originalPropertiesFromMuleApi);
  }

  @Override
  protected String getClassName() {
    return MULE_SYSTEM_PROPERTIES_CLASS_NAME;
  }

  @Override
  protected void writeClassContent(OutputStream outputStream) throws IOException {
    appendLine(outputStream, "public class " + getClassName() + " {");

    for (MuleSystemPropertyDeclaration property : originalPropertiesFromMuleApi) {
      for (Class<? extends Annotation> annotation : property.getAnnotations()) {
        appendLine(outputStream, "\t@" + annotation.getSimpleName());
      }
      appendLine(outputStream, "\tpublic static final String " + property.getName() + " = \"" + property.getValue() + "\";");
      appendLine(outputStream);
    }

    appendLine(outputStream, "\tprivate " + getClassName() + "() {");
    appendLine(outputStream, "\t\t// Private constructor to prevent instantiation");
    appendLine(outputStream, "\t}");
    appendLine(outputStream);

    appendLine(outputStream, "}");
  }

  private static Set<String> collectImports(List<MuleSystemPropertyDeclaration> properties) {
    return properties.stream()
        .flatMap(prop -> prop.getAnnotations().stream())
        .filter(MuleSystemPropertiesGenerator::isImportNeeded)
        .map(Class::getName)
        .collect(toSet());
  }

  private static List<MuleSystemPropertyDeclaration> getOriginalPropertiesFromMuleApi() throws ClassNotFoundException {
    Class<?> muleApiProperties = Class.forName("org.mule.runtime.api.util.MuleSystemProperties");
    Field[] fields = muleApiProperties.getFields();
    return stream(fields)
        .filter(MuleSystemPropertiesGenerator::isPublicStaticFinalString)
        .map(f -> {
          try {
            List<Class<? extends Annotation>> annotations = stream(f.getAnnotations())
                .map(Annotation::annotationType)
                .filter(MuleSystemPropertiesGenerator::isAvailableAnnotation)
                .collect(toList());
            return new MuleSystemPropertyDeclaration(f.getName(), (String) f.get(null), annotations);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(toList());
  }

  private static boolean isPublicStaticFinalString(Field field) {
    int modifiers = field.getModifiers();
    return isPublic(modifiers) && isStatic(modifiers) && isFinal(modifiers) && field.getType().equals(String.class);
  }

  private static boolean isAvailableAnnotation(Class<? extends Annotation> annotation) {
    return !annotation.getPackageName().startsWith("org.mule.api.annotation");
  }

  private static boolean isImportNeeded(Class<?> cls) {
    return !cls.getPackageName().startsWith("java.lang");
  }

  private static class MuleSystemPropertyDeclaration {

    private final String name;
    private final String value;
    private final List<Class<? extends Annotation>> annotations;

    public MuleSystemPropertyDeclaration(String name, String value, List<Class<? extends Annotation>> annotations) {
      this.name = name;
      this.value = value;
      this.annotations = annotations;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public List<Class<? extends Annotation>> getAnnotations() {
      return annotations;
    }
  }
}
