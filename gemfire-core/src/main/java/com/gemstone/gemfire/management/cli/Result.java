/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.management.cli;

import java.io.IOException;

/**
 * The result of processing a GemFire Command Line Interface (CLI) command
 * string.
 * 
 * A string representation of this Result can be iterated over using the methods
 * {@link #hasNextLine()}, {@link #nextLine()} and {@link #resetToFirstLine()}.
 * 
 * A Result may have one or more files as part of the command output and if so
 * they can be saved to the file system using {@link #saveIncomingFiles(String)}.
 * To check whether the result has a file in it use
 * {@link #hasIncomingFiles()}.
 * 
 * @author Kirk Lund
 * @author Abhishek Chaudhari
 * 
 * @since 7.0
 */
public interface Result {
  
  /**
   * Indicates a Results status.
   * 
   * @author Kirk Lund
   * @since 7.0
   */
  public enum Status {
    /**
     * Indicates that the command completed successfully.
     */
    OK(0),
    
    /**
     * Indicates that an error occurred while processing the command.
     */
    ERROR(-1);
    
    private final int code;

    private Status(int code) {
      this.code = code;
    }

    /**
     * Returns the code associated with this state.
     */
    public int getCode() {
      return this.code;
    }
  }

  /**
   * Returns the status of a processed command.
   */
  public Status getStatus();
  
  /**
   * Resets the pointer to the first line in the Result.
   */
  public void resetToFirstLine();

  /**
   * Returns whether the result has any more lines of information.
   * 
   * @return True if there are more lines, false otherwise.
   */
  public boolean hasNextLine();

  /**
   * Returns the next line of information from the Result.
   * 
   * @throws IndexOutOfBoundsException
   *           if this method is called more times than there are lines of
   *           information.
   */
  public String nextLine();
  
  /**
   * Returns whether this Result has a file as a part of the command output.
   * 
   * @return True if there is a file, false otherwise.
   */
  public boolean hasIncomingFiles();
  
  /**
   * Save the file(s) from this Result. {@link #hasIncomingFiles()} should be used
   * before calling this method to verify that the Result contains a file.
   * 
   * @param directory
   *          Directory to which the file(s) should be saved.
   * @throws IOException
   *           If an error occurs while saving the file.
   * @throws RuntimeException
   *           If there is no file in the Result to save.
   */
  public void saveIncomingFiles(String directory) throws IOException;
  
  /****
   * Return whether the configuration changes due to command have been persisted to cluster configuration or not.
   * 
   * @return True if the command has failed to persist configuration changes , false otherwise.
   */
  public boolean failedToPersist();
  
  /*****
   * Sets whether the command changes have not been persisted to the cluster configuration 
   * @param commandPersisted true if the command changes are persisted to the cluster configuration, false otherwise.
   */
  public void setCommandPersisted(boolean commandPersisted);
}

