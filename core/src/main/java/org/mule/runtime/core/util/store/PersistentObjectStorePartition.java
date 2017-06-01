/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.util.store;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.io.FileUtils.moveFileToDirectory;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.store.ObjectStoreManager.UNBOUNDED;
import static org.mule.runtime.core.config.i18n.CoreMessages.failedToCreate;
import static org.mule.runtime.core.api.util.FileUtils.cleanDirectory;
import static org.mule.runtime.core.api.util.FileUtils.newFile;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreNotAvailableException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.store.ExpirableObjectStore;
import org.mule.runtime.core.api.store.ListableObjectStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentObjectStorePartition<T extends Serializable> implements ListableObjectStore<T>, ExpirableObjectStore<T> {

  private static final String OBJECT_FILE_EXTENSION = ".obj";
  private static final String PARTITION_DESCRIPTOR_FILE = "partition-descriptor";
  public static final String CORRUPTED_FOLDER = "corrupted-files";

  private static final Logger logger = LoggerFactory.getLogger(PersistentObjectStorePartition.class);

  private final MuleContext muleContext;
  private final ObjectSerializer serializer;

  private boolean loaded = false;


  private File partitionDirectory;
  private String partitionName;
  private final BidiMap realKeyToUUIDIndex = new TreeBidiMap();

  PersistentObjectStorePartition(MuleContext muleContext, String partitionName, File partitionDirectory) {
    this.muleContext = muleContext;
    serializer = muleContext.getObjectSerializer();
    this.partitionName = partitionName;
    this.partitionDirectory = partitionDirectory;
  }

  PersistentObjectStorePartition(MuleContext muleContext, File partitionDirectory) throws ObjectStoreNotAvailableException {
    this.muleContext = muleContext;
    serializer = muleContext.getObjectSerializer();
    this.partitionDirectory = partitionDirectory;
    this.partitionName = readPartitionFileName(partitionDirectory);
  }

  private String readPartitionFileName(File partitionDirectory) throws ObjectStoreNotAvailableException {
    File partitionDescriptorFile = new File(partitionDirectory, PARTITION_DESCRIPTOR_FILE);
    try {
      return readFileToString(partitionDescriptorFile);
    } catch (IOException e) {
      throw new ObjectStoreNotAvailableException(e);
    }
  }

  @Override
  public synchronized void open() throws ObjectStoreException {
    createDirectory(partitionDirectory);
    createOrRetrievePartitionDescriptorFile();
  }

  @Override
  public void close() throws ObjectStoreException {}

  @Override
  public List<Serializable> allKeys() throws ObjectStoreException {
    assureLoaded();

    synchronized (realKeyToUUIDIndex) {
      return unmodifiableList(new ArrayList<Serializable>(realKeyToUUIDIndex.keySet()));
    }
  }

  @Override
  public boolean contains(Serializable key) throws ObjectStoreException {
    assureLoaded();

    synchronized (realKeyToUUIDIndex) {
      return realKeyToUUIDIndex.containsKey(key);
    }
  }

  @Override
  public void store(Serializable key, T value) throws ObjectStoreException {
    assureLoaded();

    synchronized (realKeyToUUIDIndex) {
      if (realKeyToUUIDIndex.containsKey(key)) {
        throw new ObjectAlreadyExistsException();
      }
      File newFile = createFileToStoreObject();
      realKeyToUUIDIndex.put(key, newFile.getName());
      serialize(newFile, new StoreValue<T>(key, value));
    }
  }

  @Override
  public void clear() throws ObjectStoreException {
    synchronized (realKeyToUUIDIndex) {
      try {
        cleanDirectory(this.partitionDirectory);
      } catch (IOException e) {
        throw new ObjectStoreException(createStaticMessage("Could not clear ObjectStore"), e);
      }

      realKeyToUUIDIndex.clear();
    }
  }

  @Override
  public T retrieve(Serializable key) throws ObjectStoreException {
    assureLoaded();

    synchronized (realKeyToUUIDIndex) {
      if (!realKeyToUUIDIndex.containsKey(key)) {
        throw new ObjectDoesNotExistException(createStaticMessage("Key does not exist: " + key));
      }
      String filename = (String) realKeyToUUIDIndex.get(key);
      File file = getValueFile(filename);
      return deserialize(file).getValue();
    }
  }

  @Override
  public T remove(Serializable key) throws ObjectStoreException {
    assureLoaded();

    synchronized (realKeyToUUIDIndex) {
      T value = retrieve(key);
      deleteStoreFile(getValueFile((String) realKeyToUUIDIndex.get(key)));
      return value;
    }
  }

  @Override
  public boolean isPersistent() {
    return true;
  }

  @Override
  public void expire(long entryTTL, int maxEntries) throws ObjectStoreException {
    assureLoaded();

    synchronized (realKeyToUUIDIndex) {
      File[] files = listValuesFiles();
      Arrays.sort(files, (f1, f2) -> {
        int result = Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        if (result == 0) {
          result = f1.getName().compareTo(f2.getName());
        }
        return result;
      });
      int startIndex = trimToMaxSize(files, maxEntries);

      if (entryTTL == UNBOUNDED) {
        return;
      }

      final long now = System.currentTimeMillis();
      for (int i = startIndex; i < files.length; i++) {
        Long lastModified = files[i].lastModified();
        if ((now - lastModified) >= entryTTL) {
          deleteStoreFile(files[i]);
        } else {
          break;
        }
      }
    }
  }

  private void assureLoaded() throws ObjectStoreException {
    if (!loaded) {
      loadStoredKeysAndFileNames();
    }
  }

  private void moveToCorruptedFilesFolder(File file) throws IOException {
    String workingDirectory = (new File(muleContext.getConfiguration().getWorkingDirectory()))
        .toPath().normalize().toString();

    String diffFolder = file.getAbsolutePath().split(workingDirectory)[1];
    File corruptedFile = new File(muleContext.getConfiguration().getWorkingDirectory()
        + File.separator + CORRUPTED_FOLDER + diffFolder);
    moveFileToDirectory(file, corruptedFile.getParentFile(), true);
  }

  private void loadStoredKeysAndFileNames() throws ObjectStoreException {
    synchronized (realKeyToUUIDIndex) {
      /*
       * by re-checking this condition here we can avoid contention in {@link #assureLoaded}. The amount of times that this
       * condition should evaluate to {@code true} is really limited, which provides better performance in the long run
       */
      if (loaded) {
        return;
      }

      try {
        File[] files = listValuesFiles();
        for (File file : files) {
          try {
            StoreValue<T> storeValue = deserialize(file);
            realKeyToUUIDIndex.put(storeValue.getKey(), file.getName());
          } catch (ObjectStoreException e) {
            if (logger.isWarnEnabled()) {
              logger
                  .warn(format("Could not deserialize the ObjectStore file: %s. The file will be skipped and moved to the Garbage folder",
                               file.getName()));
            }
            moveToCorruptedFilesFolder(file);
          }
        }

        loaded = true;
      } catch (Exception e) {
        throw new ObjectStoreException(createStaticMessage(format("Could not restore object store data from %1s",
                                                                  partitionDirectory.getAbsolutePath())));
      }
    }
  }

  private File[] listValuesFiles() {
    File[] files =
        partitionDirectory.listFiles((FileFilter) file -> !file.isDirectory() && file.getName().endsWith(OBJECT_FILE_EXTENSION));
    if (files == null) {
      files = new File[0];
    }
    return files;
  }

  protected void createDirectory(File directory) throws ObjectStoreException {
    try {
      // To support concurrency we need to check if directory exists again
      // inside
      // synchronized method
      if (!directory.exists() && !directory.mkdirs()) {
        throw new MuleRuntimeException(failedToCreate("object store directory " + directory.getAbsolutePath()));
      }
    } catch (Exception e) {
      throw new ObjectStoreException(e);
    }
  }

  private File getValueFile(String filename) {
    return new File(partitionDirectory, filename);
  }

  protected File createFileToStoreObject() throws ObjectStoreException {
    String filename = org.mule.runtime.core.util.UUID.getUUID() + OBJECT_FILE_EXTENSION;
    try {
      return newFile(partitionDirectory, filename);
    } catch (MuleRuntimeException mre) {
      throw new ObjectStoreException(mre);
    }
  }

  protected File createOrRetrievePartitionDescriptorFile() throws ObjectStoreException {
    try {
      File partitionDescriptorFile = new File(partitionDirectory, PARTITION_DESCRIPTOR_FILE);
      if (partitionDescriptorFile.exists()) {
        this.partitionName = readPartitionFileName(partitionDirectory);
        return partitionDescriptorFile;
      }
      FileWriter fileWriter = new FileWriter(partitionDescriptorFile.getAbsolutePath(), false);
      try {
        fileWriter.write(partitionName);
        fileWriter.flush();
      } finally {
        fileWriter.close();
      }
      return partitionDescriptorFile;
    } catch (Exception e) {
      throw new ObjectStoreException(e);
    }
  }

  protected void serialize(File outputFile, StoreValue<T> storeValue) throws ObjectStoreException {
    try (ObjectOutputStream objectOutputStream =
        new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
      serializer.getInternalProtocol().serialize(storeValue, objectOutputStream);
      objectOutputStream.flush();
    } catch (Exception se) {
      throw new ObjectStoreException(se);
    }
  }

  @SuppressWarnings("unchecked")
  protected StoreValue<T> deserialize(File file) throws ObjectStoreException {
    try (ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      StoreValue<T> storedValue = serializer.getInternalProtocol().deserialize(objectInputStream);
      if (storedValue.getValue() instanceof DeserializationPostInitialisable) {
        DeserializationPostInitialisable.Implementation.init(storedValue.getValue(), muleContext);
      }
      return storedValue;
    } catch (Exception e) {
      throw new ObjectStoreException(e);
    }
  }

  protected void deleteStoreFile(File file) throws ObjectStoreException {
    if (file.exists()) {
      if (!file.delete()) {
        throw new ObjectStoreException(createStaticMessage("Deleting " + file.getAbsolutePath() + " failed"));
      }
      realKeyToUUIDIndex.removeValue(file.getName());
    } else {
      throw new ObjectDoesNotExistException();
    }
  }

  private int trimToMaxSize(File[] files, int maxEntries) throws ObjectStoreException {
    if (maxEntries == UNBOUNDED) {
      return 0;
    }
    int expired = 0;
    int excess = (files.length - maxEntries);
    if (excess > 0) {
      for (int i = 0; i < excess; i++) {
        deleteStoreFile(files[i]);
        expired++;
      }
    }
    return expired;
  }

  public String getPartitionName() {
    return partitionName;
  }

  public static class StoreValue<T> implements Serializable {

    private Serializable key;
    private T value;

    public StoreValue(Serializable key, T value) {
      this.key = key;
      this.value = value;
    }

    public Serializable getKey() {
      return key;
    }

    public T getValue() {
      return value;
    }
  }
}
