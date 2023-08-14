/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.classloading.api.ClassLoadingHelper;
import org.mule.test.classloading.api.validation.ClassLoadingValidationsProvider;

@Extension(name = "ClassLoading")
@Xml(prefix = "classloading")
@ConnectionProviders({CLPoolingConnectionProvider.class, CLCachedConnectionProvider.class, CLNoneConnectionProvider.class})
@Configurations({CLConfiguration.class, CLInvalidConfiguration.class})
@Export(
    classes = {ClassLoadingHelper.class, ClassLoadingValidationsProvider.class,
        ArtifactAstDependencyGraphProviderValidationsProvider.class},
    resources = {"META-INF/services/org.mule.runtime.ast.api.validation.ValidationsProvider"})
public class ClassLoadingExtension {

}
