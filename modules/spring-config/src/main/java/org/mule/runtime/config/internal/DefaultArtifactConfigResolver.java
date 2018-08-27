/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.IMPORT_ELEMENT;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.api.dsl.processor.ConfigFile;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.api.dsl.processor.SimpleConfigAttribute;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.EnvironmentPropertiesConfigurationProvider;
import org.mule.runtime.config.internal.dsl.model.config.RuntimeConfigurationException;
import org.mule.runtime.core.api.config.ConfigResource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.w3c.dom.Document;

/**
 * Default implementation for {@link ArtifactConfigResolver}.
 */
public class DefaultArtifactConfigResolver implements ArtifactConfigResolver {

  @Override
  public ArtifactConfig resolveArtifactConfig(ArtifactConfigResolverContext context) {
    ArtifactConfig.Builder applicationConfigBuilder = new ArtifactConfig.Builder();
    applicationConfigBuilder.setArtifactProperties(context.getArtifactProperties());

    if (!isEmpty(context.getArtifactConfigResources())) {
      List<Pair<String, Supplier<InputStream>>> initialConfigFiles = new ArrayList<>();
      for (ConfigResource artifactConfigResource : context.getArtifactConfigResources()) {
        initialConfigFiles.add(new Pair<>(artifactConfigResource.getResourceName(), () -> {
          try {
            return artifactConfigResource.getInputStream();
          } catch (IOException e) {
            throw new MuleRuntimeException(e);
          }
        }));
      }

      List<ConfigFile> configFiles = new ArrayList<>();
      recursivelyResolveConfigFiles(initialConfigFiles, configFiles, context).forEach(applicationConfigBuilder::addConfigFile);
    }

    applicationConfigBuilder.setApplicationName(context.getArtifactName());
    return applicationConfigBuilder.build();
  }

  private List<ConfigFile> recursivelyResolveConfigFiles(List<Pair<String, Supplier<InputStream>>> configFilesToResolve,
                                                         List<ConfigFile> alreadyResolvedConfigFiles,
                                                         ArtifactConfigResolverContext context) {

    DefaultConfigurationPropertiesResolver propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(), new EnvironmentPropertiesConfigurationProvider());

    ImmutableList.Builder<ConfigFile> resolvedConfigFilesBuilder =
        ImmutableList.<ConfigFile>builder().addAll(alreadyResolvedConfigFiles);
    configFilesToResolve.stream()
        .filter(fileNameInputStreamPair -> !alreadyResolvedConfigFiles.stream()
            .anyMatch(configFile -> configFile.getFilename().equals(fileNameInputStreamPair.getFirst())))
        .forEach(fileNameInputStreamPair -> {
          InputStream is = null;
          try {
            is = fileNameInputStreamPair.getSecond().get();
            Document document =
                context.getXmlConfigurationDocumentLoader().loadDocument(context.getExtensions(),
                                                                         fileNameInputStreamPair.getFirst(),
                                                                         is);
            ConfigLine mainConfigLine = context.getXmlApplicationParser().parse(document.getDocumentElement()).get();
            ConfigFile configFile = new ConfigFile(fileNameInputStreamPair.getFirst(), asList(mainConfigLine));
            resolvedConfigFilesBuilder.add(configFile);
          } finally {
            if (is != null) {
              try {
                is.close();
              } catch (IOException e) {
                throw new MuleRuntimeException(e);
              }
            }
          }
        });

    ImmutableSet.Builder<String> importedFiles = ImmutableSet.builder();
    for (ConfigFile configFile : resolvedConfigFilesBuilder.build()) {
      List<ConfigLine> rootConfigLines = configFile.getConfigLines();
      ConfigLine muleRootElementConfigLine = rootConfigLines.get(0);
      importedFiles.addAll(muleRootElementConfigLine.getChildren().stream()
          .filter(configLine -> configLine.getNamespace().equals(CORE_PREFIX)
              && configLine.getIdentifier().equals(IMPORT_ELEMENT))
          .map(configLine -> {
            SimpleConfigAttribute fileConfigAttribute = configLine.getConfigAttributes().get("file");
            if (fileConfigAttribute == null) {
              throw new RuntimeConfigurationException(
                                                      createStaticMessage(format("<import> does not have a file attribute defined. At file '%s', at line %s",
                                                                                 configFile.getFilename(),
                                                                                 configLine.getLineNumber())));
            }
            return fileConfigAttribute.getValue();
          })
          .map(value -> (String) propertyResolver.resolveValue(value))
          .filter(fileName -> !alreadyResolvedConfigFiles.stream()
              .anyMatch(solvedConfigFile -> solvedConfigFile.getFilename().equals(fileName)))
          .collect(toList()));
    }

    Set<String> importedConfigurationFiles = importedFiles.build();

    if (importedConfigurationFiles.isEmpty()) {
      return resolvedConfigFilesBuilder.build();
    }

    List<Pair<String, Supplier<InputStream>>> newConfigFilesToResolved = importedConfigurationFiles.stream()
        .map(importedFileName -> {
          ClassLoader classLoader = context.getExecutionClassLoader();
          if (classLoader.getResource(importedFileName) == null) {
            throw new RuntimeConfigurationException(createStaticMessage(format("Could not find imported resource '%s'",
                                                                               importedFileName)));
          }
          return (Pair<String, Supplier<InputStream>>) new Pair(importedFileName, (Supplier<InputStream>) () -> classLoader
              .getResourceAsStream(importedFileName));
        }).collect(toList());

    return recursivelyResolveConfigFiles(newConfigFilesToResolved, resolvedConfigFilesBuilder.build(), context);
  }

}
