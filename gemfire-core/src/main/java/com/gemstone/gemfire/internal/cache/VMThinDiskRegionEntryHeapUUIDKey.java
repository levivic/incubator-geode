/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache;
// DO NOT modify this class. It was generated from LeafRegionEntry.cpp
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import com.gemstone.gemfire.internal.cache.lru.EnableLRU;
import com.gemstone.gemfire.internal.cache.persistence.DiskRecoveryStore;
import com.gemstone.gemfire.internal.util.concurrent.CustomEntryConcurrentHashMap.HashEntry;
// macros whose definition changes this class:
// disk: DISK
// lru: LRU
// stats: STATS
// versioned: VERSIONED
// offheap: OFFHEAP
// One of the following key macros must be defined:
// key object: KEY_OBJECT
// key int: KEY_INT
// key long: KEY_LONG
// key uuid: KEY_UUID
// key string1: KEY_STRING1
// key string2: KEY_STRING2
/**
 * Do not modify this class. It was generated.
 * Instead modify LeafRegionEntry.cpp and then run
 * bin/generateRegionEntryClasses.sh from the directory
 * that contains your build.xml.
 */
public class VMThinDiskRegionEntryHeapUUIDKey extends VMThinDiskRegionEntryHeap {
  public VMThinDiskRegionEntryHeapUUIDKey (RegionEntryContext context, UUID key,
      Object value
      ) {
    super(context,
          (value instanceof RecoveredEntry ? null : value)
        );
    // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
    initialize(context, value);
    this.keyMostSigBits = key.getMostSignificantBits();
    this.keyLeastSigBits = key.getLeastSignificantBits();
  }
  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  // common code
  protected int hash;
  private HashEntry<Object, Object> next;
  @SuppressWarnings("unused")
  private volatile long lastModified;
  private static final AtomicLongFieldUpdater<VMThinDiskRegionEntryHeapUUIDKey> lastModifiedUpdater
    = AtomicLongFieldUpdater.newUpdater(VMThinDiskRegionEntryHeapUUIDKey.class, "lastModified");
  private volatile Object value;
  @Override
  protected final Object getValueField() {
    return this.value;
  }
  @Override
  protected void setValueField(Object v) {
    this.value = v;
  }
  protected long getlastModifiedField() {
    return lastModifiedUpdater.get(this);
  }
  protected boolean compareAndSetLastModifiedField(long expectedValue, long newValue) {
    return lastModifiedUpdater.compareAndSet(this, expectedValue, newValue);
  }
  /**
   * @see HashEntry#getEntryHash()
   */
  public final int getEntryHash() {
    return this.hash;
  }
  protected void setEntryHash(int v) {
    this.hash = v;
  }
  /**
   * @see HashEntry#getNextEntry()
   */
  public final HashEntry<Object, Object> getNextEntry() {
    return this.next;
  }
  /**
   * @see HashEntry#setNextEntry
   */
  public final void setNextEntry(final HashEntry<Object, Object> n) {
    this.next = n;
  }
  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  // disk code
  protected void initialize(RegionEntryContext context, Object value) {
    diskInitialize(context, value);
  }
  @Override
  public int updateAsyncEntrySize(EnableLRU capacityController) {
    throw new IllegalStateException("should never be called");
  }
  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  private void diskInitialize(RegionEntryContext context, Object value) {
    DiskRecoveryStore drs = (DiskRecoveryStore)context;
    DiskStoreImpl ds = drs.getDiskStore();
    long maxOplogSize = ds.getMaxOplogSize();
    //get appropriate instance of DiskId implementation based on maxOplogSize
    this.id = DiskId.createDiskId(maxOplogSize, true/* is persistence */, ds.needsLinkedList());
    Helper.initialize(this, drs, value);
  }
  /**
   * DiskId
   * 
   * @since 5.1
   */
  protected DiskId id;//= new DiskId();
  public DiskId getDiskId() {
    return this.id;
  }
  @Override
  void setDiskId(RegionEntry old) {
    this.id = ((AbstractDiskRegionEntry)old).getDiskId();
  }
//  // inlining DiskId
//  // always have these fields
//  /**
//   * id consists of
//   * most significant
//   * 1 byte = users bits
//   * 2-8 bytes = oplog id
//   * least significant.
//   * 
//   * The highest bit in the oplog id part is set to 1 if the oplog id
//   * is negative.
//   * @todo this field could be an int for an overflow only region
//   */
//  private long id;
//  /**
//   * Length of the bytes on disk.
//   * This is always set. If the value is invalid then it will be set to 0.
//   * The most significant bit is used by overflow to mark it as needing to be written.
//   */
//  protected int valueLength = 0;
//  // have intOffset or longOffset
//  // intOffset
//  /**
//   * The position in the oplog (the oplog offset) where this entry's value is
//   * stored
//   */
//  private volatile int offsetInOplog;
//  // longOffset
//  /**
//   * The position in the oplog (the oplog offset) where this entry's value is
//   * stored
//   */
//  private volatile long offsetInOplog;
//  // have overflowOnly or persistence
//  // overflowOnly
//  // no fields
//  // persistent
//  /** unique entry identifier * */
//  private long keyId;
  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  // key code
  private final long keyMostSigBits;
  private final long keyLeastSigBits;
  @Override
  public final Object getKey() {
    return new UUID(this.keyMostSigBits, this.keyLeastSigBits);
  }
  @Override
  public boolean isKeyEqual(Object k) {
    if (k instanceof UUID) {
      UUID uuid = (UUID)k;
      return uuid.getLeastSignificantBits() == this.keyLeastSigBits && uuid.getMostSignificantBits() == this.keyMostSigBits;
    }
    return false;
  }
  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
}
