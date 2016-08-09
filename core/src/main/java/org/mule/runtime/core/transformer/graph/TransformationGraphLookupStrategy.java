/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.transformer.CompositeConverter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Looks for available conversion paths inside a transformation graph.
 */
public class TransformationGraphLookupStrategy {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private DirectedGraph<DataType, TransformationEdge> graph;

  public TransformationGraphLookupStrategy(DirectedGraph<DataType, TransformationEdge> graph) {
    this.graph = graph;
  }

  /**
   * Looks for {@link Converter} to convert from the source to the target data types.
   *
   * @param source data type to be converted
   * @param target data type to be converted to
   * @return a list of {@link Converter} that are able to convert from the source to the target data types.
   */
  public List<Converter> lookupConverters(DataType source, DataType target) {
    List<Converter> converters = new LinkedList<>();
    if (!graph.containsVertex(source)) {
      return converters;
    }

    // Checks if there is a converter with the specified output data type
    if (!graph.containsVertex(target)) {
      return converters;
    }

    Set<DataType> visited = new HashSet<>();

    List<List<TransformationEdge>> transformationPaths = findTransformationPaths(source, target, visited);

    converters = createConverters(transformationPaths);

    return converters;
  }

  private List<Converter> createConverters(List<List<TransformationEdge>> transformationPaths) {
    List<Converter> converters = new LinkedList<>();

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

    return converters;
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

        if (edgeTarget.equals(target)) {
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
