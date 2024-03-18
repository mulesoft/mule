/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.classloader;

import static java.lang.Math.max;
import static java.lang.System.lineSeparator;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.center;
import static org.apache.commons.lang3.StringUtils.repeat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

/**
 * Default implementation of {@link ClassLoaderNode}
 *
 * @since 4.2
 */
public class DefaultClassLoaderNode implements ClassLoaderNode {

  protected ArtifactClassLoader artifactClassLoader;
  protected List<ClassLoaderNode> delegateNodes;
  protected ClassLoaderNode parent;
  private String id;
  private boolean isMulePlugin = false;

  public ArtifactClassLoader getArtifactClassLoader() {
    return artifactClassLoader;
  }

  @Override
  public boolean isMulePlugin() {
    return isMulePlugin;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public List<ClassLoaderNode> getDelegateNodes() {
    return delegateNodes;
  }

  public void setParent(ClassLoaderNode parent) {
    this.parent = parent;
  }

  @Override
  public Optional<ClassLoaderNode> getParent() {
    return ofNullable(parent);
  }

  public static DefaultClassLoaderNodeBuilder builder() {
    return new DefaultClassLoaderNodeBuilder();
  }

  public String toString() {
    Stack<ClassLoaderNode> classLoaderNodes = new Stack<>();
    ClassLoaderNode currentNode = this;

    int maximumIdLength = 0;
    do {
      classLoaderNodes.push(currentNode);
      maximumIdLength = max(maximumIdLength, currentNode.getId().length());
      for (ClassLoaderNode childNode : currentNode.getDelegateNodes()) {
        maximumIdLength = max(maximumIdLength, childNode.getId().length());
      }
    } while ((currentNode = currentNode.getParent().orElse(null)) != null);
    final StringBuilder stringBuilder = new StringBuilder();
    int frameSize = maximumIdLength + 4;
    AtomicBoolean isFirst = new AtomicBoolean(true);
    do {
      ClassLoaderNode classLoaderNode = classLoaderNodes.pop();
      boolean isLastNode = classLoaderNodes.empty();
      if (!isFirst.get()) {
        stringBuilder.append(center("^", frameSize)).append(lineSeparator());
        stringBuilder.append(center("|", frameSize)).append(lineSeparator());
      }
      isFirst.set(false);
      printNodeFrame(stringBuilder, frameSize, classLoaderNode, false, false);
      for (int i = 0; i < classLoaderNode.getDelegateNodes().size(); i++) {
        ClassLoaderNode delegateNode = classLoaderNode.getDelegateNodes().get(i);
        stringBuilder.append(center("|", frameSize)).append(lineSeparator());
        boolean isLastItem = i == classLoaderNode.getDelegateNodes().size() - 1;
        printNodeFrame(stringBuilder, frameSize, delegateNode, true,
                       isLastNode && isLastItem);
      }
    } while (!classLoaderNodes.empty());
    return stringBuilder.toString();
  }

  /**
   * Find the {@link ClassLoaderNode} within this node hierarchy only within the reachable class loaders.
   * <p/>
   * This method assumes that the requested classloader exists in the hierarchy.
   *
   * @param artifactClassLoader the classloader owned by the {@link ClassLoaderNode} to search.
   * @return the found {@link ClassLoaderNode}
   */
  public ClassLoaderNode findClassLoaderNode(ArtifactClassLoader artifactClassLoader) {
    if (getArtifactClassLoader().equals(artifactClassLoader)) {
      return this;
    }
    for (ClassLoaderNode classLoaderNode : getDelegateNodes()) {
      if (classLoaderNode.getArtifactClassLoader().equals(artifactClassLoader)) {
        return classLoaderNode;
      }
    }
    return getParent().get().findClassLoaderNode(artifactClassLoader);
  }

  private void printNodeFrame(StringBuilder stringBuilder, int frameSize, ClassLoaderNode classLoaderNode,
                              boolean includeDelegateLines, boolean doNotAddHierarchyLines) {
    if (includeDelegateLines) {
      stringBuilder.append(repeat(" ", frameSize / 2)).append(repeat("-", frameSize / 2)).append("> ");
    }
    stringBuilder.append(repeat("-", frameSize)).append(lineSeparator());
    if (includeDelegateLines) {
      stringBuilder.append(center(doNotAddHierarchyLines ? " " : "|", frameSize)).append(repeat(" ", 2));
    }
    stringBuilder.append("|").append(center(classLoaderNode.getId(), frameSize - 2)).append("|").append(lineSeparator());
    if (includeDelegateLines) {
      stringBuilder.append(center(doNotAddHierarchyLines ? " " : "|", frameSize)).append(repeat(" ", 2));
    }
    stringBuilder.append(repeat("-", frameSize)).append(lineSeparator());
  }

  public List<ClassLoaderNode> findPossibleResourceOwners(String resourceName) {
    List<ClassLoaderNode> resourceOwners = new ArrayList<>();
    if (!(getArtifactClassLoader() instanceof RegionClassLoader)
        && getArtifactClassLoader().findLocalResource(resourceName) != null) {
      resourceOwners.add(this);
    }
    for (ClassLoaderNode classLoaderNode : getDelegateNodes()) {
      if (!classLoaderNode.isMulePlugin()
          && classLoaderNode.getArtifactClassLoader().findInternalResource(resourceName) != null) {
        resourceOwners.add(classLoaderNode);
      }
    }
    getParent().map(classLoaderNode -> resourceOwners.addAll(classLoaderNode.findPossibleResourceOwners(resourceName)));
    return resourceOwners;
  }

  public List<ClassLoaderNode> findPossibleClassOwners(String className) {
    List<ClassLoaderNode> resourceOwners = new ArrayList<>();
    try {
      if (!(getArtifactClassLoader() instanceof RegionClassLoader)) {
        getArtifactClassLoader().loadInternalClass(className);
        resourceOwners.add(this);
      }
    } catch (ClassNotFoundException e) {
      // Nothing to do, continue.
    }
    for (ClassLoaderNode classLoaderNode : getDelegateNodes()) {
      try {
        if (!classLoaderNode.isMulePlugin()
            && classLoaderNode.getArtifactClassLoader().loadInternalClass(className) != null) {
          resourceOwners.add(classLoaderNode);
        }
      } catch (ClassNotFoundException e) {
        // Nothing to do, continue.
      }
    }
    getParent().map(classLoaderNode -> resourceOwners.addAll(classLoaderNode.findPossibleClassOwners(className)));
    return resourceOwners;
  }

  public static final class DefaultClassLoaderNodeBuilder {

    protected ArtifactClassLoader artifactClassLoader;
    protected List<ClassLoaderNode> delegateNodes = Collections.emptyList();
    protected ClassLoaderNode parent;
    private String id;
    private boolean isMulePlugin = false;

    private DefaultClassLoaderNodeBuilder() {}

    public DefaultClassLoaderNodeBuilder withArtifactClassLoader(ArtifactClassLoader artifactClassLoader) {
      this.artifactClassLoader = artifactClassLoader;
      return this;
    }

    public DefaultClassLoaderNodeBuilder withDelegateNodes(List<ClassLoaderNode> delegateNodes) {
      this.delegateNodes = delegateNodes;
      return this;
    }

    public DefaultClassLoaderNodeBuilder withParent(ClassLoaderNode parent) {
      this.parent = parent;
      return this;
    }

    public DefaultClassLoaderNodeBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public DefaultClassLoaderNodeBuilder withIsMulePlugin(boolean isMulePlugin) {
      this.isMulePlugin = isMulePlugin;
      return this;
    }

    public DefaultClassLoaderNode build() {
      DefaultClassLoaderNode defaultClassLoaderNode = new DefaultClassLoaderNode();
      defaultClassLoaderNode.parent = this.parent;
      defaultClassLoaderNode.delegateNodes = this.delegateNodes;
      defaultClassLoaderNode.artifactClassLoader = this.artifactClassLoader;
      defaultClassLoaderNode.id = this.id;
      defaultClassLoaderNode.isMulePlugin = this.isMulePlugin;
      for (ClassLoaderNode delegateNode : delegateNodes) {
        if (delegateNode instanceof DefaultClassLoaderNode)
          ((DefaultClassLoaderNode) delegateNode).parent = defaultClassLoaderNode;
      }
      return defaultClassLoaderNode;
    }
  }


}
