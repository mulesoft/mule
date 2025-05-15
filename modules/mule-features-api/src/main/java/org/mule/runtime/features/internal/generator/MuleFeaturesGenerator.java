/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.features.internal.generator;

import static java.lang.String.format;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class MuleFeaturesGenerator extends AbstractClassGenerator {

  private static final String PACKAGE_NAME = "org.mule.runtime.features.api";
  private static final String MULE_FEATURE_CLASS_NAME = "MuleRuntimeFeature";
  private static final Class<?> MULE_API_FEATURE_CLASS;
  private static final Method GET_ISSUE_ID_METHOD;
  private static final Method GET_ENABLED_BY_DEFAULT_SINCE_METHOD;
  private static final Method GET_OVERRIDING_SYSTEM_PROPERTY_NAME_METHOD;

  static {
    try {
      MULE_API_FEATURE_CLASS = Class.forName("org.mule.runtime.api.config.MuleRuntimeFeature");
      GET_ISSUE_ID_METHOD = MULE_API_FEATURE_CLASS.getMethod("getIssueId");
      GET_ENABLED_BY_DEFAULT_SINCE_METHOD = MULE_API_FEATURE_CLASS.getMethod("getEnabledByDefaultSince");
      GET_OVERRIDING_SYSTEM_PROPERTY_NAME_METHOD = MULE_API_FEATURE_CLASS.getMethod("getOverridingSystemPropertyName");
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private final List<MuleFeatureDeclaration> originalPropertiesFromMuleApi;

  public MuleFeaturesGenerator(File outputDir) {
    super(outputDir);
    this.originalPropertiesFromMuleApi = getOriginalFeatureFromMuleApi();
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
    return MULE_FEATURE_CLASS_NAME;
  }

  @Override
  protected void writeClassContent(OutputStream outputStream) throws IOException {
    appendLine(outputStream, "public enum " + getClassName() + " {");

    for (MuleFeatureDeclaration property : originalPropertiesFromMuleApi) {
      for (Class<? extends Annotation> annotation : property.getAnnotations()) {
        appendLine(outputStream, "\t@" + annotation.getSimpleName());
      }
      appendLine(outputStream,
                 format("\t%s(\"%s\", \"%s\", \"%s\"),", property.getName(), property.getIssueId(),
                        property.getEnabledByDefaultSince(), property.getOverridingSystemPropertyName().orElse(null)));
      appendLine(outputStream);
    }
    appendLine(outputStream, "\t;");

    appendLine(outputStream, "\tprivate final String issueId;");
    appendLine(outputStream, "\tprivate final String enabledByDefaultSince;");
    appendLine(outputStream, "\tprivate final String overridingSystemPropertyName;");
    appendLine(outputStream);
    appendLine(outputStream,
               "\t" + getClassName() + "(String issueId, String enabledByDefaultSince, String overridingSystemPropertyName) {");
    appendLine(outputStream, "\t\tthis.issueId = issueId;");
    appendLine(outputStream, "\t\tthis.enabledByDefaultSince = enabledByDefaultSince;");
    appendLine(outputStream, "\t\tthis.overridingSystemPropertyName = overridingSystemPropertyName;");
    appendLine(outputStream, "\t}");
    appendLine(outputStream);

    appendLine(outputStream, "}");
  }

  private static Set<String> collectImports(List<MuleFeatureDeclaration> properties) {
    return properties.stream()
        .flatMap(prop -> prop.getAnnotations().stream())
        .filter(MuleFeaturesGenerator::isImportNeeded)
        .map(Class::getName)
        .collect(toSet());
  }

  private static List<MuleFeatureDeclaration> getOriginalFeatureFromMuleApi() {
    Field[] fields = MULE_API_FEATURE_CLASS.getFields();
    return stream(fields)
        .filter(MuleFeaturesGenerator::isPublicStaticFinalFeature)
        .map(f -> {
          try {
            List<Class<? extends Annotation>> annotations = stream(f.getAnnotations())
                .map(Annotation::annotationType)
                .filter(MuleFeaturesGenerator::isAvailableAnnotation)
                .collect(toList());
            return new MuleFeatureDeclaration(f.getName(), f.get(null), annotations);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(toList());
  }

  private static boolean isPublicStaticFinalFeature(Field field) {
    int modifiers = field.getModifiers();
    return isPublic(modifiers) && isStatic(modifiers) && isFinal(modifiers) && field.getType().equals(MULE_API_FEATURE_CLASS);
  }

  private static boolean isAvailableAnnotation(Class<? extends Annotation> annotation) {
    return !annotation.getPackageName().startsWith("org.mule.api.annotation");
  }

  private static boolean isImportNeeded(Class<?> cls) {
    return !cls.getPackageName().startsWith("java.lang");
  }

  private static class MuleFeatureDeclaration {

    private final String name;
    private final String issueId;
    private final String enabledByDefaultSince;
    private final Optional<String> overridingSystemPropertyName;
    private final List<Class<? extends Annotation>> annotations;

    public MuleFeatureDeclaration(String name, Object value, List<Class<? extends Annotation>> annotations) {
      this.name = name;
      this.annotations = annotations;
      try {
        issueId = (String) GET_ISSUE_ID_METHOD.invoke(value);
        enabledByDefaultSince = (String) GET_ENABLED_BY_DEFAULT_SINCE_METHOD.invoke(value);
        overridingSystemPropertyName = (Optional<String>) GET_OVERRIDING_SYSTEM_PROPERTY_NAME_METHOD.invoke(value);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getCause());
      }
    }

    public String getName() {
      return name;
    }

    public String getIssueId() {
      return issueId;
    }

    public String getEnabledByDefaultSince() {
      return enabledByDefaultSince;
    }

    public Optional<String> getOverridingSystemPropertyName() {
      return overridingSystemPropertyName;
    }

    public List<Class<? extends Annotation>> getAnnotations() {
      return annotations;
    }
  }
}
