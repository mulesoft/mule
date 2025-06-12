/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreate;
import static org.mule.runtime.core.api.util.FileUtils.cleanDirectory;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.runtime.core.internal.util.store.MuleObjectStoreManager.UNBOUNDED;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.unmodifiableList;

import static org.apache.commons.io.FileUtils.readFileToString;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ExpirableObjectStore;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreNotAvailableException;
import org.mule.runtime.api.store.TemplateObjectStore;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.util.UUID;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentObjectStorePartition<T extends Serializable> extends TemplateObjectStore<T>
    implements ExpirableObjectStore<T> {

  private static final String OBJECT_FILE_EXTENSION = ".obj";
  private static final String PARTITION_DESCRIPTOR_FILE = "partition-descriptor";
  public static final String CORRUPTED_FOLDER = "corrupted-files";

  private static final Logger LOGGER = LoggerFactory.getLogger(PersistentObjectStorePartition.class);

  private final MuleConfiguration muleConfiguration;
  private final ObjectSerializer serializer;

  private boolean loaded = false;

  private File partitionDirectory;
  private String partitionName;
  private final BidiMap realKeyToUUIDIndex = new TreeBidiMap();

  // The purpose of this lock is to ensure consistency between the realKeyToUUIDIndex above and the file system, not the
  // consistency of the store itself.
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private final Lock rLock = rwLock.readLock();
  private final Lock wLock = rwLock.writeLock();

  public PersistentObjectStorePartition(MuleConfiguration muleConfiguration, ObjectSerializer serializer, String partitionName,
                                        File partitionDirectory) {
    this.muleConfiguration = muleConfiguration;
    this.serializer = serializer;
    this.partitionName = partitionName;
    this.partitionDirectory = partitionDirectory;
  }

  public PersistentObjectStorePartition(MuleConfiguration muleConfiguration, ObjectSerializer serializer, File partitionDirectory)
      throws ObjectStoreNotAvailableException {
    this.muleConfiguration = muleConfiguration;
    this.serializer = serializer;
    this.partitionDirectory = partitionDirectory;
    this.partitionName = readPartitionFileName(partitionDirectory);
  }

  protected PersistentObjectStorePartition() {
    muleConfiguration = null;
    serializer = null;
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
  public void close() throws ObjectStoreException {
    wLock.lock();
    try {
      try {
        cleanDirectory(this.partitionDirectory);
        partitionDirectory.delete();
      } catch (IOException e) {
        throw new ObjectStoreException(createStaticMessage("Could not close object store partition"), e);
      }

      realKeyToUUIDIndex.clear();
    } finally {
      wLock.unlock();
    }
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    assureLoaded();

    rLock.lock();
    try {
      return unmodifiableList(new ArrayList<String>(realKeyToUUIDIndex.keySet()));
    } finally {
      rLock.unlock();
    }
  }

  @Override
  protected boolean doContains(String key) throws ObjectStoreException {
    assureLoaded();

    rLock.lock();
    try {
      return realKeyToUUIDIndex.containsKey(key);
    } finally {
      rLock.unlock();
    }
  }

  @Override
  protected void doStore(String key, T value) throws ObjectStoreException {
    assureLoaded();

    wLock.lock();
    try {
      if (realKeyToUUIDIndex.containsKey(key)) {
        throw new ObjectAlreadyExistsException();
      }
      File newFile = createFileToStoreObject();
      realKeyToUUIDIndex.put(key, newFile.getName());
      serialize(newFile, new StoreValue<>(key, value));
    } finally {
      wLock.unlock();
    }
  }

  @Override
  public void clear() throws ObjectStoreException {
    wLock.lock();
    try {
      try {
        cleanDirectory(this.partitionDirectory);
        createOrRetrievePartitionDescriptorFile();
      } catch (IOException e) {
        throw new ObjectStoreException(createStaticMessage("Could not clear ObjectStore"), e);
      }

      realKeyToUUIDIndex.clear();
    } finally {
      wLock.unlock();
    }
  }

  @Override
  protected T doRetrieve(String key) throws ObjectStoreException {
    assureLoaded();

    rLock.lock();
    try {
      if (!realKeyToUUIDIndex.containsKey(key)) {
        throw new ObjectDoesNotExistException(createStaticMessage("Key does not exist: " + key));
      }
      return load(key);
    } finally {
      rLock.unlock();
    }
  }

  @Override
  public Map<String, T> retrieveAll() throws ObjectStoreException {
    assureLoaded();

    rLock.lock();
    try {
      Map<String, T> values = new LinkedHashMap<>(realKeyToUUIDIndex.size());
      for (Object k : realKeyToUUIDIndex.keySet()) {
        String key = (String) k;
        values.put(key, load(key));
      }

      return values;
    } finally {
      rLock.unlock();
    }
  }

  private T load(String key) throws ObjectStoreException {
    String filename = (String) realKeyToUUIDIndex.get(key);
    File file = getValueFile(filename);
    return deserialize(file).getValue();
  }

  @Override
  protected T doRemove(String key) throws ObjectStoreException {
    assureLoaded();

    wLock.lock();
    try {
      T value = retrieve(key);
      deleteStoreFile(getValueFile((String) realKeyToUUIDIndex.get(key)));
      return value;
    } finally {
      wLock.unlock();
    }
  }

  @Override
  public boolean isPersistent() {
    return true;
  }

  @Override
  public void expire(long entryTTL, int maxEntries) throws ObjectStoreException {
    assureLoaded();

    wLock.lock();
    try {
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
    } finally {
      wLock.unlock();
    }
  }

  private void assureLoaded() throws ObjectStoreException {
    if (!loaded) {
      loadStoredKeysAndFileNames();
    }
  }

  private void createInnerDirectories(File file) {
    File parentFile = file.getParentFile();
    if (parentFile.exists()) {
      return;
    }
    createInnerDirectories(parentFile);
    parentFile.mkdir();
  }

  private void moveToCorruptedFilesFolder(File file) throws IOException {
    Path workingDirectory = (new File(muleConfiguration.getWorkingDirectory()))
        .toPath().normalize();
    Path absoluteFilePath = file.toPath();
    Path relativePath = workingDirectory.relativize(absoluteFilePath);
    File corruptedDir = new File(muleConfiguration.getWorkingDirectory() + File.separator + CORRUPTED_FOLDER);
    if (!corruptedDir.exists()) {
      corruptedDir.mkdir();
    }
    File corruptedFile = new File(corruptedDir.getAbsolutePath() + File.separator + relativePath.toString());
    createInnerDirectories(corruptedFile);
    Files.move(file.toPath(), corruptedFile.toPath(), REPLACE_EXISTING);
  }

  private void loadStoredKeysAndFileNames() throws ObjectStoreException {
    wLock.lock();
    try {
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
            LOGGER.warn("Could not deserialize the ObjectStore file: {}. "
                + "The file will be skipped and moved to the Garbage folder",
                        file.getName());
            moveToCorruptedFilesFolder(file);
          }
        }

        loaded = true;
      } catch (Exception e) {
        throw new ObjectStoreException(createStaticMessage(format("Could not restore object store data from %1s",
                                                                  partitionDirectory.getAbsolutePath())));
      }
    } finally {
      wLock.unlock();
    }
  }

  public File getPartitionDirectory() {
    return partitionDirectory;
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
    String filename = UUID.getUUID() + OBJECT_FILE_EXTENSION;
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
      try (FileWriter fileWriter = new FileWriter(partitionDescriptorFile.getAbsolutePath(), false)) {
        fileWriter.write(partitionName);
        fileWriter.flush();
      }
      return partitionDescriptorFile;
    } catch (Exception e) {
      throw new ObjectStoreException(e);
    }
  }

  protected void serialize(File outputFile, StoreValue<T> storeValue) throws ObjectStoreException {
    try (
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
      serializer.getInternalProtocol().serialize(storeValue, objectOutputStream);
      objectOutputStream.flush();
    } catch (Exception se) {
      throw new ObjectStoreException(se);
    }
  }

  @SuppressWarnings("unchecked")
  protected StoreValue<T> deserialize(File file) throws ObjectStoreException {
    try (
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {
      StoreValue<T> storedValue = serializer.getInternalProtocol().deserialize(objectInputStream);
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

    private final Serializable key;
    private final T value;

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
