/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache.tx;

import com.gemstone.gemfire.cache.EntryExistsException;
import com.gemstone.gemfire.cache.EntryNotFoundException;
import com.gemstone.gemfire.cache.Operation;
import com.gemstone.gemfire.cache.Region.Entry;
import com.gemstone.gemfire.cache.UnsupportedOperationInTransactionException;
import com.gemstone.gemfire.cache.client.ServerOperationException;
import com.gemstone.gemfire.cache.client.internal.ServerRegionDataAccess;
import com.gemstone.gemfire.internal.cache.DistributedPutAllOperation;
import com.gemstone.gemfire.internal.cache.DistributedRemoveAllOperation;
import com.gemstone.gemfire.internal.cache.EntryEventImpl;
import com.gemstone.gemfire.internal.cache.KeyInfo;
import com.gemstone.gemfire.internal.cache.LocalRegion;
import com.gemstone.gemfire.internal.cache.tier.sockets.ClientProxyMembershipID;
import com.gemstone.gemfire.internal.cache.tier.sockets.VersionedObjectList;

import java.util.Set;

public class ClientTXRegionStub implements TXRegionStub {

//  private final LocalRegion region;
  private final ServerRegionDataAccess proxy;
  public ClientTXRegionStub(LocalRegion region) {
//    this.region = region;
    this.proxy = region.getServerProxy(); 
  }
  
  
  public boolean containsKey(KeyInfo keyInfo) {
    return proxy.containsKey(keyInfo.getKey());
  }

  
  public boolean containsValueForKey(KeyInfo keyInfo) {
    return proxy.containsValueForKey(keyInfo.getKey());
  }

  
  public void destroyExistingEntry(EntryEventImpl event, boolean cacheWrite,
      Object expectedOldValue) {
	if(event.getOperation().isLocal()) {
	  throw new UnsupportedOperationInTransactionException();
	}
    Object result = proxy.destroy(event.getKey(), expectedOldValue, event.getOperation(), event, event.getCallbackArgument());
    if (result instanceof EntryNotFoundException) {
      throw (EntryNotFoundException)result;
    }

  }

  
  public Object findObject(KeyInfo keyInfo, boolean isCreate,
      boolean generateCallbacks, Object value, boolean preferCD,
      ClientProxyMembershipID requestingClient, EntryEventImpl event, boolean allowReadFromHDFS) {
    return proxy.get(keyInfo.getKey(), keyInfo.getCallbackArg(), event);
  }

  
  public Entry<?,?> getEntry(KeyInfo keyInfo, boolean allowTombstones) {
    return proxy.getEntry(keyInfo.getKey());
  }

  
  public Object getEntryForIterator(KeyInfo keyInfo, boolean allowTombstones) {
    return getEntry(keyInfo, allowTombstones);
  }

  
  public void invalidateExistingEntry(EntryEventImpl event,
      boolean invokeCallbacks, boolean forceNewEntry) {
	  if(event.getOperation().isLocal()) {
	    throw new UnsupportedOperationInTransactionException();
	  }
	  proxy.invalidate(event);

  }

  
  public boolean putEntry(EntryEventImpl event, boolean ifNew, boolean ifOld,
      Object expectedOldValue, boolean requireOldValue, long lastModified,
      boolean overwriteDestroyed) {
    if (event.isBulkOpInProgress()) {
      // this is a put all, ignore this!
      return true;
    }
    Object result = null;
    try {
      result = proxy.put(event.getKey(), event.getRawNewValue(), event.getDeltaBytes(),
        event, event.getOperation(), requireOldValue,
        expectedOldValue, event.getCallbackArgument(), event.isCreate());
    } catch (ServerOperationException e) {
      if (e.getCause() != null && (e.getCause() instanceof EntryExistsException)) {
        throw (EntryExistsException)e.getCause();
      }
      throw e;
    }
    if (event.getOperation() == Operation.REPLACE) {
      if (!requireOldValue) { // replace(K,V,V)
        return ((Boolean)result).booleanValue();
      } else { // replace(K,V)
        event.setOldValue(result);
      }
    } else if (event.getOperation() == Operation.PUT_IF_ABSENT) {
//      if (logger.isDebugEnabled()) {
//        logger.debug("putIfAbsent for " + event.getKey() + " is returning " + result);
//      }
      event.setOldValue(result);
      if (result != null) {
        return false;
      }
    }
    return true;
  }

  
  public int entryCount() {
    return proxy.size();
  }

  
  public Set getRegionKeysForIteration(LocalRegion currRegion) {
    return proxy.keySet();
  }
  
  public void postPutAll(DistributedPutAllOperation putallOp, VersionedObjectList successfulPuts, LocalRegion r) {
	  /*
	   * Don't do anything here , it's handled in proxy and elsewhere.
	   */
  }
  @Override
  public void postRemoveAll(DistributedRemoveAllOperation op, VersionedObjectList successfulOps, LocalRegion region) {
    // Don't do anything here , it's handled in proxy and elsewhere.
  }


  @Override
  public void cleanup() {
  }
  

}
