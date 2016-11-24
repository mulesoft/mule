/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.apache.maven.repository.internal.MavenRepositorySystemUtils.newSession;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;
import org.mule.test.runner.classification.DefaultWorkspaceReader;
import org.mule.test.runner.classification.LoggerRepositoryListener;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

/**
 * Factory to create a {@link RepositorySystem} from Eclipse Aether to work in {@code offline} mode and resolve dependencies
 * through a {@link org.eclipse.aether.repository.WorkspaceReader} and a {@link LocalRepository}.
 * <p/>
 * Any missing {@link Artifact} while resolving the dependency graph will be logged along with the path to these unresolved
 * artifacts.
 * <p/>
 * It is assumed that before this is used either Maven triggered the build and resolved dependencies, downloaded and installed
 * them in the local repository, or the IDE downloaded and installed them using Maven plugins.
 * <p/>
 * It also supports a {@link org.eclipse.aether.repository.WorkspaceReader} to resolve from Workspace artifacts that were not yet
 * packaged (multi-module projects in Maven or project references in IDE) if a {@link WorkspaceLocationResolver} is provided.
 *
 * @since 4.0
 */
public class RepositorySystemFactory {

  /**
   * Creates an instance of the {@link RepositorySystemFactory} to collect Maven dependencies.
   *
   * @param classPath {@link URL}'s from class path
   * @param workspaceLocationResolver {@link WorkspaceLocationResolver} to resolve artifactId's {@link Path}s from workspace. Not
   *        {@code null}.
   */
  public static DependencyResolver newOfflineDependencyResolver(List<URL> classPath,
                                                                WorkspaceLocationResolver workspaceLocationResolver,
                                                                File mavenLocalRepositoryLocation) {
    DefaultRepositorySystemSession session = newDefaultRepositorySystemSession();
    session.setOffline(true);
    session.setIgnoreArtifactDescriptorRepositories(true);
    RepositorySystem system = newRepositorySystem(classPath, workspaceLocationResolver, mavenLocalRepositoryLocation, session);
    return new DependencyResolver(system, session);
  }

  /**
   * Creates an instance of the {@link RepositorySystemFactory} to collect Maven dependencies.
   *
   * @param classPath {@link URL}'s from class path
   * @param workspaceLocationResolver {@link WorkspaceLocationResolver} to resolve artifactId's {@link Path}s from workspace. Not
   *        {@code null}.
   */
  public static DependencyResolver newOnlineDependencyResolver(List<URL> classPath,
                                                               WorkspaceLocationResolver workspaceLocationResolver,
                                                               File mavenLocalRepositoryLocation) {
    DefaultRepositorySystemSession session = newDefaultRepositorySystemSession();
    RepositorySystem system = newRepositorySystem(classPath, workspaceLocationResolver, mavenLocalRepositoryLocation, session);
    return new DependencyResolver(system, session);
  }

  private static DefaultRepositorySystemSession newDefaultRepositorySystemSession() {
    DefaultRepositorySystemSession session = newSession();

    session.setUpdatePolicy(UPDATE_POLICY_NEVER);
    session.setChecksumPolicy(CHECKSUM_POLICY_IGNORE);
    return session;
  }

  private static RepositorySystem newRepositorySystem(List<URL> classPath, WorkspaceLocationResolver workspaceLocationResolver,
                                                      File mavenLocalRepositoryLocation, DefaultRepositorySystemSession session) {
    RepositorySystem system = newRepositorySystem();

    // We have to set to use a "simple" aether local repository so it will not cache artifacts (enhanced is supported for doing
    // operations such install).
    LocalRepository localRepo = new LocalRepository(mavenLocalRepositoryLocation, "simple");
    session
        .setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
    session.setWorkspaceReader(new DefaultWorkspaceReader(classPath, workspaceLocationResolver));

    session.setRepositoryListener(new LoggerRepositoryListener());
    return system;
  }

  /**
   * Creates and configures the {@link RepositorySystem} to use for resolving transitive dependencies.
   *
   * @return {@link RepositorySystem}
   */
  private static RepositorySystem newRepositorySystem() {
    /*
     * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the pre populated
     * DefaultServiceLocator, we only MavenXpp3Reader need to register the repository connector and transporter factories.
     */
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {

      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        exception.printStackTrace();
      }
    });

    return locator.getService(RepositorySystem.class);
  }



}
