/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.artifact;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.NullDomainMuleContextLifecycleStrategy;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.runtime.deployment.model.internal.artifact.ImmutableArtifactContext;

/**
 * Spring implementation of {@link ArtifactConfigurationProcessor} that parses the XML configuration files and generates the
 * runtime object using the spring bean container.
 *
 * @since 4.0
 */
public final class SpringArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    final String[] configResources = artifactContextConfiguration.getConfigResources();

    if (isEmpty(configResources)) {
      ((DefaultMuleContext) artifactContextConfiguration.getMuleContext())
          .setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
      return new ImmutableArtifactContext(artifactContextConfiguration.getMuleContext());
    }

    // TODO how to deal with the FFR?
    // new ArtifactAstConfigurationBuilder(createApplicationModel(getExtensions(artifactContextConfiguration.getMuleContext()
    // .getExtensionManager()),
    // artifactContextConfiguration.getArtifactDeclaration(),
    // configResources,
    // artifactContextConfiguration.getArtifactProperties(),
    // artifactContextConfiguration.isDisableXmlValidations(),
    // null),
    // null, null, false);
    //

    SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
        new SpringXmlConfigurationBuilder(configResources,
                                          artifactContextConfiguration.getArtifactDeclaration(),
                                          artifactContextConfiguration.getArtifactProperties(),
                                          artifactContextConfiguration.getArtifactType(),
                                          artifactContextConfiguration.isEnableLazyInitialization(),
                                          artifactContextConfiguration.isDisableXmlValidations(),
                                          artifactContextConfiguration.getRuntimeLockFactory(),
                                          artifactContextConfiguration.getMemoryManagementService());
    artifactContextConfiguration.getParentArtifactContext()
        .ifPresent(parentContext -> springXmlConfigurationBuilder.setParentContext(parentContext.getMuleContext(),
                                                                                   parentContext.getArtifactAst()));
    artifactContextConfiguration.getServiceConfigurators().stream()
        .forEach(springXmlConfigurationBuilder::addServiceConfigurator);
    springXmlConfigurationBuilder.configure(artifactContextConfiguration.getMuleContext());
    return springXmlConfigurationBuilder.createArtifactContext();
  }

  // private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
  // return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  // }
  //
  // private ArtifactAst createApplicationModel(Set<ExtensionModel> extensions,
  // ArtifactDeclaration artifactDeclaration,
  // ConfigResource[] artifactConfigResources,
  // Map<String, String> artifactProperties,
  // boolean disableXmlValidations,
  // FeatureFlaggingService featureFlaggingService) {
  // try {
  // final ArtifactAst artifactAst;
  //
  // if (artifactDeclaration == null) {
  // if (artifactConfigResources.length == 0) {
  // artifactAst = emptyArtifact();
  // } else {
  // final AstXmlParser parser =
  // createMuleXmlParser(extensions, artifactProperties, disableXmlValidations, featureFlaggingService);
  // artifactAst = parser.parse(artifactConfigResources);
  // }
  // } else {
  // artifactAst = toArtifactast(artifactDeclaration, extensions);
  // }
  //
  // return artifactAst;
  // } catch (MuleRuntimeException e) {
  // throw e;
  // } catch (Exception e) {
  // throw new MuleRuntimeException(e);
  // }
  // }
  //
  // private AstXmlParser createMuleXmlParser(Set<ExtensionModel> extensions,
  // Map<String, String> artifactProperties, boolean disableXmlValidations,
  // FeatureFlaggingService featureFlaggingService) {
  // ConfigurationPropertiesResolver propertyResolver =
  // new DefaultConfigurationPropertiesResolver(empty(), new StaticConfigurationPropertiesProvider(artifactProperties));
  //
  // Builder builder = AstXmlParser.builder()
  // .withPropertyResolver(propertyKey -> (String) propertyResolver.resolveValue(propertyKey))
  // .withExtensionModels(extensions)
  // .withParentArtifact(resolveParentArtifact(featureFlaggingService));
  // if (!featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)) {
  // builder.withLegacyFailStrategy();
  // }
  // if (disableXmlValidations) {
  // builder.withSchemaValidationsDisabled();
  // }
  //
  // switch (artifactType) {
  // case APP:
  // builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.APPLICATION);
  // break;
  // case DOMAIN:
  // builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.DOMAIN);
  // break;
  // case POLICY:
  // builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.POLICY);
  // break;
  // default:
  // break;
  // }
  //
  // return builder.build();
  // }

}
