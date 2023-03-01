/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.PropertiesUtils.discoverProperties;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.findMethod;

import org.mule.test.runner.api.DependencyResolver;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.io.ByteStreams;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.EnumerationState;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.RecordComponentDescription.InDefinedShape;
import net.bytebuddy.description.type.RecordComponentList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bytecode.StackSize;

/**
 * Utility class for runner.
 */
public final class RunnerModuleUtils {

  private static final Logger LOGGER = getLogger(RunnerModuleUtils.class);

  public static final String EXCLUDED_PROPERTIES_FILE = "excluded.properties";

  public static final String EXCLUDED_ARTIFACTS = "excluded.artifacts";
  public static final String EXTRA_BOOT_PACKAGES = "extraBoot.packages";
  public static final String JAR_EXTENSION = "jar";

  // TODO: MULE-19762 remove once forward compatibility is finished
  private static String DEFAULT_TEST_SDK_API_VERSION_PROPERTY = SYSTEM_PROPERTY_PREFIX + "testSdkApiVersion";
  private static final String SDK_API_GROUP_ID = "org.mule.sdk";
  private static final String SDK_API_ARTIFACT_ID = "mule-sdk-api";
  private static final String DEFAULT_SDK_API_VERSION = getDefaultSdkApiVersionForTest();
  private static final Artifact DEFAULT_SDK_API_ARTIFACT = new DefaultArtifact(SDK_API_GROUP_ID,
                                                                               SDK_API_ARTIFACT_ID,
                                                                               JAR_EXTENSION,
                                                                               DEFAULT_SDK_API_VERSION);


  private RunnerModuleUtils() {}

  /**
   * Loads the {@link RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} resources files, merges the entries so only one
   * {@link Properties} is returned with all values.
   *
   * @return a {@link Properties} loaded with the content of the file.
   * @throws IOException           if the properties couldn't load the file.
   * @throws IllegalStateException if the file couldn't be found.
   */
  public static Properties getExcludedProperties() throws IllegalStateException, IOException {
    Properties excludedProperties = new Properties();
    discoverProperties(EXCLUDED_PROPERTIES_FILE).stream()
        .forEach(properties -> properties.forEach((k, v) -> excludedProperties.merge(k, v, (v1, v2) -> v1 + "," + v2)));
    return excludedProperties;
  }

  /**
   * @return an {@link Artifact} pointing to the default mule-sdk-api.
   */
  public static Artifact getDefaultSdkApiArtifact() {
    return DEFAULT_SDK_API_ARTIFACT;
  }

  /**
   * Tests the {@code extensionClassLoader} for the presence of the {@code mule-sdk-api} classpath and forces it to load it if
   * missing
   *
   * @param extensionClassLoader the extension's classlaoder
   * @param dependencyResolver   a {@link DependencyResolver}
   * @param repositories         the repositories for fetching the mule-sdk-api if missing in the classloader
   * @since 4.5.0
   */
  // TODO: MULE-19762 remove once forward compatibility is finished
  public static void assureSdkApiInClassLoader(ClassLoader extensionClassLoader,
                                               DependencyResolver dependencyResolver,
                                               List<RemoteRepository> repositories) {
    try {
      Class.forName("org.mule.sdk.api.runtime.parameter.ParameterResolver", true, extensionClassLoader);
    } catch (ClassNotFoundException cnf) {
      try {
        URL sdkApiUrl = dependencyResolver
            .resolveArtifact(getDefaultSdkApiArtifact(), repositories)
            .getArtifact()
            .getFile().getAbsoluteFile().toURL();

        Method method = findMethod(extensionClassLoader.getClass(), "addURL", URL.class);

        // Until Java8, this will be the path taken.
        if (method != null) {
          method.setAccessible(true);
          method.invoke(extensionClassLoader, sdkApiUrl);
          return;
        }


        // For Java 9+ Use bytebuddy to add dynamically all classes from the Jar
        addSdkApiClassesDynamically(extensionClassLoader, sdkApiUrl);

      } catch (Exception e) {
        throw new RuntimeException("Could not assure sdk-api in extension classloader", e);
      }
    }
  }

  private static void addSdkApiClassesDynamically(ClassLoader extensionClassLoader, URL sdkApiUrl) throws IOException {
    boolean classAdded = true;
    Set<String> addedClasses = new HashSet<>();

    // Some classes needs to be loaded in certain order and will fail to do so if tried to be loaded first.
    // This code will try to load all the remaining classes and keep trying until none can be loaded.
    while (classAdded) {
      classAdded = false;
      ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(sdkApiUrl.getFile()));
      for (Map.Entry<TypeDescription, byte[]> classEntry : getSdkClassMap(zipInputStream, addedClasses).entrySet()) {

        try {
          Map<TypeDescription, byte[]> mapEntry = new HashMap<>();
          mapEntry.put(classEntry.getKey(), classEntry.getValue());
          ClassLoadingStrategy.Default.INJECTION.load(extensionClassLoader, mapEntry);
          addedClasses.add(classEntry.getKey().getName());
          classAdded = true;
          LOGGER.debug("Class {} was succesfully added to the extension classloader.", classEntry.getKey().getName());
        } catch (IllegalStateException e) {
          LOGGER.debug(format("Class %s failed to be added to the extension classloader. Error message : %s",
                              classEntry.getKey().getName(), e.getMessage()),
                       e);
        }
      }
    }
  }

  private static Map<TypeDescription, byte[]> getSdkClassMap(ZipInputStream zipInputStream, Set<String> alreadyProcessed)
      throws IOException {
    Map<TypeDescription, byte[]> sdkClasses = new HashMap<>();
    ZipEntry zipEntry = zipInputStream.getNextEntry();
    while (zipEntry != null) {

      if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".class")) {
        String zipEntryName = getZipEntryClassName(zipEntry.getName());
        if (!alreadyProcessed.contains(zipEntryName)) {
          byte[] fileBytes = ByteStreams.toByteArray(zipInputStream);
          sdkClasses.put(new TypeDescription() {

            @Override
            public String getActualName() {
              return null;
            }

            @Override
            public int getModifiers() {
              return 0;
            }

            @Override
            public boolean isFinal() {
              return false;
            }

            @Override
            public boolean isSynthetic() {
              return false;
            }

            @Override
            public SyntheticState getSyntheticState() {
              return null;
            }

            @Override
            public boolean isPublic() {
              return false;
            }

            @Override
            public boolean isProtected() {
              return false;
            }

            @Override
            public boolean isPackagePrivate() {
              return false;
            }

            @Override
            public boolean isPrivate() {
              return false;
            }

            @Override
            public boolean isStatic() {
              return false;
            }

            @Override
            public boolean isDeprecated() {
              return false;
            }

            @Override
            public Ownership getOwnership() {
              return null;
            }

            @Override
            public Visibility getVisibility() {
              return null;
            }

            @Override
            public boolean isEnum() {
              return false;
            }

            @Override
            public EnumerationState getEnumerationState() {
              return null;
            }

            @Override
            public boolean isAbstract() {
              return false;
            }

            @Override
            public boolean isInterface() {
              return false;
            }

            @Override
            public boolean isAnnotation() {
              return false;
            }

            @Override
            public TypeManifestation getTypeManifestation() {
              return null;
            }

            @Override
            public Iterator<TypeDefinition> iterator() {
              return null;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
              return null;
            }

            @Override
            public TypeList.Generic getTypeVariables() {
              return null;
            }

            @Override
            public TypeVariableSource getEnclosingSource() {
              return null;
            }

            @Override
            public boolean isInferrable() {
              return false;
            }

            @Override
            public Generic findVariable(String symbol) {
              return null;
            }

            @Override
            public <T> T accept(Visitor<T> visitor) {
              return null;
            }

            @Override
            public boolean isGenerified() {
              return false;
            }

            @Override
            public String getName() {
              return zipEntryName;
            }

            @Override
            public String getInternalName() {
              return null;
            }

            @Override
            public String getDescriptor() {
              return null;
            }

            @Override
            public String getGenericSignature() {
              return null;
            }

            @Override
            public boolean isVisibleTo(TypeDescription typeDescription) {
              return false;
            }

            @Override
            public boolean isAccessibleTo(TypeDescription typeDescription) {
              return false;
            }

            @Override
            public Generic asGenericType() {
              return null;
            }

            @Override
            public TypeDescription asErasure() {
              return null;
            }

            @Override
            public Generic getSuperClass() {
              return null;
            }

            @Override
            public TypeList.Generic getInterfaces() {
              return null;
            }

            @Override
            public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
              return null;
            }

            @Override
            public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
              return null;
            }

            @Override
            public boolean isInstance(Object value) {
              return false;
            }

            @Override
            public boolean isAssignableFrom(Class<?> type) {
              return false;
            }

            @Override
            public boolean isAssignableFrom(TypeDescription typeDescription) {
              return false;
            }

            @Override
            public boolean isAssignableTo(Class<?> type) {
              return false;
            }

            @Override
            public boolean isAssignableTo(TypeDescription typeDescription) {
              return false;
            }

            @Override
            public boolean isInHierarchyWith(Class<?> type) {
              return false;
            }

            @Override
            public boolean isInHierarchyWith(TypeDescription typeDescription) {
              return false;
            }

            @Override
            public TypeDescription getComponentType() {
              return null;
            }

            @Override
            public Sort getSort() {
              return null;
            }

            @Override
            public String getTypeName() {
              return null;
            }

            @Override
            public StackSize getStackSize() {
              return null;
            }

            @Override
            public boolean isArray() {
              return false;
            }

            @Override
            public boolean isPrimitive() {
              return false;
            }

            @Override
            public boolean represents(Type type) {
              return false;
            }

            @Override
            public TypeDescription getDeclaringType() {
              return null;
            }

            @Override
            public TypeList getDeclaredTypes() {
              return null;
            }

            @Override
            public MethodDescription.InDefinedShape getEnclosingMethod() {
              return null;
            }

            @Override
            public TypeDescription getEnclosingType() {
              return null;
            }

            @Override
            public int getActualModifiers(boolean superFlag) {
              return 0;
            }

            @Override
            public String getSimpleName() {
              return null;
            }

            @Override
            public String getLongSimpleName() {
              return null;
            }

            @Override
            public String getCanonicalName() {
              return null;
            }

            @Override
            public boolean isAnonymousType() {
              return false;
            }

            @Override
            public boolean isLocalType() {
              return false;
            }

            @Override
            public boolean isMemberType() {
              return false;
            }

            @Override
            public PackageDescription getPackage() {
              return null;
            }

            @Override
            public AnnotationList getInheritedAnnotations() {
              return null;
            }

            @Override
            public boolean isSamePackage(TypeDescription typeDescription) {
              return false;
            }

            @Override
            public boolean isPrimitiveWrapper() {
              return false;
            }

            @Override
            public boolean isAnnotationReturnType() {
              return false;
            }

            @Override
            public boolean isAnnotationValue() {
              return false;
            }

            @Override
            public boolean isAnnotationValue(Object value) {
              return false;
            }

            @Override
            public boolean isPackageType() {
              return false;
            }

            @Override
            public int getInnerClassCount() {
              return 0;
            }

            @Override
            public boolean isInnerClass() {
              return false;
            }

            @Override
            public boolean isNestedClass() {
              return false;
            }

            @Override
            public TypeDescription asBoxed() {
              return null;
            }

            @Override
            public TypeDescription asUnboxed() {
              return null;
            }

            @Override
            public Object getDefaultValue() {
              return null;
            }

            @Override
            public TypeDescription getNestHost() {
              return null;
            }

            @Override
            public TypeList getNestMembers() {
              return null;
            }

            @Override
            public boolean isNestHost() {
              return false;
            }

            @Override
            public boolean isNestMateOf(Class<?> type) {
              return false;
            }

            @Override
            public boolean isNestMateOf(TypeDescription typeDescription) {
              return false;
            }

            @Override
            public ClassFileVersion getClassFileVersion() {
              return null;
            }

            @Override
            public RecordComponentList<InDefinedShape> getRecordComponents() {
              return null;
            }

            @Override
            public Generic findExpectedVariable(String symbol) {
              return null;
            }

            @Override
            public TypeList getPermittedSubtypes() {
              return null;
            }

            @Override
            public boolean isCompileTimeConstant() {
              return false;
            }

            @Override
            public boolean isRecord() {
              return false;
            }

            @Override
            public boolean isSealed() {
              return false;
            }

            @Override
            public boolean equals(Object obj) {
              if (obj instanceof TypeDescription) {
                return getName().equals(((TypeDescription) obj).getName());
              }
              return false;
            }

            @Override
            public int hashCode() {
              return getName().hashCode();
            }
          }, fileBytes);
        }
      }
      zipEntry = zipInputStream.getNextEntry();
    }
    return sdkClasses;
  }

  private static String getZipEntryClassName(String zipEntryName) {
    String zipEntryClassName = zipEntryName.replace("/", ".");
    zipEntryClassName = zipEntryClassName.substring(0, zipEntryClassName.lastIndexOf("."));
    return zipEntryClassName;
  }

  /**
   * @return resolves the default version of {@code mule-sdk-api} to add into the container classpath
   * @sine 4.5.0
   */
  // TODO: MULE-19762 remove once forward compatibility is finished
  private static String getDefaultSdkApiVersionForTest() {
    return getProperty(DEFAULT_TEST_SDK_API_VERSION_PROPERTY, "0.4.0");
  }

}
