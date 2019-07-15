/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.ComponentModel.Builder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;

import org.junit.Test;

public class ComponentAstSpliteratorTestCase extends AbstractMuleTestCase {

  @Test
  public void singleLevel() {
    final ComponentModel.Builder rootBuilder = baseComponentBuilder("root");

    final ComponentModel child1 = baseComponentBuilder("1").build();
    rootBuilder.addChildComponentModel(child1);
    final ComponentModel child2 = baseComponentBuilder("2").build();
    rootBuilder.addChildComponentModel(child2);

    final ComponentModel root = rootBuilder.build();

    final List<Object> visitedComponents = new ArrayList<>();
    ((ComponentAst) root).recursiveStream().forEach(c -> {
      visitedComponents.add(c);
    });

    assertThat(visitedComponents, contains(sameInstance(root), sameInstance(child1), sameInstance(child2)));
  }

  @Test
  public void twoLevels() {
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
    ((ComponentAst) root).recursiveStream().forEach(c -> {
      visitedComponents.add(c);
    });

    assertThat(visitedComponents, contains(sameInstance(root),
                                           sameInstance(child1), sameInstance(child11), sameInstance(child12),
                                           sameInstance(child2), sameInstance(child21), sameInstance(child22)));

    final Spliterator<ComponentAst> rootSpliterator = ((ComponentAst) root).recursiveSpliterator();
    assertThat(rootSpliterator.getExactSizeIfKnown(), is(7L));
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
    ((ComponentAst) root).directChildrenStream()
        .forEach(c -> {
          visitedComponents.add(c);
        });

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
    ((ComponentAst) root).directChildrenStream()
        .forEach(c -> {
          visitedComponents.add(c);
        });

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
