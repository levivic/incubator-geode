package com.gemstone.gemfire.internal.logging;

import java.io.File;
import java.io.IOException;

import com.gemstone.gemfire.internal.util.StopWatch;

import junit.framework.TestCase;

/**
 * Tests performance of logging when level is OFF.
 * 
 * @author Kirk Lund
 */
public abstract class LoggingPerformanceTestCase extends TestCase {

  protected static final boolean TIME_BASED = Boolean.getBoolean("gemfire.test.LoggingPerformanceTestCase.TIME_BASED");
  protected static final long TIME_TO_RUN = 1000 * 60 * 10; // ten minutes
  protected static final int LOG_SETS = 1000;
  protected static final int LOG_REPETITIONS_PER_SET = 1000;
  protected static final String message = "This is a log message";
  
  protected File configDirectory = new File(getUniqueName());//null;
  protected File logFile = new File(configDirectory, getUniqueName()+".log");

  public LoggingPerformanceTestCase(String name) {
    super(name);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    this.configDirectory = null; // leave this directory in place for now
  }

  protected String getUniqueName() {
    return getClass().getSimpleName() + "_" + getName();
  }
  
  protected long performLoggingTest(final PerformanceLogger perfLogger) {
    if (TIME_BASED) {
      return performTimeBasedLoggingTest(perfLogger);
    } else {
      return performCountBasedLoggingTest(perfLogger);
    }
  }
  
  protected long performIsEnabledTest(final PerformanceLogger perfLogger) {
    if (TIME_BASED) {
      return performTimeBasedIsEnabledTest(perfLogger);
    } else {
      return performCountBasedIsEnabledTest(perfLogger);
    }
  }
  
  protected long performTimeBasedLoggingTest(final PerformanceLogger perfLogger) {
    System.out.println("\nBeginning " + getUniqueName());
    
    final StopWatch stopWatch = new StopWatch(true);
    long count = 0;
    while (stopWatch.elapsedTimeMillis() < TIME_TO_RUN) {
      perfLogger.log(message);
      count++;
    }
    stopWatch.stop();
    
    final long millis = stopWatch.elapsedTimeMillis();
    final long seconds = millis / 1000;
    final long minutes = seconds / 60;
    
    System.out.println(getUniqueName() + " performTimeBasedLoggingTest");
    System.out.println("Number of log statements: " + count);
    System.out.println("Total elapsed time in millis: " + millis);
    System.out.println("Total elapsed time in seconds: " + seconds);
    System.out.println("Total elapsed time in minutes: " + minutes);
    
    return millis;
  }
  
  protected long performCountBasedLoggingTest(final PerformanceLogger perfLogger) {
    System.out.println("\nBeginning " + getUniqueName());
    
    final StopWatch stopWatch = new StopWatch(true);
    for (int sets = 0; sets < LOG_SETS; sets++) {
      for (int count = 0; count < LOG_REPETITIONS_PER_SET; count++) {
        perfLogger.log(message);
        //fail("KIRK");
      }
    }
    stopWatch.stop();
    
    final long millis = stopWatch.elapsedTimeMillis();
    final long seconds = millis / 1000;
    final long minutes = seconds / 60;
    
    System.out.println(getUniqueName() + " performCountBasedLoggingTest");
    System.out.println("Number of log statements: " + LOG_SETS * LOG_REPETITIONS_PER_SET);
    System.out.println("Total elapsed time in millis: " + millis);
    System.out.println("Total elapsed time in seconds: " + seconds);
    System.out.println("Total elapsed time in minutes: " + minutes);
    
    return millis;
  }
  
  protected long performTimeBasedIsEnabledTest(final PerformanceLogger perfLogger) {
    System.out.println("\nBeginning " + getUniqueName());
    
    final StopWatch stopWatch = new StopWatch(true);
    long count = 0;
    while (stopWatch.elapsedTimeMillis() < TIME_TO_RUN) {
      perfLogger.isEnabled();
      count++;
    }
    stopWatch.stop();
    
    final long millis = stopWatch.elapsedTimeMillis();
    final long seconds = millis / 1000;
    final long minutes = seconds / 60;
    
    System.out.println(getUniqueName() + " performTimeBasedIsEnabledTest");
    System.out.println("Number of isEnabled statements: " + count);
    System.out.println("Total elapsed time in millis: " + millis);
    System.out.println("Total elapsed time in seconds: " + seconds);
    System.out.println("Total elapsed time in minutes: " + minutes);
    
    return millis;
  }
  
  protected long performCountBasedIsEnabledTest(final PerformanceLogger perfLogger) {
    System.out.println("\nBeginning " + getUniqueName());
    
    final StopWatch stopWatch = new StopWatch(true);
    for (int sets = 0; sets < LOG_SETS; sets++) {
      for (int count = 0; count < LOG_REPETITIONS_PER_SET; count++) {
        perfLogger.isEnabled();
      }
    }
    stopWatch.stop();
    
    final long millis = stopWatch.elapsedTimeMillis();
    final long seconds = millis / 1000;
    final long minutes = seconds / 60;
    
    System.out.println(getUniqueName() + " performCountBasedIsEnabledTest");
    System.out.println("Number of isEnabled statements: " + LOG_SETS * LOG_REPETITIONS_PER_SET);
    System.out.println("Total elapsed time in millis: " + millis);
    System.out.println("Total elapsed time in seconds: " + seconds);
    System.out.println("Total elapsed time in minutes: " + minutes);
    
    return millis;
  }
  
  protected abstract PerformanceLogger createPerformanceLogger() throws IOException;
  
  public void testCountBasedLogging() throws Exception {
    performCountBasedLoggingTest(createPerformanceLogger());
    assertTrue(this.logFile.exists());
  }

  public void testTimeBasedLogging() throws Exception {
    performTimeBasedLoggingTest(createPerformanceLogger());
    assertTrue(this.logFile.exists());
  }

  public void testCountBasedIsEnabled() throws Exception {
    performCountBasedIsEnabledTest(createPerformanceLogger());
    assertTrue(this.logFile.exists());
  }

  public void testTimeBasedIsEnabled() throws Exception {
    performTimeBasedIsEnabledTest(createPerformanceLogger());
    assertTrue(this.logFile.exists());
  }
  
  public static interface PerformanceLogger {
    public void log(final String message);
    public boolean isEnabled();
  }
}
