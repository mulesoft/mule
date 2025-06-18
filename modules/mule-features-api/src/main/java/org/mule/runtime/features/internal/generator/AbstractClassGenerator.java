/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.features.internal.generator;

import static java.nio.file.Files.createDirectories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

abstract class AbstractClassGenerator {

  private final File outputDir;

  AbstractClassGenerator(File outputDir) {
    this.outputDir = outputDir;
  }

  protected abstract String getPackageName();

  protected abstract Set<String> getImports();

  protected abstract String getClassName();

  protected abstract void writeClassContent(OutputStream outputStream) throws IOException;

  protected void appendLine(OutputStream outputStream, String line) throws IOException {
    outputStream.write((line + "\n").getBytes());
  }

  protected void appendLine(OutputStream outputStream) throws IOException {
    appendLine(outputStream, "");
  }

  public void generate() throws IOException {
    Path packageLocation = Paths.get(outputDir.getAbsolutePath(), getLocationFromPackageName(getPackageName()));
    createDirectories(packageLocation);
    File generatedJava = packageLocation.resolve(getClassName() + ".java").toFile();
    generatedJava.createNewFile();
    try (FileOutputStream outputStream = new FileOutputStream(generatedJava)) {
      appendLine(outputStream, "package " + getPackageName() + ";");
      appendLine(outputStream);

      for (String importedClass : getImports()) {
        appendLine(outputStream, "import " + importedClass + ";");
      }

      writeClassContent(outputStream);
    }
  }

  private static String getLocationFromPackageName(String packageName) {
    return packageName.replace(".", "/");
  }

  protected static boolean isPublicStaticFinalString(Field field) {
    int modifiers = field.getModifiers();
    return isPublic(modifiers) && isStatic(modifiers) && isFinal(modifiers) && field.getType().equals(String.class);
  }

  protected static boolean isPublicStaticFinalFeature(Field field, Class<?> featureClass) {
    int modifiers = field.getModifiers();
    return isPublic(modifiers) && isStatic(modifiers) && isFinal(modifiers)
        && field.getType().equals(featureClass);
  }

  protected static boolean isAvailableAnnotation(Class<? extends Annotation> annotation) {
    return !annotation.getPackageName().startsWith("org.mule.api.annotation");
  }

  protected static boolean isImportNeeded(Class<?> cls) {
    return !cls.getPackageName().startsWith("java.lang");
  }
}
