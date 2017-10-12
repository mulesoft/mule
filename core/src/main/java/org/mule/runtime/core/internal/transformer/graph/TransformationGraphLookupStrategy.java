/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Looks for available conversion paths inside a transformation graph.
 */
public class TransformationGraphLookupStrategy {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private TransformationGraph graph;

  public TransformationGraphLookupStrategy(TransformationGraph graph) {
    this.graph = graph;
  }

  /**
   * Looks for {@link Converter} to convert from the source to the target data types.
   * All {@link Converter}s found will have a source that is compatible with {@param source}
   * (since if a converter can convert a super type, it should be able to convert any type that extends it)
   * and a target such that {@param target} isCompatibleWith() the {@link Converter}'s one
   * (since if we want a converter that returns an specific type, it should return exactly that type or any type that extends it.)
   *
   * @param source data type to be converted
   * @param target data type to be converted to
   * @return a list of {@link Converter} that are able to convert from the source to the target data types.
   */
  public List<Converter> lookupConverters(DataType source, DataType target) {
    List<Converter> converters = new LinkedList<>();
    if (!graph.containsVertexOrSuper(source)) {
      return converters;
    }

    if (!graph.containsVertexOrSub(target)) {
      return converters;
    }

    //Since we should have all possible transformations we should check for them all.
    List<DataType> compatibleSourceVertexes = graph.getSuperVertexes(source);
    List<DataType> compatibleTargetVertexes = graph.getSubVertexes(target);

    List<List<TransformationEdge>> transformationPaths = new LinkedList<>();
    for (DataType sourceVertex : compatibleSourceVertexes) {
      for (DataType targetVertex : compatibleTargetVertexes) {
        transformationPaths.addAll(findTransformationPaths(sourceVertex, targetVertex, new HashSet<>()));
      }
    }

    converters = createConverters(transformationPaths);

    return converters;
  }

  private List<Converter> createConverters(List<List<TransformationEdge>> transformationPaths) {
    //Using a set instead of a list for when a path of just one converter is found multiple times.
    Set<Converter> converters = new HashSet<>();

    for (List<TransformationEdge> transformationPath : transformationPaths) {
      Converter[] pathConverters = new Converter[transformationPath.size()];

      int index = 0;
      for (TransformationEdge edge : transformationPath) {
        pathConverters[index++] = edge.getConverter();
      }

      Converter converter;
      if (transformationPath.size() == 1) {
        converter = transformationPath.get(0).getConverter();
      } else {
        converter = new CompositeConverter(pathConverters);
      }
      converters.add(converter);
    }

    return new ArrayList<>(converters);
  }

  private List<List<TransformationEdge>> findTransformationPaths(DataType source, DataType target, Set<DataType> visited) {
    List<List<TransformationEdge>> validTransformationEdges = new LinkedList<>();

    if (visited.contains(source)) {
      return validTransformationEdges;
    } else {
      visited.add(source);

      Set<TransformationEdge> transformationEdges = graph.outgoingEdgesOf(source);
      for (TransformationEdge transformationEdge : transformationEdges) {
        DataType edgeTarget = graph.getEdgeTarget(transformationEdge);

        if (target.isCompatibleWith(edgeTarget)) {
          LinkedList<TransformationEdge> transformationEdges1 = new LinkedList<>();
          transformationEdges1.add(transformationEdge);
          validTransformationEdges.add(transformationEdges1);
        } else {
          List<List<TransformationEdge>> newTransformations = findTransformationPaths(edgeTarget, target, visited);

          for (List<TransformationEdge> transformationEdgeList : newTransformations) {
            transformationEdgeList.add(0, transformationEdge);
            validTransformationEdges.add(transformationEdgeList);
          }
        }
      }

      visited.remove(source);
    }


    return validTransformationEdges;
  }

}
