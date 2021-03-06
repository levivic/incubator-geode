package com.gemstone.gemfire.internal.redis.executor.set;

import java.util.List;
import java.util.Set;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.internal.redis.ByteArrayWrapper;
import com.gemstone.gemfire.internal.redis.Command;
import com.gemstone.gemfire.internal.redis.ExecutionHandlerContext;
import com.gemstone.gemfire.internal.redis.RedisConstants.ArityDef;
import com.gemstone.gemfire.internal.redis.RedisDataType;
import com.gemstone.gemfire.internal.redis.Coder;

public class SMembersExecutor extends SetExecutor {

  @Override
  public void executeCommand(Command command, ExecutionHandlerContext context) {
    List<byte[]> commandElems = command.getProcessedCommand();

    if (commandElems.size() < 2) {
      command.setResponse(Coder.getErrorResponse(context.getByteBufAllocator(), ArityDef.SMEMBERS));
      return;
    }

    ByteArrayWrapper key = command.getKey();
    checkDataType(key, RedisDataType.REDIS_SET, context);
    @SuppressWarnings("unchecked")
    Region<ByteArrayWrapper, Boolean> keyRegion = (Region<ByteArrayWrapper, Boolean>) context.getRegionCache().getRegion(key);
    
    if (keyRegion == null) {
      command.setResponse(Coder.getEmptyArrayResponse(context.getByteBufAllocator()));
      return;
    }
    
    Set<ByteArrayWrapper> members = keyRegion.keySet();
    
    command.setResponse(Coder.getBulkStringArrayResponse(context.getByteBufAllocator(), members));
  }
}
