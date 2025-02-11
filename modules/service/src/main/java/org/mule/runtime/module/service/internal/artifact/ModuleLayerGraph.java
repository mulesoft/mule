/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.artifact;

import org.mule.runtime.module.artifact.api.classloader.ModuleLayerInformationSupplier;

import static java.lang.Math.max;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.center;
import static org.apache.commons.lang3.StringUtils.repeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Graph that generates a string representation of the {@link ModuleLayer}s' hierarchy.
 * 
 * @since 4.6
 */
public class ModuleLayerGraph implements ModuleLayerInformationSupplier {

  private static final String REPETITION = "*";
  private static final Map<ModuleLayer, String> ids = new HashMap<>();
  private final Map<ModuleLayer, List<ModuleLayer>> nodes = new HashMap<>();
  private final ModuleLayer rootNode;

  public ModuleLayerGraph(ModuleLayer layer) {
    addModuleLayerAndParents(layer);
    this.rootNode = layer;
  }

  /**
   * Adds the id for a given Layer to use in the string representation.
   * 
   * @param layer the ModuleLayer to set the id to.
   * @param id
   */
  public static void setModuleLayerId(ModuleLayer layer, String id) {
    ids.put(layer, id);
  }

  private void addModuleLayerAndParents(ModuleLayer layer) {
    // Adds this module layer, as well as its parents, to this graph, unless it's a filtered layer
    List<ModuleLayer> parents = new ArrayList<>();
    this.nodes.put(layer, parents);
    if (isFilteredLayer(layer)) {
      return;
    } else {
      String name = ids.getOrDefault(layer, Integer.toString(this.nodes.size()));
      ids.computeIfAbsent(layer, key -> name);
    }
    for (ModuleLayer parent : layer.parents()) {
      if (!this.nodes.containsKey(parent)) {
        addModuleLayerAndParents(parent);
      }
      parents.add(parent);
    }
  }

  public String graphString() {
    // We go level by level adding the layers on each case.
    LinkedList<ModuleLayer> moduleLayers = new LinkedList<>();

    int maximumIdLength = getMaxLength(rootNode, new HashSet<>());
    int frameSize = maximumIdLength + 5;

    // First level only includes the root node
    moduleLayers.add(rootNode);
    final StringBuilder stringBuilder = new StringBuilder();

    // We have to consider that there might be repeated nodes (different layers may have
    // the same parent). In that case, we just add (to the representation) that direct parent
    // with an '*' character, and not its corresponding parents. This 2 Maps are used for that:
    // know in which level we should consider at which level the node should actually be fully added.
    // We will fully add (with the parents) in the deepest apparition.
    Map<ModuleLayer, Integer> printAtLevel = new HashMap<>();
    Map<ModuleLayer, Integer> maxPrintAtLevel = new HashMap<>();
    dfsDefinePrintLevel(rootNode, printAtLevel, maxPrintAtLevel, 0);
    List<Set<ModuleLayer>> layersToPrintByLevel = layerPerLevel(printAtLevel);

    int currentLevel = 0;

    while (!moduleLayers.isEmpty()) {
      LinkedList<ModuleLayer> nextLevel = new LinkedList<>();

      Set<ModuleLayer> correspondingToLevel = layersToPrintByLevel.get(currentLevel);
      sortLayersByDepth(moduleLayers, correspondingToLevel, maxPrintAtLevel);
      // We add all the frames for this level:
      printNodeFrame(stringBuilder, frameSize, moduleLayers, correspondingToLevel);
      // We add all the corresponding lines for the parents
      printNodeDelegates(stringBuilder, frameSize, moduleLayers, correspondingToLevel);
      while (!moduleLayers.isEmpty()) {
        // For the next layer, we add the parents of the current one, filtering in case it's a filtered layer
        ModuleLayer current = moduleLayers.pop();
        if (correspondingToLevel.contains(current)) {
          current.parents().stream().filter(layer -> !isFilteredLayer(layer)).forEach(nextLevel::add);
        }
      }
      moduleLayers = nextLevel;
      currentLevel++;
    }
    return stringBuilder.toString();
  }

  /**
   * @return the max length of all the ids.
   */
  private int getMaxLength(ModuleLayer moduleLayer, Set<ModuleLayer> analysed) {
    int maximumIdLength = ids.get(moduleLayer).length();
    for (ModuleLayer parent : nodes.get(moduleLayer)) {
      if (isFilteredLayer(parent)) {
        continue;
      }
      if (!analysed.contains(moduleLayer)) {
        maximumIdLength = max(maximumIdLength, getMaxLength(parent, analysed));
      }
    }
    analysed.add(moduleLayer);
    return maximumIdLength;
  }

  private void printNodeFrame(StringBuilder stringBuilder, int frameSize, List<ModuleLayer> moduleLayers,
                              Set<ModuleLayer> printables) {
    for (int i = 0; i < moduleLayers.size(); i++) {
      stringBuilder.append(repeat("-", frameSize));
      if (i < moduleLayers.size() - 1) {
        stringBuilder.append("  ");
      }
    }
    stringBuilder.append(lineSeparator());
    // To avoid case of a layer that happens to appear more than once in the same level
    Set<ModuleLayer> alreadyAnalyzed = new HashSet<>();

    for (int i = 0; i < moduleLayers.size(); i++) {
      ModuleLayer layer = moduleLayers.get(i);
      stringBuilder.append("|")
          .append(center(ids.get(layer) + (printables.contains(layer) && !alreadyAnalyzed.contains(layer) ? "" : REPETITION),
                         frameSize - 2))
          .append("|");
      if (i < moduleLayers.size() - 1) {
        stringBuilder.append("  ");
      }
      alreadyAnalyzed.add(layer);
    }
    stringBuilder.append(lineSeparator());
    for (int i = 0; i < moduleLayers.size(); i++) {
      stringBuilder.append(repeat("-", frameSize));
      if (i < moduleLayers.size() - 1) {
        stringBuilder.append("  ");
      }
    }
    stringBuilder.append(lineSeparator());
  }

  private void printNodeDelegates(StringBuilder stringBuilder, int frameSize, List<ModuleLayer> moduleLayers,
                                  Set<ModuleLayer> printParentsOf) {
    // We get the position for every node's parents.
    List<List<Integer>> positionOfParents = new ArrayList<>();
    int totalParents = parentInfo(moduleLayers, printParentsOf, positionOfParents);
    if (totalParents == 0) {
      // if there are no parents, there is nothing to be done
      return;
    }

    // For each node, we add the arrows for the next layer to link to their parents
    int accumulatedParents = 0;
    for (int i = 0; i < moduleLayers.size(); i++) {
      stringBuilder.append(repeat(" ", frameSize / 2));
      int parents = positionOfParents.get(i).size();
      int parentsToRight = 0;
      if (parents > 0) {
        stringBuilder.append("|");
        parentsToRight = parents - (accumulatedParents >= positionOfParents.get(i).get(0) ? 1 : 0);
      } else {
        stringBuilder.append(" ");
      }

      if (parentsToRight > 0) {
        stringBuilder.append(repeat("-", frameSize * parentsToRight));
      }
      accumulatedParents += positionOfParents.get(i).size();
      if (accumulatedParents < totalParents) {
        stringBuilder.append(repeat(" ", frameSize / 2));
      }
    }
    stringBuilder.append(lineSeparator());

    addArrowsString(stringBuilder, totalParents, frameSize);
  }

  private int parentInfo(List<ModuleLayer> moduleLayers, Set<ModuleLayer> printParentsOf, List<List<Integer>> positionOfParents) {
    int current = 0;
    for (ModuleLayer moduleLayer : moduleLayers) {
      List<Integer> pos = new ArrayList<>();
      positionOfParents.add(pos);
      if (!printParentsOf.contains(moduleLayer)) {
        continue;
      }
      for (ModuleLayer parent : moduleLayer.parents()) {
        if (!isFilteredLayer(parent)) {
          pos.add(current);
          current++;
        }
      }
    }
    return current;
  }

  private void addArrowsString(StringBuilder stringBuilder, int totalParents, int frameSize) {
    for (int i = 0; i < totalParents; i++) {
      stringBuilder.append(repeat(" ", frameSize / 2));
      stringBuilder.append("|");
      if (i < totalParents - 1) {
        stringBuilder.append(repeat(" ", frameSize / 2));
      }
    }
    stringBuilder.append(lineSeparator());
    for (int i = 0; i < totalParents; i++) {
      stringBuilder.append(repeat(" ", frameSize / 2));
      stringBuilder.append("V");
      if (i < totalParents - 1) {
        stringBuilder.append(repeat(" ", frameSize / 2));
      }
    }
    stringBuilder.append(lineSeparator());
  }

  private void dfsDefinePrintLevel(ModuleLayer current, Map<ModuleLayer, Integer> levelToPrint,
                                   Map<ModuleLayer, Integer> maxLevelRoot, int currentLevel) {
    if (!levelToPrint.containsKey(current) || levelToPrint.get(current) < currentLevel) {
      levelToPrint.put(current, currentLevel);
      maxLevelRoot.put(current, currentLevel);
    }
    for (ModuleLayer parent : current.parents()) {
      dfsDefinePrintLevel(parent, levelToPrint, maxLevelRoot, currentLevel + 1);
      if (maxLevelRoot.get(parent) > maxLevelRoot.get(current)) {
        maxLevelRoot.put(current, maxLevelRoot.get(parent));
      }
    }
  }

  private List<Set<ModuleLayer>> layerPerLevel(Map<ModuleLayer, Integer> levelToPrint) {
    int max = 0;
    for (Integer level : levelToPrint.values()) {
      if (level > max) {
        max = level;
      }
    }
    List<Set<ModuleLayer>> result = new ArrayList<>();
    for (int i = 0; i <= max; i++) {
      result.add(new HashSet<>());
    }

    levelToPrint.forEach((layer, value) -> result.get(value).add(layer));
    return result;
  }

  public String moduleLayerModules() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("Modules in each Module Layer:");
    stringBuilder.append(lineSeparator());
    Set<ModuleLayer> checked = new HashSet<>();
    LinkedList<ModuleLayer> queue = new LinkedList<>();
    queue.add(this.rootNode);
    checked.add(this.rootNode);
    while (!queue.isEmpty()) {
      ModuleLayer layer = queue.pollFirst();
      stringBuilder.append(ids.get(layer));
      stringBuilder.append(": ");
      if (!layer.modules().isEmpty()) {
        stringBuilder.append(layer.modules().stream().map(Module::getName).collect(joining(", ")));
      } else {
        stringBuilder.append("(Empty Layer)");
      }
      stringBuilder.append(lineSeparator());
      for (ModuleLayer parent : layer.parents()) {
        if (ids.containsKey(parent) && !checked.contains(parent)) {
          checked.add(parent);
          queue.add(parent);
        }
      }
    }
    return stringBuilder.toString();
  }

  protected boolean isFilteredLayer(ModuleLayer layer) {
    return layer.equals(ModuleLayer.boot());
  }

  private void sortLayersByDepth(List<ModuleLayer> moduleLayers, Set<ModuleLayer> processedInThisLevel,
                                 Map<ModuleLayer, Integer> depth) {
    moduleLayers.sort((m1, m2) -> {
      if (processedInThisLevel.contains(m2)) {
        return 1;
      } else if (processedInThisLevel.contains(m1)) {
        return -1;
      } else {
        return depth.get(m1) - depth.get(m2);
      }
    });
    Set<ModuleLayer> alreadyAppeared = new HashSet<>();
    List<ModuleLayer> moveToEnd = new ArrayList<>();

    for (ListIterator<ModuleLayer> iter = moduleLayers.listIterator(); iter.hasNext();) {
      ModuleLayer current = iter.next();
      if (alreadyAppeared.contains(current)) {
        moveToEnd.add(current);
        iter.remove();
      } else {
        alreadyAppeared.add(current);
      }
    }
    moduleLayers.addAll(moveToEnd);
  }

  @Override
  public String retrieveRepresentation() {
    return graphString() +
        lineSeparator() +
        moduleLayerModules();
  }
}
