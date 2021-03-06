/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache30;

import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.internal.logging.LogWriterImpl;
import com.gemstone.gemfire.internal.logging.LoggingThreadGroup;

/**
 * Tests populating a region with data that is ever-increasing in
 * size.  It is used for testing the "Heap LRU" feature that helps
 * prevent out of memory errors.
 */
public class TestHeapLRU {

  public static void main(String[] args) throws Exception {
    DistributedSystem system =
      DistributedSystem.connect(new java.util.Properties());
    Cache cache = CacheFactory.create(system);
    AttributesFactory factory = new AttributesFactory();

    factory.setEvictionAttributes(EvictionAttributes.createLRUHeapAttributes(null, EvictionAction.OVERFLOW_TO_DISK));
    factory.setDiskSynchronous(true);
    factory.setDiskStoreName(cache.createDiskStoreFactory()
                             .setDiskDirs(new java.io.File[] { new java.io.File(System.getProperty("user.dir"))})
                             .create("TestHeapLRU")
                             .getName());
    Region region =
      cache.createRegion("TestDiskRegion",
                           factory.create());

    ThreadGroup tg = LoggingThreadGroup.createThreadGroup("Annoying threads");
    Thread thread = new Thread(tg, "Annoying thread") {
        public void run() {
          try {
            while (true) {
              System.out.println("Annoy...");
              Object[] array = new Object[10 /* * 1024 */];
              for (int i = 0; i < array.length; i++) {
                array[i] = new byte[1024];
                Thread.sleep(10);
              }

              System.out.println("SYSTEM GC");
              System.gc();
              Thread.sleep(1000);
            }

          } catch (InterruptedException ex) {
            System.err.println("Interrupted"); // FIXME should throw
          }
        }
      };
    thread.setDaemon(true);
//     thread.start();

//    ArrayList list = new ArrayList();
    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      if (i % 1000 == 0) {
//         System.out.println("i = " + i);
//         list = new ArrayList();

      } else {
//         list.add(new Integer(i));
      }

      Integer key = new Integer(i % 10000);
      long[] value = new long[2000];
//       System.out.println("Put " + key + " -> " + value);
      region.put(key, value);
    }
  }

}
