/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache.tier.sockets;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gemstone.gemfire.cache.AttributesMutator;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.EntryNotFoundException;
import com.gemstone.gemfire.cache.EvictionAttributes;
import com.gemstone.gemfire.cache.InterestPolicy;
import com.gemstone.gemfire.cache.PartitionAttributesFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionFactory;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.SubscriptionAttributes;
import com.gemstone.gemfire.cache.client.PoolFactory;
import com.gemstone.gemfire.cache.client.PoolManager;
import com.gemstone.gemfire.cache.server.CacheServer;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.cache30.CacheTestCase;
import com.gemstone.gemfire.internal.AvailablePort;
import com.gemstone.gemfire.internal.cache.CachedDeserializable;
import com.gemstone.gemfire.internal.cache.LocalRegion;
import com.gemstone.gemfire.internal.cache.Token;

import dunit.Host;
import dunit.SerializableRunnable;
import dunit.VM;

/**
 * @author dsmith
 *
 */
public class ForceInvalidateEvictionDUnitTest extends CacheTestCase {
  
  private static final long serialVersionUID = -11364213547039967L;

  public ForceInvalidateEvictionDUnitTest(String name) {
    super(name);
  }

  private void doPropagationTest(VM sourceVM, VM destinationVm, boolean validateCallbacks, boolean validateContent) {

    addListener(destinationVm);

    //Put some entries. They will be locally expired
    putEntries(sourceVM, 0, 20);
    
    //Make sure the local expiration happened
    checkValue(sourceVM, 0, null);
    
    //Make sure there was no expiration in the destination VM
    if(validateContent) {
      checkValue(destinationVm, 0, "value");
    }
    
    //do an invalidate
    invalidateEntry(sourceVM, 0);
    
    //Make sure the invalidate didn't stick around
    checkValue(sourceVM, 0, null);
    
    //Make sure the destination receives invalidate
    if(validateContent) {
      checkValue(destinationVm, 0, Token.INVALID);
    }
    
    if(validateCallbacks) {
      checkAndClearListener(destinationVm, 0, true);
    }
    
    removeListener(destinationVm);
  }
  
  public void testPRToAccessor() {
    
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);
    VM vm2 = host.getVM(2);
    
    createPR(vm0);
    createPR(vm1);
    createAccessor(vm2, true);
    
    
    //Do the test twice, to make sure both
    //primary and secondary propagate the event
    doPropagationTest(vm0, vm2, true, false);
    doPropagationTest(vm1, vm2, true, false);
  }
  
  public void testBridgeClientWithPR() {
    
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);
    VM vm3 = host.getVM(3);
    
    createPR(vm0);
    createPR(vm1);
    int port = addBridgeServer(vm1);
    createClient(vm3, port);
    
    
    //Do the test twice, to make sure both
    //primary and secondary propagate the event
    doPropagationTest(vm0, vm3, true, true);
    doPropagationTest(vm1, vm3, true, true);
  }

  public void testBridgeClientWithAccessorServer() {
    
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);
    VM vm2 = host.getVM(2);
    VM vm3 = host.getVM(3);
    
    createPR(vm0);
    createPR(vm1);
    createAccessor(vm2, false);
    int port = addBridgeServer(vm2);
    createClient(vm3, port);
    
    doPropagationTest(vm0, vm3, true, true);
  }
  
  public void testBridgeClientWithAccessorSource() {
    
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);
    VM vm2 = host.getVM(2);
    VM vm3 = host.getVM(3);
    
    createPR(vm0);
    createPR(vm1);
    createAccessor(vm2, false);
    
    //test an invalidate from the accessor through one of the data stores
    int port1 = addBridgeServer(vm0);
    createClient(vm3, port1);
    doPropagationTest(vm2, vm3, true, true);
    vm3.invoke(new SerializableRunnable("close cache") {
      
      public void run() {
        Cache cache = getCache();
        cache.close();
      }
    });
    
    //test an invalidate from the accessor through the other data store
    int port2 = addBridgeServer(vm1);
    createClient(vm3, port2);
    doPropagationTest(vm2, vm3, true, true);
  }
  
  
  private void createPR(VM vm) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      
      public void run() {
        Cache cache = getCache();
        RegionFactory rf = new RegionFactory();
        rf.setOffHeap(isOffHeapEnabled());
        rf.setDataPolicy(DataPolicy.PARTITION);
        
        PartitionAttributesFactory paf = new PartitionAttributesFactory();
        paf.setRedundantCopies(1);
        paf.setTotalNumBuckets(5);
        rf.setPartitionAttributes(paf.create());
        rf.setEvictionAttributes(EvictionAttributes.createLRUEntryAttributes(1));
        rf.setConcurrencyChecksEnabled(false);
        rf.create(name);
      }
    });
  }
  
  private void createAccessor(VM vm, final boolean allContent) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      
      public void run() {
        Cache cache = getCache();
        RegionFactory rf = new RegionFactory();
        rf.setOffHeap(isOffHeapEnabled());
        rf.setDataPolicy(DataPolicy.PARTITION);
        
        PartitionAttributesFactory paf = new PartitionAttributesFactory();
        paf.setRedundantCopies(1);
        paf.setTotalNumBuckets(5);
        paf.setLocalMaxMemory(0);
        rf.setPartitionAttributes(paf.create());
        rf.setEvictionAttributes(EvictionAttributes.createLRUEntryAttributes(1));
        rf.setConcurrencyChecksEnabled(false);
        if(allContent) {
//          rf.initCacheListeners(new CacheListener [] { new MyListener()});
          rf.setSubscriptionAttributes(new SubscriptionAttributes(InterestPolicy.ALL));
        }
        rf.create(name);
      }
    });
  }
  
  private void addListener(VM vm) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      
      public void run() {
        Cache cache = getCache();
        Region region = cache.getRegion(name);
        AttributesMutator am = region.getAttributesMutator();
        am.initCacheListeners(new CacheListener[] { new MyListener() });
      }
    });
  }
  
  private void removeListener(VM vm) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      
      public void run() {
        Cache cache = getCache();
        Region region = cache.getRegion(name);
        AttributesMutator am = region.getAttributesMutator();
        am.initCacheListeners(null);
      }
    });
  }
  
  private void checkAndClearListener(VM vm, final Serializable key, final boolean invalidated) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      public void run() {
        Cache cache = getCache();
        Region region = cache.getRegion(name);
        final MyListener listener = (MyListener) region.getAttributes().getCacheListeners()[0];
        if(invalidated) {
          waitForCriterion(new WaitCriterion() {

            public String description() {
              return "Didn't receive invalidate after 30 seconds";
            }

            public boolean done() {
              return listener.remove(key);
            }
            
          }, 30000, 100, true);
        } else {
          assertFalse(listener.remove(key));
        }
      }
    });
  }
  
  
  private void checkValue(VM vm, final Serializable key, final Object expected) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      public void run() {
        Cache cache = getCache();
        final LocalRegion region = (LocalRegion) cache.getRegion(name);
        
        waitForCriterion(new WaitCriterion() {
          
          public boolean done() {
            Object value  = null;
            try {
              value = region.getValueInVM(key);
              if(value instanceof CachedDeserializable) {
                value = ((CachedDeserializable) value).getDeserializedForReading();
              }
            } catch(EntryNotFoundException e) {
              //ok
            }
            return expected == null ? value == null : expected.equals(value);
          }
          
          public String description() {
            return "Value did not become " + expected + " after 30s: " + region.getValueInVM(key);
          }
        }, 30000, 100, true);
        
      }
    });
  }

  private void invalidateEntry(VM vm, final Serializable key) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      public void run() {
        Cache cache = getCache();
        Region region = cache.getRegion(name);
        region.invalidate(key);
      }
    });
  }

  private void putEntries(VM vm, final int start, final int end) {
    final String name = getUniqueName();
    vm.invoke(new SerializableRunnable() {
      public void run() {
        Cache cache = getCache();
        Region region = cache.getRegion(name);
        for(int i = start; i < end; i++) {
          region.put(i, "value");
        }
      }
    });
  }
  
  private void createClient(VM vm, final int port) {
    final String name = getUniqueName();
    final Host host = Host.getHost(0);
    vm.invoke(new SerializableRunnable() {
      
      public void run() {
        Cache cache = getCache();
        
        PoolFactory pf = PoolManager.createFactory();
        pf.addServer(getServerHostName(host), port);
        pf.setSubscriptionEnabled(true);
        pf.create(name);
        RegionFactory rf = new RegionFactory();
        rf.setOffHeap(isOffHeapEnabled());
        rf.setScope(Scope.LOCAL);
        rf.setPoolName(name);
        Region region = rf.create(name);
        region.registerInterest("ALL_KEYS");
      }
    });
    
  }

  private int addBridgeServer(VM vm) {
    final int port = AvailablePort.getRandomAvailablePort(AvailablePort.SOCKET);
    vm.invoke(new SerializableRunnable("add bridge server") {
      public void run() {
        Cache cache =getCache();
        CacheServer server = cache.addCacheServer();
        server.setNotifyBySubscription(true);
        server.setPort(port);
        try {
          server.start();
        } catch (IOException e) {
          fail("IO Exception", e);
        }
      }
    });
    
    return port;
  }

  private static class MyListener<K,V> extends CacheListenerAdapter<K,V> {
    //Used because a CM can't handle nulls.
    
    private static final Object VALUE = new Object();
    Map invalidates = new ConcurrentHashMap();

    @Override
    public void afterInvalidate(EntryEvent<K, V> event) {
      Object oldValue = event.getOldValue();
      invalidates.put(event.getKey(), VALUE);
    }
    
    public boolean remove(K key) {
      return invalidates.remove(key) != null;
    }
  } 

  public boolean isOffHeapEnabled() {
    return false;
  }      
}
