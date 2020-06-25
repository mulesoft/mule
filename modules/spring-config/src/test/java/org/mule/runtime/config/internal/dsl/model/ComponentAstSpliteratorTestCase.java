/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.lang.Thread.currentThread;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import static org.mule.runtime.ast.api.util.AstTraversalDirection.BOTTOM_UP;
import static org.mule.runtime.ast.api.util.AstTraversalDirection.TOP_DOWN;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.ComponentModel.Builder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ComponentAstSpliteratorTestCase extends AbstractMuleTestCase {

  @Test
  public void singleLevel() {
    ComponentModel.Builder rootBuilder = baseComponentBuilder("root");

    ComponentModel child1 = baseComponentBuilder("1").build();
    rootBuilder.addChildComponentModel(child1);
    ComponentModel child2 = baseComponentBuilder("2").build();
    rootBuilder.addChildComponentModel(child2);

    ComponentModel root = rootBuilder.build();

    final List<ComponentAst> visitedComponents = new ArrayList<>();
    root.recursiveStream().forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(root), sameInstance(child1), sameInstance(child2)));

    visitedComponents.clear();
    root.recursiveStream(TOP_DOWN).forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(root), sameInstance(child1), sameInstance(child2)));

    visitedComponents.clear();
    root.recursiveStream(BOTTOM_UP).forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(child1), sameInstance(child2), sameInstance(root)));
  }

  @Test
  public void twoLevels() {
    ComponentModel.Builder rootBuilder = baseComponentBuilder("root");

    Builder child1Builder = baseComponentBuilder("1");
    ComponentModel child11 = baseComponentBuilder("11").build();
    child1Builder.addChildComponentModel(child11);
    ComponentModel child12 = baseComponentBuilder("12").build();
    child1Builder.addChildComponentModel(child12);
    ComponentModel child1 = child1Builder.build();
    rootBuilder.addChildComponentModel(child1);

    Builder child2Builder = baseComponentBuilder("2");
    ComponentModel child21 = baseComponentBuilder("21").build();
    child2Builder.addChildComponentModel(child21);
    ComponentModel child22 = baseComponentBuilder("22").build();
    child2Builder.addChildComponentModel(child22);
    ComponentModel child2 = child2Builder.build();
    rootBuilder.addChildComponentModel(child2);

    ComponentModel root = rootBuilder.build();

    final List<Object> visitedComponents = new ArrayList<>();
    root.recursiveStream().forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(root),
                                           sameInstance(child1), sameInstance(child11), sameInstance(child12),
                                           sameInstance(child2), sameInstance(child21), sameInstance(child22)));

    assertThat(root.recursiveSpliterator().getExactSizeIfKnown(), is(7L));

    visitedComponents.clear();

    root.recursiveStream(TOP_DOWN).forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(root),
                                           sameInstance(child1), sameInstance(child11), sameInstance(child12),
                                           sameInstance(child2), sameInstance(child21), sameInstance(child22)));

    assertThat(root.recursiveSpliterator(TOP_DOWN).getExactSizeIfKnown(), is(7L));

    visitedComponents.clear();

    root.recursiveStream(BOTTOM_UP).forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(child11), sameInstance(child12), sameInstance(child1),
                                           sameInstance(child21), sameInstance(child22), sameInstance(child2),
                                           sameInstance(root)));

    assertThat(root.recursiveSpliterator(BOTTOM_UP).getExactSizeIfKnown(), is(7L));

  }

  @Test
  public void multipleLevels() {
    ComponentModel child1221 = baseComponentBuilder("1_2_2_1").build();
    ComponentModel child1222 = baseComponentBuilder("1_2_2_2").build();
    ComponentModel child1223 = baseComponentBuilder("1_2_2_3").build();

    ComponentModel child121 = baseComponentBuilder("1_2_1").build();

    Builder child122Builder = baseComponentBuilder("1_2_2");
    child122Builder.addChildComponentModel(child1221);
    child122Builder.addChildComponentModel(child1222);
    child122Builder.addChildComponentModel(child1223);
    ComponentModel child122 = child122Builder.build();

    ComponentModel child11 = baseComponentBuilder("1_1").build();

    Builder child12Builder = baseComponentBuilder("1_2");
    child12Builder.addChildComponentModel(child121);
    child12Builder.addChildComponentModel(child122);
    ComponentModel child12 = child12Builder.build();

    Builder child1Builder = baseComponentBuilder("1");
    child1Builder.addChildComponentModel(child11);
    child1Builder.addChildComponentModel(child12);
    ComponentModel child1 = child1Builder.build();

    ComponentModel child21 = baseComponentBuilder("2_1").build();

    Builder child2Builder = baseComponentBuilder("2");
    child2Builder.addChildComponentModel(child21);

    ComponentModel child2 = child2Builder.build();

    ComponentModel child3 = baseComponentBuilder("3").build();

    ComponentModel.Builder rootBuilder = baseComponentBuilder("root");
    rootBuilder.addChildComponentModel(child1);
    rootBuilder.addChildComponentModel(child2);
    rootBuilder.addChildComponentModel(child3);

    ComponentModel root = rootBuilder.build();

    final List<Object> visitedComponents = new ArrayList<>();

    root.recursiveStream().forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(root),
                                           sameInstance(child1), sameInstance(child11), sameInstance(child12),
                                           sameInstance(child121), sameInstance(child122),
                                           sameInstance(child1221), sameInstance(child1222), sameInstance(child1223),
                                           sameInstance(child2), sameInstance(child21), sameInstance(child3)));

    assertThat(root.recursiveSpliterator().getExactSizeIfKnown(), is(12L));

    visitedComponents.clear();

    root.recursiveStream(TOP_DOWN).forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(root),
                                           sameInstance(child1), sameInstance(child11), sameInstance(child12),
                                           sameInstance(child121), sameInstance(child122),
                                           sameInstance(child1221), sameInstance(child1222), sameInstance(child1223),
                                           sameInstance(child2), sameInstance(child21), sameInstance(child3)));

    assertThat(root.recursiveSpliterator().getExactSizeIfKnown(), is(12L));

    visitedComponents.clear();

    root.recursiveStream(BOTTOM_UP).forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(child11),
                                           sameInstance(child121),
                                           sameInstance(child1221), sameInstance(child1222), sameInstance(child1223),
                                           sameInstance(child122),
                                           sameInstance(child12),
                                           sameInstance(child1),
                                           sameInstance(child21), sameInstance(child2),
                                           sameInstance(child3),
                                           sameInstance(root)));

    assertThat(root.recursiveSpliterator().getExactSizeIfKnown(), is(12L));
  }

  @Test
  public void twoLevelsDirectOnly() {
    final ComponentModel.Builder rootBuilder = baseComponentBuilder("root");

    final Builder child1Builder = baseComponentBuilder("1");
    final ComponentModel child11 = baseComponentBuilder("11").build();
    child1Builder.addChildComponentModel(child11);
    final ComponentModel child12 = baseComponentBuilder("12").build();
    child1Builder.addChildComponentModel(child12);
    final ComponentModel child1 = child1Builder.build();
    rootBuilder.addChildComponentModel(child1);

    final Builder child2Builder = baseComponentBuilder("2");
    final ComponentModel child21 = baseComponentBuilder("21").build();
    child2Builder.addChildComponentModel(child21);
    final ComponentModel child22 = baseComponentBuilder("22").build();
    child2Builder.addChildComponentModel(child22);
    final ComponentModel child2 = child2Builder.build();
    rootBuilder.addChildComponentModel(child2);

    final ComponentModel root = rootBuilder.build();

    final List<Object> visitedComponents = new ArrayList<>();
    root.directChildrenStream().forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(child1), sameInstance(child2)));
    assertThat(visitedComponents, hasSize(2));
  }

  @Test
  public void twoLevelsDirectOnly2() {
    final ComponentModel.Builder rootBuilder = baseComponentBuilder("root");

    final Builder child1Builder = baseComponentBuilder("1");
    final ComponentModel child11 = baseComponentBuilder("11").build();
    child1Builder.addChildComponentModel(child11);
    final Builder child12Builder = baseComponentBuilder("12");
    child12Builder.addChildComponentModel(baseComponentBuilder("121").build());
    final ComponentModel child12 = child12Builder.build();
    child1Builder.addChildComponentModel(child12);
    final ComponentModel child1 = child1Builder.build();
    rootBuilder.addChildComponentModel(child1);

    final Builder child2Builder = baseComponentBuilder("2");
    final ComponentModel child21 = baseComponentBuilder("21").build();
    child2Builder.addChildComponentModel(child21);
    final ComponentModel child22 = baseComponentBuilder("22").build();
    child2Builder.addChildComponentModel(child22);
    final ComponentModel child2 = child2Builder.build();
    rootBuilder.addChildComponentModel(child2);

    final ComponentModel root = rootBuilder.build();

    final List<Object> visitedComponents = new ArrayList<>();
    root.directChildrenStream().forEach(visitedComponents::add);

    assertThat(visitedComponents, contains(sameInstance(child1), sameInstance(child2)));
    assertThat(visitedComponents, hasSize(2));
  }

  protected Builder baseComponentBuilder(String name) {
    return new ComponentModel.Builder()
        .setIdentifier(ComponentIdentifier.builder().namespace("lalala").name(name).build());
  }

  @Test
  public void fiveLevelsFiveChildrenEachDirectOnly() {
    final ComponentModel.Builder rootBuilder = baseComponentBuilder("root");

    addLevel(rootBuilder, 1);

    final ComponentModel root = rootBuilder.build();

    final List<Object> visitedComponents = new ArrayList<>();
    ((ComponentAst) root).directChildrenStream()
        .forEach(c -> {
          visitedComponents.add(c);
        });

    assertThat(visitedComponents, hasSize(5));
  }

  @Test
  public void fiveLevelsFiveChildrenEachParallel() {
    final ComponentModel.Builder rootBuilder = baseComponentBuilder("root");

    addLevel(rootBuilder, 1);

    final ComponentModel root = rootBuilder.build();

    final List<Object> visitedComponents = new ArrayList<>();
    final Set<Thread> visitingThreads = new HashSet<>();
    ((ComponentAst) root).recursiveStream().parallel().forEach(c -> {
      visitedComponents.add(c);
      visitingThreads.add(currentThread());
    });

    assertThat(visitedComponents, hasSize(3906));
  }

  protected void addLevel(final ComponentModel.Builder rootBuilder, int level) {
    for (int i = 0; i < 5; i++) {
      final Builder childBuilder = baseComponentBuilder("" + level + "." + i);

      if (level < 5) {
        addLevel(childBuilder, level + 1);
      }

      rootBuilder.addChildComponentModel(childBuilder.build());
    }
  }

}
