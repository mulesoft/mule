/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.transformer.ResolverException;
import org.mule.runtime.core.internal.transformer.graph.GraphTransformerResolver;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.internal.transformer.simple.ObjectToString;
import org.mule.runtime.core.privileged.transformer.TransformerChain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Will discover transformers based on type information only. It looks for transformers that support the source and result types
 * passed into the method. This resolver only resolves on the first source type, which is the way transformer resolution working
 * in Mule 2.x.
 */
public class TypeBasedTransformerResolver implements TransformerResolver, MuleContextAware, Disposable, Initialisable {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(TypeBasedTransformerResolver.class);

  private ObjectToString objectToString;
  private ObjectToByteArray objectToByteArray;

  private MuleContext muleContext;

  protected Map<String, Transformer> exactTransformerCache = new ConcurrentHashMap/* <String, Transformer> */(8);

  protected TransformerResolver graphTransformerResolver = new GraphTransformerResolver();

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      objectToString = new ObjectToString();
      objectToByteArray = new ObjectToByteArray();

      // these are just fallbacks that are not to go
      // into the mule registry
      initialiseIfNeeded(objectToString, muleContext);
      initialiseIfNeeded(objectToByteArray, muleContext);
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }

  public Transformer resolve(DataType source, DataType result) throws ResolverException {
    Transformer transformer = exactTransformerCache.get(source.toString() + result.toString());
    if (transformer != null) {
      return transformer;
    }

    MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();
    List<Transformer> trans = registry.lookupTransformers(source, result);

    Transformer compositeTransformer = graphTransformerResolver.resolve(source, result);
    if (compositeTransformer != null) {
      // Needs to create a new list because the lookup returns a cached instance
      trans = new LinkedList<>(trans);
      trans.add(compositeTransformer);
    }

    transformer = getNearestTransformerMatch(trans, source.getType(), result.getType());
    // If an exact mach is not found, we have a 'second pass' transformer that can be used to converting to String or
    // byte[]
    Transformer secondPass;
    if (transformer == null) {
      // If no transformers were found but the outputType type is String or byte[] we can perform a more general search
      // using Object.class and then convert to String or byte[] using the second pass transformer
      if (String.class.equals(result.getType())) {
        secondPass = objectToString;
      } else if (byte[].class.equals(result.getType())) {
        secondPass = objectToByteArray;
      } else {
        return null;
      }
      // Perform a more general search
      trans = registry.lookupTransformers(source, DataType.OBJECT);

      transformer = getNearestTransformerMatch(trans, source.getType(), result.getType());
      if (transformer != null) {
        transformer = new TransformerChain(transformer, secondPass);
        try {
          registry.registerTransformer(transformer);
        } catch (MuleException e) {
          throw new ResolverException(e.getI18nMessage(), e);
        }
      }
    }

    if (transformer != null) {
      exactTransformerCache.put(source.toString() + result.toString(), transformer);
    }
    return transformer;
  }

  protected Transformer getNearestTransformerMatch(List<Transformer> trans, Class input, Class output) throws ResolverException {
    if (trans.size() > 1) {
      if (logger.isDebugEnabled()) {
        logger.debug("Comparing transformers for best match: source = " + input + " target = " + output
            + " Possible transformers = " + trans);
      }

      List<TransformerWeighting> weightings = calculateTransformerWeightings(trans, input, output);

      TransformerWeighting maxWeighting = weightings.get(weightings.size() - 1);

      for (int index = weightings.size() - 2; index >= 0 && maxWeighting.compareTo(weightings.get(index)) == 0; index--) {
        // We may have two transformers that are exactly the same, in which case we can use either i.e. use the current
        TransformerWeighting current = weightings.get(index);
        if (!maxWeighting.getTransformer().getClass().equals(current.getTransformer().getClass())) {
          List<Transformer> transformers = Arrays.asList(current.getTransformer(), maxWeighting.getTransformer());
          throw new ResolverException(CoreMessages.transformHasMultipleMatches(input, output, transformers));
        }
      }

      return maxWeighting.getTransformer();
    } else if (trans.size() == 0) {
      return null;
    } else {
      return trans.get(0);
    }
  }

  private List<TransformerWeighting> calculateTransformerWeightings(List<Transformer> transformers, Class input, Class output) {
    List<TransformerWeighting> weightings = new ArrayList<>(transformers.size());

    for (Transformer transformer : transformers) {
      TransformerWeighting transformerWeighting = new TransformerWeighting(input, output, transformer);
      weightings.add(transformerWeighting);
    }

    Collections.sort(weightings);

    return weightings;
  }

  @Override
  public void dispose() {
    exactTransformerCache.clear();
  }

  @Override
  public void transformerChange(Transformer transformer, RegistryAction registryAction) {
    if (transformer instanceof Converter) {
      graphTransformerResolver.transformerChange(transformer, registryAction);
      exactTransformerCache.clear();
    }
  }
}
