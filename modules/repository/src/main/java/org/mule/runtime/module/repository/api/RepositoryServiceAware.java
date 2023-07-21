/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.repository.api;

/**
 * Enables {@link RepositoryService} injection.
 *
 * @deprecated on 4.1, use @Inject on a field or setter method of type {@link RepositoryService}
 */
@Deprecated
public interface RepositoryServiceAware {

  void setRepositoryService(RepositoryService repositoryService);

}
