/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.memcached.commands;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.EntryNotFoundException;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.internal.memcached.KeyWrapper;
import com.gemstone.gemfire.internal.memcached.Reply;
import com.gemstone.gemfire.internal.memcached.RequestReader;
import com.gemstone.gemfire.internal.memcached.ResponseStatus;
import com.gemstone.gemfire.internal.memcached.ValueWrapper;
import com.gemstone.gemfire.memcached.GemFireMemcachedServer.Protocol;

/**
 * The command "delete" allows for explicit deletion of items:
 * delete <key> [noreply]\r\n
 * 
 * @author Swapnil Bawaskar
 *
 */
public class DeleteCommand extends AbstractCommand {

  @Override
  public ByteBuffer processCommand(RequestReader request, Protocol protocol, Cache cache) {
    if (protocol == protocol.ASCII) {
      return processAsciiCommand(request.getRequest(), cache);
    }
    return processBinaryCommand(request, cache);
  }

  private ByteBuffer processAsciiCommand(ByteBuffer buffer, Cache cache) {
    CharBuffer flb = getFirstLineBuffer();
    getAsciiDecoder().reset();
    getAsciiDecoder().decode(buffer, flb, false);
    flb.flip();
    String firstLine = getFirstLine();
    String[] firstLineElements = firstLine.split(" ");

    assert "delete".equals(firstLineElements[0]);
    String key = stripNewline(firstLineElements[1]);
    boolean noReply = firstLineElements.length > 2;
    Region<Object, ValueWrapper> r = getMemcachedRegion(cache);
    String reply = null;
    try {
      r.destroy(key);
      reply = Reply.DELETED.toString();
    } catch (EntryNotFoundException e) {
      reply = Reply.NOT_FOUND.toString();
    }
    return noReply ? null : asciiCharset.encode(reply);
  }

  private ByteBuffer processBinaryCommand(RequestReader request, Cache cache) {
    ByteBuffer buffer = request.getRequest();
    ByteBuffer response = request.getResponse();
    
    KeyWrapper key = getKey(buffer, HEADER_LENGTH);
    
    Region<Object, ValueWrapper> r = getMemcachedRegion(cache);
    try {
      r.destroy(key);
      if (isQuiet()) {
        return null;
      }
      response.putShort(POSITION_RESPONSE_STATUS, ResponseStatus.NO_ERROR.asShort());
    } catch (EntryNotFoundException e) {
      response.putShort(POSITION_RESPONSE_STATUS, ResponseStatus.KEY_NOT_FOUND.asShort());
    } catch (Exception e) {
      response = handleBinaryException(key, request, response, "delete", e);
    }
    if (getLogger().fineEnabled()) {
      getLogger().fine("delete:key:"+key);
    }
    return response;
  }
  
  /**
   * Overridden by Q command
   */
  protected boolean isQuiet() {
    return false;
  }
}
