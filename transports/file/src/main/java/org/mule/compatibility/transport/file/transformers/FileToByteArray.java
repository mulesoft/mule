/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file.transformers;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

/**
 * <code>FileToByteArray</code> reads the contents of a file as a byte array.
 */
public class FileToByteArray extends AbstractTransformer implements DiscoverableTransformer {

  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

  public FileToByteArray() {
    super();
    registerSourceType(DataType.fromType(File.class));
    registerSourceType(DataType.fromType(FileInputStream.class));
    setReturnDataType(DataType.BYTE_ARRAY);
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    File file = null;
    FileInputStream fileInputStream = null;

    if (src instanceof FileInputStream) {
      fileInputStream = (FileInputStream) src;
    } else if (src instanceof File) {
      file = (File) src;

      if (file == null) {
        throw new TransformerException(this, new IllegalArgumentException("null file"));
      }

      if (!file.exists()) {
        throw new TransformerException(this, new FileNotFoundException(file.getPath()));
      }

      if (file.length() == 0) {
        logger.warn("File is empty: " + file.getAbsolutePath());
        return ArrayUtils.EMPTY_BYTE_ARRAY;
      }

      try {
        fileInputStream = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new TransformerException(this, e);
      }

    } else {
      throw new TransformerException(MessageFactory.createStaticMessage("Cannot handle source type %s", src.getClass().getName()),
                                     this);
    }

    try {
      return IOUtils.toByteArray(fileInputStream);
    }
    // at least try..
    catch (OutOfMemoryError oom) {
      throw new TransformerException(this, oom);
    } catch (IOException e) {
      throw new TransformerException(this, e);
    } finally {
      IOUtils.closeQuietly(fileInputStream);
    }
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int priorityWeighting) {
    this.priorityWeighting = priorityWeighting;
  }

}
