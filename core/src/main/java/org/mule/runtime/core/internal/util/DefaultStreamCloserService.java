/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Closeable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

/**
 * Closes streams of different types by looking up available {@link StreamCloser}'s from the Mule registry.
 * {@link StreamCloser} instances are only fetched from the registry the first time the
 * {@link #closeStream(Object)} method is called with a steam that cannot be closed by {@lnk CoreStreamTypesCloser}. Any other
 * closers added to the registry after that will be ignored
 */
public class DefaultStreamCloserService implements StreamCloserService {

  private static final Logger log = LoggerFactory.getLogger(DefaultStreamCloserService.class);

  private MuleContext muleContext;
  private StreamCloser coreStreamTypesCloser = new CoreStreamTypesCloser();
  private Collection<StreamCloser> allStreamClosers = null;

  @Override
  public void closeStream(Object stream) {
    try {
      if (coreStreamTypesCloser.canClose(stream.getClass())) {
        coreStreamTypesCloser.close(stream);
      } else {
        for (StreamCloser closer : getAllStreamClosers()) {
          if (closer.canClose(stream.getClass())) {
            closer.close(stream);
            return;
          }
        }

        if (log.isDebugEnabled()) {
          log.debug(String.format("Unable to find a StreamCloser for the stream type: %s " + ", the stream will not be closed.",
                                  stream.getClass()));
        }
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug(String.format("Exception closing stream of class %s", stream.getClass()), e);
      }
    }

  }

  /**
   * Lazyly fetches and keeps all the registered {@link StreamCloser} instances from the registry.
   * Because there're not too many of them, this is the most efficient option to avoid accessing the registry continuosly. If we
   * get to a situation in which we have many of them, considering using a {@link java.util.Map} guarded by a
   * {@link java.util.concurrent.locks.ReadWriteLock}
   *
   * @return all {@link StreamCloser} instances in the registry
   * @throws Exception
   */
  private Collection<StreamCloser> getAllStreamClosers() throws Exception {
    if (allStreamClosers == null) {
      allStreamClosers = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObjects(StreamCloser.class);
    }

    return allStreamClosers;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
  }

  static class CoreStreamTypesCloser implements StreamCloser {

    @Override
    public boolean canClose(Class streamType) {
      return InputStream.class.isAssignableFrom(streamType) || InputSource.class.isAssignableFrom(streamType)
          || StreamSource.class.isAssignableFrom(streamType) || Closeable.class.isAssignableFrom(streamType)
          || java.io.Closeable.class.isAssignableFrom(streamType)
          || (SAXSource.class.isAssignableFrom(streamType) && !streamType.getName().endsWith("StaxSource"));
    }

    @Override
    public void close(Object stream) throws IOException {
      if (stream instanceof InputStream) {
        try {
          ((InputStream) stream).close();
        } catch (IOException e) {
          this.logCloseException(stream, e);
        }
      } else if (stream instanceof InputSource) {
        closeInputSourceStream((InputSource) stream);
      } else if (stream instanceof SAXSource) {
        closeInputSourceStream(((SAXSource) stream).getInputSource());
      } else if (stream instanceof StreamSource) {
        try {
          ((StreamSource) stream).getInputStream().close();
        } catch (IOException e) {
          this.logCloseException(stream, e);
        }
      } else if (stream instanceof Closeable) {
        try {
          ((Closeable) stream).close();
        } catch (MuleException e) {
          this.logCloseException(stream, e);
        }
      } else if (stream instanceof java.io.Closeable) {
        try {
          ((java.io.Closeable) stream).close();
        } catch (Exception e) {
          logCloseException(stream, e);
        }
      }
    }

    private void closeInputSourceStream(InputSource payload) throws IOException {
      if (payload.getByteStream() != null) {
        payload.getByteStream().close();
      } else if (payload.getCharacterStream() != null) {
        payload.getCharacterStream().close();
      }
    }

    private void logCloseException(Object stream, Throwable e) {
      if (log.isWarnEnabled()) {
        log.warn("Exception was found trying to close resource of class " + stream.getClass().getCanonicalName(), e);
      }
    }

  }

}
