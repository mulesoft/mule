/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.api;

import java.util.List;

/**
 * Enables injection of all available {@link MuleCoreExtension}.
 * <p/>
 * Classes implementing this interface cannot contain methods annotated with {@link MuleCoreExtensionDependency} as that will
 * create an unresolvable dependency on runtime.
 *
 * @deprecated on 4.1, use @Inject on a field or setter method of type {@link List<MuleCoreExtension>}
 */
@Deprecated
public interface CoreExtensionsAware {

  void setCoreExtensions(List<MuleCoreExtension> coreExtensions);
}
