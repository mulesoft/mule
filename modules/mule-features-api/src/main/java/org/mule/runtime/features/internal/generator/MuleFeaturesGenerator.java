/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.features.internal.generator;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.config.Feature;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class MuleFeaturesGenerator extends AbstractClassGenerator {

  private static final String PACKAGE_NAME = "org.mule.runtime.features.api";
  private static final String MULE_FEATURE_CLASS_NAME = "MuleRuntimeFeature";
  private static final Class<?> MULE_API_FEATURE_CLASS;

  static {
    try {
      MULE_API_FEATURE_CLASS = Class.forName("org.mule.runtime.api.config.MuleRuntimeFeature");
    } catch (ClassNotFoundException e) {
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
    HashSet<String> imports = new HashSet<>();
    imports.add("org.mule.runtime.api.config.Feature");
    imports.add("java.util.Optional");
    imports.addAll(collectImports(originalPropertiesFromMuleApi));
    return imports;
  }

  @Override
  protected String getClassName() {
    return MULE_FEATURE_CLASS_NAME;
  }

  @Override
  protected void writeClassContent(OutputStream outputStream) throws IOException {
    appendLine(outputStream, "public enum " + getClassName() + " implements Feature {");

    for (MuleFeatureDeclaration property : originalPropertiesFromMuleApi) {
      for (Class<? extends Annotation> annotation : property.getAnnotations()) {
        appendLine(outputStream, "\t@" + annotation.getSimpleName());
      }
      appendLine(outputStream, format("\t%s(\"%s\", \"%s\", \"%s\", \"%s\", \"%s\"),",
                                      property.getName(),
                                      property.getDescription(),
                                      property.getIssueId(),
                                      property.getEnabledByDefaultSince(),
                                      property.getOverridingSystemPropertyName().orElse(null),
                                      property.getMinJavaVersion()));
      appendLine(outputStream);
    }
    appendLine(outputStream, "\t;");

    appendLine(outputStream, "\tprivate final String description;");
    appendLine(outputStream, "\tprivate final String issueId;");
    appendLine(outputStream, "\tprivate final String enabledByDefaultSince;");
    appendLine(outputStream, "\tprivate final String overridingSystemPropertyName;");
    appendLine(outputStream, "\tprivate final String minJavaVersion;");
    appendLine(outputStream);
    appendLine(outputStream, "\t" + getClassName()
        + "(String description, String issueId, String enabledByDefaultSince, String overridingSystemPropertyName, String minJavaVersion) {");
    appendLine(outputStream, "\t\tthis.description = description;");
    appendLine(outputStream, "\t\tthis.issueId = issueId;");
    appendLine(outputStream, "\t\tthis.enabledByDefaultSince = enabledByDefaultSince;");
    appendLine(outputStream, "\t\tthis.overridingSystemPropertyName = overridingSystemPropertyName;");
    appendLine(outputStream, "\t\tthis.minJavaVersion = minJavaVersion;");
    appendLine(outputStream, "\t}");
    appendLine(outputStream);

    appendLine(outputStream,
               "\tpublic String getDescription() {" +
                   "\treturn description;" +
                   "\t} ");
    appendLine(outputStream,
               "\tpublic String getIssueId() {" +
                   "\treturn issueId;" +
                   "\t} ");
    appendLine(outputStream,
               "\tpublic String getSince() {" +
                   "\treturn getEnabledByDefaultSince();" +
                   "\t} ");
    appendLine(outputStream,
               "\tpublic String getEnabledByDefaultSince() {" +
                   "\treturn enabledByDefaultSince;" +
                   "\t} ");
    appendLine(outputStream,
               "\tpublic Optional<String> getOverridingSystemPropertyName() {" +
                   "\treturn Optional.ofNullable(overridingSystemPropertyName);" +
                   "\t}");
    appendLine(outputStream,
               "\tpublic String getMinJavaVersion() {" +
                   "\treturn minJavaVersion;" +
                   "\t}");
    appendLine(outputStream, "}");
  }

  private static Set<String> collectImports(List<MuleFeatureDeclaration> properties) {
    return properties.stream().flatMap(prop -> prop.getAnnotations().stream())
        .filter(AbstractClassGenerator::isImportNeeded).map(Class::getName).collect(toSet());
  }


  private static List<MuleFeatureDeclaration> getOriginalFeatureFromMuleApi() {
    Field[] fields = MULE_API_FEATURE_CLASS.getFields();
    return stream(fields).filter(field -> AbstractClassGenerator.isPublicStaticFinalFeature(field, MULE_API_FEATURE_CLASS))
        .map(f -> {
          try {
            List<Class<? extends Annotation>> annotations = stream(f.getAnnotations())
                .map(Annotation::annotationType).filter(AbstractClassGenerator::isAvailableAnnotation)
                .collect(toList());
            return new MuleFeatureDeclaration(f.getName(), f.get(null), annotations);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }).collect(toList());
  }



  private static class MuleFeatureDeclaration {

    private final String name;
    private final String description;
    private final String issueId;
    private final String enabledByDefaultSince;
    private final Optional<String> overridingSystemPropertyName;
    private final String minJavaVersion;
    private final List<Class<? extends Annotation>> annotations;

    public MuleFeatureDeclaration(String name, Object value, List<Class<? extends Annotation>> annotations) {
      this.name = name;
      this.annotations = annotations;
      
      Feature feature = (Feature) value;

      String rawDescription = feature.getDescription();
      this.description = rawDescription != null ? rawDescription.replaceAll("\\R", " ").trim() : name;
      this.issueId = feature.getIssueId();
      this.enabledByDefaultSince = feature.getEnabledByDefaultSince();
      this.overridingSystemPropertyName = feature.getOverridingSystemPropertyName();
      this.minJavaVersion = feature.getMinJavaVersion();
    }

    /**
     * Returns the name of the feature flag.
     *
     * @return the feature flag name.
     */
    public String getName() {
      return name;
    }

    /**
     * Description of the feature.
     *
     * @return The feature description.
     */
    public String getDescription() {
      return description;
    }

    /**
     * The issue that caused this feature addition.
     *
     * @return Issue that motivated the feature.
     */
    public String getIssueId() {
      return issueId;
    }

    /**
     * A comma-separated list of versions (must include all the different minors) since this feature will be enabled by default.
     * Any relevant artifact (application, policy... etc) with a minMuleVersion matching this list will have this {@link Feature}
     * enabled by default.
     *
     * @return A comma-separated list of versions.
     */
    public String getEnabledByDefaultSince() {
      return enabledByDefaultSince;
    }

    /**
     * Returns the minimum Java version required for this feature to be enabled.
     *
     * @return The minimum Java version required for this feature, or null if there is no minimum version requirement.
     */
    public String getMinJavaVersion() {
      return minJavaVersion;
    }

    /**
     * Returns the system property name that can override the feature flag configuration.
     *
     * @return Optional containing the system property name, or {@code Optional.empty()} if no override is available
     * @see Feature#getOverridingSystemPropertyName()
     */
    public Optional<String> getOverridingSystemPropertyName() {
      return overridingSystemPropertyName;
    }

    /**
     * Returns the list of annotations applied to this feature declaration.
     *
     * @return an immutable list of annotation classes applied to this feature.
     */
    public List<Class<? extends Annotation>> getAnnotations() {
      return annotations;
    }
  }
}
