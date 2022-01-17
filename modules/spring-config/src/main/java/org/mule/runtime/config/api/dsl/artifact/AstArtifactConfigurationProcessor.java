/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.artifact;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;
import static org.mule.runtime.config.internal.ApplicationFilteredFromPolicyArtifactAst.applicationFilteredFromPolicyArtifactAst;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;

import static java.util.Collections.emptySet;
import static java.util.Optional.empty;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.StaticConfigurationPropertiesProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.NullDomainMuleContextLifecycleStrategy;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.runtime.deployment.model.internal.artifact.ImmutableArtifactContext;
import org.mule.runtime.dsl.api.ConfigResource;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ArtifactConfigurationProcessor} that parses the XML configuration files and delegates to
 * {@link ArtifactAstConfigurationBuilder} to create registry and populate the {@link MuleContext}.
 *
 * @since 4.5
 */
public final class AstArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    final String[] configResources = artifactContextConfiguration.getConfigResources();

    if (isEmpty(configResources)) {
      ((DefaultMuleContext) artifactContextConfiguration.getMuleContext())
          .setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
      return new ImmutableArtifactContext(artifactContextConfiguration.getMuleContext());
    }

    ArtifactAstConfigurationBuilder configurationBuilder =
        new ArtifactAstConfigurationBuilder(createApplicationModel(artifactContextConfiguration.getMuleContext(),
                                                                   artifactContextConfiguration.getArtifactDeclaration(),
                                                                   loadConfigResources(configResources),
                                                                   artifactContextConfiguration.getArtifactProperties(),
                                                                   artifactContextConfiguration.getArtifactType(),
                                                                   artifactContextConfiguration.getParentArtifactContext()
                                                                       .map(parentContext -> parentContext.getArtifactAst())
                                                                       .orElse(emptyArtifact()),
                                                                   artifactContextConfiguration.isDisableXmlValidations()),
                                            artifactContextConfiguration.getArtifactProperties(),
                                            artifactContextConfiguration.getArtifactType(),
                                            artifactContextConfiguration.isEnableLazyInitialization());

    artifactContextConfiguration.getParentArtifactContext()
        .ifPresent(parentContext -> configurationBuilder.setParentContext(parentContext.getMuleContext(),
                                                                          parentContext.getArtifactAst()));
    artifactContextConfiguration.getServiceConfigurators().stream()
        .forEach(configurationBuilder::addServiceConfigurator);
    configurationBuilder.configure(artifactContextConfiguration.getMuleContext());
    return configurationBuilder.createArtifactContext();
  }

  private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }

  protected ConfigResource[] loadConfigResources(String[] configs) throws ConfigurationException {
    try {
      ConfigResource[] artifactConfigResources = new ConfigResource[configs.length];
      for (int i = 0; i < configs.length; i++) {
        artifactConfigResources[i] = new ConfigResource(configs[i]);
      }
      return artifactConfigResources;
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  private ArtifactAst createApplicationModel(MuleContext muleContext,
                                             ArtifactDeclaration artifactDeclaration,
                                             ConfigResource[] artifactConfigResources,
                                             Map<String, String> artifactProperties,
                                             ArtifactType artifactType,
                                             ArtifactAst parentArtifactAst,
                                             boolean disableXmlValidations) {
    Set<ExtensionModel> extensions = getExtensions(muleContext.getExtensionManager());
    try {
      final ArtifactAst artifactAst;

      if (artifactDeclaration == null) {
        if (artifactConfigResources.length == 0) {
          artifactAst = emptyArtifact();
        } else {
          final AstXmlParser parser = createMuleXmlParser(muleContext, extensions, artifactProperties, artifactType,
                                                          parentArtifactAst, disableXmlValidations);
          artifactAst = parser.parse(artifactConfigResources);
        }
      } else {
        artifactAst = toArtifactast(artifactDeclaration, extensions);
      }

      return artifactAst;
    } catch (MuleRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private AstXmlParser createMuleXmlParser(MuleContext muleContext,
                                           Set<ExtensionModel> extensions,
                                           Map<String, String> artifactProperties,
                                           ArtifactType artifactType,
                                           ArtifactAst parentArtifactAst,
                                           boolean disableXmlValidations) {
    ConfigurationPropertiesResolver propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(), new StaticConfigurationPropertiesProvider(artifactProperties));

    FeatureFlaggingService featureFlaggingService = getFeatureFlaggingService(muleContext);
    Builder builder = AstXmlParser.builder()
        .withPropertyResolver(propertyKey -> (String) propertyResolver.resolveValue(propertyKey))
        .withExtensionModels(extensions)
        .withParentArtifact(resolveParentArtifact(parentArtifactAst, artifactType, featureFlaggingService));
    if (!featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)) {
      builder.withLegacyFailStrategy();
    }
    if (disableXmlValidations) {
      builder.withSchemaValidationsDisabled();
    }

    switch (artifactType) {
      case APP:
        builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.APPLICATION);
        break;
      case DOMAIN:
        builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.DOMAIN);
        break;
      case POLICY:
        builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.POLICY);
        break;
      default:
        break;
    }

    return builder.build();
  }

  private FeatureFlaggingService getFeatureFlaggingService(MuleContext muleContext) {
    Registry originalRegistry = ((MuleRegistryHelper) (((MuleContextWithRegistry) muleContext).getRegistry())).getDelegate();
    return originalRegistry.lookupObject(FEATURE_FLAGGING_SERVICE_KEY);
  }

  private ArtifactAst resolveParentArtifact(ArtifactAst parentArtifactAst, ArtifactType artifactType,
                                            FeatureFlaggingService featureFlaggingService) {
    if (POLICY.equals(artifactType)) {
      return applicationFilteredFromPolicyArtifactAst(parentArtifactAst, featureFlaggingService);
    }
    return parentArtifactAst;
  }


}
