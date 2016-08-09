/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;

/**
 * Represents a node on a file system tree.
 * <p>
 * It contains an {@link OperationResult} which represents the file {@code this} node points to.
 * <p>
 * Because {@code this} node itself can point to a directory, it also contains a {@link List} of {@link #getChilds() childs).
 * <p>
 * The node can also optionally have a {@link #getContent() content}, which will return {@code null} if the node points to a
 * directory.
 * <p>
 * This class also implements the {@link Iterable} interface making it suitable for use with componets such as {@code foreach} or
 * {@code batch}. The methods in such interface are implemented by delegating into the {@link #getChilds()} list.
 * <p>
 * Instances are immutable and are to be created through a {@link Builder} instance.
 *
 * @since 4.0
 */
public class TreeNode implements Serializable, Iterable {

  private transient final OperationResult<?, FileAttributes> info;
  private final List<TreeNode> childs;

  private TreeNode(OperationResult<?, FileAttributes> info, List<TreeNode> childs) {
    this.info = info;
    this.childs = childs;
  }

  /**
   * @return a immutable {@link List} with {@code this} node's childs
   */
  public List<TreeNode> getChilds() {
    return childs;
  }

  /**
   * @return a {@link FileAttributes} object with the node's metadata
   */
  public FileAttributes getAttributes() {
    return info.getAttributes().get();
  }

  /**
   * @return a {@link DataType} with type metadata about the referenced node
   */
  public MediaType getMediaType() {
    return info.getMediaType().get();
  }

  /**
   * @return an {@link InputStream} if the node is a file, or {@code null} if it's a directory
   */
  public InputStream getContent() {
    if (info.getOutput() instanceof InputStream) {
      return (InputStream) info.getOutput();
    }

    return null;
  }

  /**
   * @return the {@link #getChilds()} {@link Iterator}
   */
  @Override
  public Iterator iterator() {
    return getChilds().iterator();
  }

  /**
   * @return the {@link #getChilds()} {@link Spliterator}
   */
  @Override
  public Spliterator spliterator() {
    return getChilds().spliterator();
  }

  /**
   * Implementation of the buidler design pattern to create instances of {@link TreeNode}
   */
  public static class Builder {

    private final List<Builder> childs = new LinkedList<>();
    private OperationResult<?, FileAttributes> info;
    private TreeNode instance;

    private Builder() {}

    /**
     * Obtains a new {@link Builder} instance to be used to create a {@link TreeNode} which references a directory
     *
     * @param attributes a {@link FileAttributes} object with the directory's metadata
     * @return a new {@link Builder}
     */
    public static Builder forDirectory(FileAttributes attributes) {
      Builder builder = new Builder();
      builder.info = OperationResult.<Object, FileAttributes>builder().output(null).attributes(attributes).build();

      return builder;
    }

    /**
     * Obtains a new {@link Builder} instance to be used to create a {@link TreeNode} which references a file
     *
     * @param message a {@link MuleMessage} which payload is an {@link InputStream} with the file content and a
     *        {@link FileAttributes} instance as attributes
     * @return a new {@link Builder}
     */
    public static Builder forFile(OperationResult<InputStream, FileAttributes> message) {
      Builder builder = new Builder();
      builder.info = message;

      return builder;
    }

    /**
     * Adds a new child {@link TreeNode} by providing a {@link Builder} that will generate it
     *
     * @param nodeBuilder a {@link Builder} which will create the child node
     * @return {@code this} instance
     */
    public Builder addChild(Builder nodeBuilder) {
      childs.add(nodeBuilder);
      return this;
    }

    /**
     * Returns the created {@link TreeNode}. If invoked several times over the same instance, the same value will always be
     * returned
     *
     * @return the build {@link TreeNode}
     */
    public TreeNode build() {
      if (instance == null) {
        instance = new TreeNode(info, childs.stream().map(Builder::build).collect(new ImmutableListCollector<>()));
      }
      return instance;
    }

  }
}
