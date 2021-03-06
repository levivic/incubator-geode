/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache.hdfs.internal.hoplog.mapred;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.lib.CombineFileSplit;

import com.gemstone.gemfire.cache.hdfs.internal.PersistedEventImpl;
import com.gemstone.gemfire.cache.hdfs.internal.hoplog.mapreduce.GFKey;
import com.gemstone.gemfire.cache.hdfs.internal.hoplog.mapreduce.HoplogUtil.HoplogOptimizedSplitter;

public class GFInputFormat extends
    com.gemstone.gemfire.cache.hdfs.internal.hoplog.mapreduce.GFInputFormat
    implements InputFormat<GFKey, PersistedEventImpl>, JobConfigurable {

  @Override
  public InputSplit[] getSplits(JobConf job, int numSplits) throws IOException {
    this.conf = job;

    Collection<FileStatus> hoplogs = getHoplogs();
    return createSplits(job, hoplogs);
  }

  /**
   * Creates an input split for every block occupied by hoplogs of the input
   * regions
   * 
   * @param job 
   * @param hoplogs
   * @return array of input splits of type file input split
   * @throws IOException
   */
  private InputSplit[] createSplits(JobConf job, Collection<FileStatus> hoplogs)
      throws IOException {
    if (hoplogs == null || hoplogs.isEmpty()) {
      return new InputSplit[0];
    }

    HoplogOptimizedSplitter splitter = new HoplogOptimizedSplitter(hoplogs);
    List<org.apache.hadoop.mapreduce.InputSplit> mr2Splits = splitter.getOptimizedSplits(conf);
    InputSplit[] splits = new InputSplit[mr2Splits.size()];
    int i = 0;
    for (org.apache.hadoop.mapreduce.InputSplit inputSplit : mr2Splits) {
      org.apache.hadoop.mapreduce.lib.input.CombineFileSplit mr2Spit;
      mr2Spit = (org.apache.hadoop.mapreduce.lib.input.CombineFileSplit) inputSplit;
      
      CombineFileSplit split = new CombineFileSplit(job, mr2Spit.getPaths(),
          mr2Spit.getStartOffsets(), mr2Spit.getLengths(),
          mr2Spit.getLocations());
      splits[i] = split;
      i++;
    }

    return splits;
  }

  @Override
  public RecordReader<GFKey, PersistedEventImpl> getRecordReader(
      InputSplit split, JobConf job, Reporter reporter) throws IOException {

    CombineFileSplit cSplit = (CombineFileSplit) split;
    AbstractGFRecordReader reader = new AbstractGFRecordReader();
    reader.initialize(cSplit, job);
    return reader;
  }

  @Override
  public void configure(JobConf job) {
    this.conf = job;
  }
}
