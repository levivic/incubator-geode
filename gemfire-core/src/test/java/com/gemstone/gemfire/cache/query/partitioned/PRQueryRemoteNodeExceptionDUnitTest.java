/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/**
 * 
 */
package com.gemstone.gemfire.cache.query.partitioned;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheClosedException;
import com.gemstone.gemfire.cache.CacheException;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.query.Query;
import com.gemstone.gemfire.cache.query.QueryInvocationTargetException;
import com.gemstone.gemfire.cache.query.cq.dunit.CqQueryTestListener;
import com.gemstone.gemfire.cache.query.data.PortfolioData;
import com.gemstone.gemfire.cache.query.internal.DefaultQuery;
import com.gemstone.gemfire.cache.query.internal.IndexTrackingQueryObserver;
import com.gemstone.gemfire.cache.query.internal.QueryObserver;
import com.gemstone.gemfire.cache.query.internal.QueryObserverAdapter;
import com.gemstone.gemfire.cache.query.internal.QueryObserverHolder;
import com.gemstone.gemfire.cache30.CacheSerializableRunnable;
import com.gemstone.gemfire.distributed.internal.DistributionMessageObserver;
import com.gemstone.gemfire.internal.cache.BucketRegion;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;
import com.gemstone.gemfire.internal.cache.PartitionedRegionDUnitTestCase;

import dunit.DistributedTestCase;
import dunit.Host;
import dunit.SerializableRunnable;
import dunit.VM;
import dunit.DistributedTestCase.WaitCriterion;

/**
 * This test verifies exception handling on coordinator node for remote as
 * well as local querying.
 * @author shobhit
 *
 */
public class PRQueryRemoteNodeExceptionDUnitTest extends PartitionedRegionDUnitTestCase {

  /**
   * constructor *
   * 
   * @param name
   */

  public PRQueryRemoteNodeExceptionDUnitTest(String name) {
    super(name);
  }

  static Properties props = new Properties();

  int totalNumBuckets = 100;

  int threadSleepTime = 500;

  int querySleepTime = 2000;

  int queryTestCycle = 10;

  PRQueryDUnitHelper PRQHelp = new PRQueryDUnitHelper("");

  final String name = "Portfolios";

  final String localName = "LocalPortfolios";

  final int cnt = 0, cntDest = 50;

  final int redundancy = 0;

  private int numOfBuckets = 10;

  @Override
  public void tearDown2() throws Exception {
    invokeInEveryVM(QueryObserverHolder.class, "reset");
    super.tearDown2();
  }

  /**
   * This test <br>
   * 1. Creates PR regions across with scope = DACK, 2
   *    data-stores <br>
   * 2. Creates a Local region on one of the VM's <br>
   * 3. Puts in the same data both in PR region & the Local Region <br>
   * 4. Queries the data both in local & PR <br>
   * 5. Puts a QueryObservers in both local as well as
   *    remote data-store node, to throw some test exceptions.  <br>
   * 6. then re-executes the query on one of the data-store node. <br>
   * 7. Verifies the exception thrown is from local node not from remote node <br>
   */
  public void testPRWithLocalAndRemoteException()
      throws Exception {

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception test Started");
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);

    List vmList = new LinkedList();
    vmList.add(vm1);
    vmList.add(vm0);

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating PR's across all VM0 , VM1");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets ));
    vm1.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets));

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created PR on VM0 , VM1");

    // creating a local region on one of the JVM's
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating Local Region on VM0");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForLocalRegionCreation(localName));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created Local Region on VM0");

    // Generating portfolio object array to be populated across the PR's & Local
    // Regions

    final PortfolioData[] portfolio = PRQHelp.createPortfolioData(cnt, cntDest);

    // Putting the data into the accessor node
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data through the accessor node");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(name, portfolio,
        cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data through the accessor node");

    // Putting the same data in the local region created
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data on local node  VM0 for result Set Comparison");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(localName,
        portfolio, cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data on local node  VM0 for result Set Comparison");

    // Execute query first time. This is to make sure all the buckets are
    // created
    // (lazy bucket creation).
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Querying on VM0 First time");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRQueryAndCompareResults(
        name, localName));

    //Insert the test hooks on local and remote node.
    //Test hook on remote node will throw CacheException while Test hook on local node will throw QueryException.
    vm1.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            class MyQueryObserver extends IndexTrackingQueryObserver {

              @Override
              public void startQuery(Query query) {
                throw new RuntimeException("For testing purpose only from remote node");
              }              
            };

            QueryObserverHolder.setInstance(new MyQueryObserver());
          };
        }
      );
    

    vm0.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            boolean gotException = false;
            Cache cache = PRQHelp.getCache();
            class MyQueryObserver extends QueryObserverAdapter {
              @Override
              public void startQuery(Query query) {
                throw new RuntimeException("For testing purpose only from local node");
              }              
            };
            
            QueryObserverHolder.setInstance(new MyQueryObserver());
            final DefaultQuery query = (DefaultQuery)cache.getQueryService().newQuery("Select * from /"+name);

            try {
              query.execute();
            } catch (Exception ex) {
              gotException = true;
              if (ex.getMessage().contains("local node")) {
//                ex.printStackTrace();
                getLogWriter().info("PRQueryRemoteNodeExceptionDUnitTest: Test received Exception from local node successfully.");
              } else {
                fail("PRQueryRemoteNodeExceptionDUnitTest: Test did not receive Exception as expected from local node rather received", ex);
              }
            }
            if (!gotException) {
              fail("PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Test did not receive Exception as expected from local as well as remote node");
            }
          }
        }
      );
    
    getLogWriter()
        .info(
            "PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception Test ENDED");
  }
  
  public void testRemoteException() throws Exception {

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception test Started");
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);

    List vmList = new LinkedList();
    vmList.add(vm1);
    vmList.add(vm0);

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating PR's across all VM0 , VM1");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets ));
    vm1.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets));

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created PR on VM0 , VM1");

    // creating a local region on one of the JVM's
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating Local Region on VM0");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForLocalRegionCreation(localName));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created Local Region on VM0");

    // Generating portfolio object array to be populated across the PR's & Local
    // Regions

    final PortfolioData[] portfolio = PRQHelp.createPortfolioData(cnt, cntDest);

    // Putting the data into the accessor node
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data through the accessor node");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(name, portfolio,
        cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data through the accessor node");

    // Putting the same data in the local region created
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data on local node  VM0 for result Set Comparison");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(localName,
        portfolio, cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data on local node  VM0 for result Set Comparison");

    vm0.invoke(new CacheSerializableRunnable(name) {
        @Override
        public void run2() throws CacheException {
          boolean gotException = false;
          Cache cache = PRQHelp.getCache();
          class MyQueryObserver extends QueryObserverAdapter {
            @Override
            public void startQuery(Query query) {
              // Replacing QueryObserver of previous test.
            }
          }
          ;
  
          QueryObserverHolder.setInstance(new MyQueryObserver());
        }
      }
    );
    
    //Insert the test hooks on local and remote node.
    //Test hook on remote node will throw CacheException while Test hook on local node will throw QueryException.
    vm1.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            class MyQueryObserver extends IndexTrackingQueryObserver {

              @Override
              public void startQuery(Query query) {
                throw new RuntimeException("For testing purpose only from remote node");
              }              
            };

            QueryObserverHolder.setInstance(new MyQueryObserver());
          };
        }
      );
    

    vm0.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            boolean gotException = false;
            Cache cache = PRQHelp.getCache();
            final DefaultQuery query = (DefaultQuery)cache.getQueryService().newQuery("Select * from /"+name);

            try {
              query.execute();
            } catch (Exception ex) {
              gotException = true;
              if (ex.getMessage().contains("remote node")) {
                ex.printStackTrace();
                getLogWriter().info("PRQueryRemoteNodeExceptionDUnitTest: Test received Exception from remote node successfully.");
              } else {
                fail("PRQueryRemoteNodeExceptionDUnitTest: Test did not receive Exception as expected from remote node rather received", ex);
              }
            }
            if (!gotException) {
              fail("PRQueryRemoteNodeExceptionDUnitTest#testRemoteException: Test did not receive Exception as expected from remote node");
            }
          }
        }
      );
    
    getLogWriter()
        .info(
            "PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception Test ENDED");
  }
  
  public void testCacheCloseExceptionFromLocalAndRemote() throws Exception {

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception test Started");
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);

    List vmList = new LinkedList();
    vmList.add(vm1);
    vmList.add(vm0);

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating PR's across all VM0 , VM1");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets ));
    vm1.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets));

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created PR on VM0 , VM1");

    // creating a local region on one of the JVM's
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating Local Region on VM0");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForLocalRegionCreation(localName));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created Local Region on VM0");

    // Generating portfolio object array to be populated across the PR's & Local
    // Regions

    final PortfolioData[] portfolio = PRQHelp.createPortfolioData(cnt, cntDest);

    // Putting the data into the accessor node
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data through the accessor node");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(name, portfolio,
        cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data through the accessor node");

    // Putting the same data in the local region created
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data on local node  VM0 for result Set Comparison");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(localName,
        portfolio, cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data on local node  VM0 for result Set Comparison");

    //Insert the test hooks on local and remote node.
    //Test hook on remote node will throw CacheException while Test hook on local node will throw QueryException.
    vm1.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            class MyQueryObserver extends IndexTrackingQueryObserver {
              private int noOfAccess = 0;
              
              @Override
              public void afterIterationEvaluation(Object result) {
                getLogWriter().info("Calling after IterationEvaluation :" + noOfAccess);
                if (noOfAccess > 2) {
                  PRQHelp.getCache().getRegion(name).destroyRegion();
                }
                ++noOfAccess;
              }              
            };

            QueryObserverHolder.setInstance(new MyQueryObserver());
          };
        }
      );
    

    vm0.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            boolean gotException = false;
            Cache cache = PRQHelp.getCache();
            class MyQueryObserver extends QueryObserverAdapter {
              private int noOfAccess = 0;
              @Override
              public void afterIterationEvaluation(Object result) {
                //Object region = ((DefaultQuery)query).getRegionsInQuery(null).iterator().next();
                getLogWriter().info("Calling after IterationEvaluation :" + noOfAccess);
                if (noOfAccess > 2) {
                  PRQHelp.getCache().close();
                }
                ++noOfAccess;
              }              
            };
            
            QueryObserverHolder.setInstance(new MyQueryObserver());
            final DefaultQuery query = (DefaultQuery)cache.getQueryService().newQuery("Select * from /"+
                name + " p where p.ID > 0");

            try {
              query.execute();
            } catch (Exception ex) {
              gotException = true;
              if (ex instanceof CacheClosedException || ex instanceof QueryInvocationTargetException) {
                getLogWriter().info(ex.getMessage());
                getLogWriter().info("PRQueryRemoteNodeExceptionDUnitTest: Test received Exception from local node successfully.");
              } else {
                fail("PRQueryRemoteNodeExceptionDUnitTest: Test did not receive Exception as expected from local node rather received", ex);
              }
            }
            if (!gotException) {
              fail("PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Test did not receive Exception as expected from local as well as remote node");
            }
          }
        }
      );
    
    getLogWriter()
        .info(
            "PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception Test ENDED");
  }
  
  public void testCacheCloseExceptionFromLocalAndRemote2() throws Exception {

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception test Started");
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);

    List vmList = new LinkedList();
    vmList.add(vm1);
    vmList.add(vm0);

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating PR's across all VM0 , VM1");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets ));
    vm1.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, redundancy, numOfBuckets));

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created PR on VM0 , VM1");

    // creating a local region on one of the JVM's
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating Local Region on VM0");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForLocalRegionCreation(localName));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created Local Region on VM0");

    // Generating portfolio object array to be populated across the PR's & Local
    // Regions

    final PortfolioData[] portfolio = PRQHelp.createPortfolioData(cnt, cntDest);

    // Putting the data into the accessor node
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data through the accessor node");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(name, portfolio,
        cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data through the accessor node");

    // Putting the same data in the local region created
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data on local node  VM0 for result Set Comparison");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(localName,
        portfolio, cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data on local node  VM0 for result Set Comparison");

    //Insert the test hooks on local and remote node.
    //Test hook on remote node will throw CacheException while Test hook on local node will throw QueryException.
    vm1.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            class MyQueryObserver extends IndexTrackingQueryObserver {
              private int noOfAccess = 0;              
              
              @Override
              public void afterIterationEvaluation(Object result) {
                getLogWriter().info("Calling after IterationEvaluation :" + noOfAccess);
                if (noOfAccess > 1) {
                  PRQHelp.getCache().getRegion(name).destroyRegion();
                }
                ++noOfAccess;
              }     
            };

            QueryObserverHolder.setInstance(new MyQueryObserver());
          };
        }
      );
    

    vm0.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            boolean gotException = false;
            Cache cache = PRQHelp.getCache();
            class MyQueryObserver extends QueryObserverAdapter {
              private int noOfAccess = 0;
              @Override
              public void afterIterationEvaluation(Object result) {
                //Object region = ((DefaultQuery)query).getRegionsInQuery(null).iterator().next();
                //getLogWriter().info("Region type:"+region);
                int i = 0;
                while (i <= 10) {
                  Region region = PRQHelp.getCache().getRegion(name);
                  if (region == null || region.isDestroyed()) {
                    //PRQHelp.getCache().close();
                    
                    break;
                  }
                  try {
                    Thread.sleep(10);
                  } catch (Exception ex) {
                  }
                  i++;
                }
              }              
            };
            
            QueryObserverHolder.setInstance(new MyQueryObserver());
            final DefaultQuery query = (DefaultQuery)cache.getQueryService().newQuery("Select * from /"
                + name + " p where p.ID > 0");

            try {
              query.execute();
            } catch (Exception ex) {
              gotException = true;
              if (ex instanceof QueryInvocationTargetException) {
                getLogWriter().info(ex.getMessage());
                getLogWriter().info("PRQueryRemoteNodeExceptionDUnitTest: Test received Exception from remote node successfully as region.destroy happened before cache.close().");
              } else {
                fail("PRQueryRemoteNodeExceptionDUnitTest: Test did not receive Exception as expected from local node rather received", ex);
              }
            }
            if (!gotException) {
              fail("PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Test did not receive Exception as expected from local as well as remote node");
            }
          }
        }
      );
    
    getLogWriter()
        .info(
            "PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception Test ENDED");
  }
  
  public void testForceReattemptExceptionFromLocal() throws Exception {

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception test Started");
    Host host = Host.getHost(0);
    VM vm0 = host.getVM(0);
    VM vm1 = host.getVM(1);
    VM vm2 = host.getVM(2);

    List vmList = new LinkedList();
    vmList.add(vm1);
    vmList.add(vm0);
    vmList.add(vm2);

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating PR's across all VM0 , VM1");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, 1/*redundancy*/, numOfBuckets ));
    vm1.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, 1/*redundancy*/, numOfBuckets));
    vm2.invoke(PRQHelp
        .getCacheSerializableRunnableForPRCreateLimitedBuckets(name, 1/*redundancy*/, numOfBuckets));

    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created PR on VM0 , VM1");

    // creating a local region on one of the JVM's
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Creating Local Region on VM0");
    vm0.invoke(PRQHelp
        .getCacheSerializableRunnableForLocalRegionCreation(localName));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Created Local Region on VM0");

    // Generating portfolio object array to be populated across the PR's & Local
    // Regions

    final PortfolioData[] portfolio = PRQHelp.createPortfolioData(cnt, cntDest);

    // Putting the data into the accessor node
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data through the accessor node");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(name, portfolio,
        cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data through the accessor node");

    // Putting the same data in the local region created
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Inserting Portfolio data on local node  VM0 for result Set Comparison");
    vm0.invoke(PRQHelp.getCacheSerializableRunnableForPRPuts(localName,
        portfolio, cnt, cntDest));
    getLogWriter()
        .info(
            "PRQueryRegionDestroyedDUnitTest#testPRWithLocalAndRemoteException: Successfully Inserted Portfolio data on local node  VM0 for result Set Comparison");

    //Insert the test hooks on local and remote node.
    //Test hook on remote node will throw CacheException while Test hook on local node will throw QueryException.
    vm1.invoke(
        new CacheSerializableRunnable(name) {
          @Override
          public void run2() throws CacheException
          {
            class MyQueryObserver extends IndexTrackingQueryObserver {
              private int noOfAccess = 0;
              
              @Override
              public void startQuery(Query query) {
                Object region = ((DefaultQuery)query).getRegionsInQuery(null).iterator().next();
                getLogWriter().info("Region type on VM1:"+region);
                if (noOfAccess == 1) {
                  PartitionedRegion pr = (PartitionedRegion)PRQHelp.getCache().getRegion(name);
                  List buks = pr.getLocalPrimaryBucketsListTestOnly();
                  getLogWriter().info("Available buckets:"+buks);
                  int bukId = ((Integer)(buks.get(0))).intValue();
                  getLogWriter().info("Destroying bucket id:"+bukId);
                  pr.getDataStore().getLocalBucketById(bukId).destroyRegion();
                }
                ++noOfAccess;
              }              
            };

            QueryObserverHolder.setInstance(new MyQueryObserver());
          };
        }
      );
    

    vm0.invoke(
        new CacheSerializableRunnable(name) {   
          @Override
          public void run2() throws CacheException
          {
            boolean gotException = false;
            Cache cache = PRQHelp.getCache();
            class MyQueryObserver extends QueryObserverAdapter {
              private int noOfAccess = 0;
              @Override
              public void startQuery(Query query) {
                Object region = ((DefaultQuery)query).getRegionsInQuery(null).iterator().next();
                getLogWriter().info("Region type on VM0:"+region);
                if (noOfAccess == 2) {
                  PartitionedRegion pr = (PartitionedRegion)PRQHelp.getCache().getRegion(name);
                  List buks = pr.getLocalPrimaryBucketsListTestOnly();
                  getLogWriter().info("Available buckets:"+buks);
                  int bukId = ((Integer)(buks.get(0))).intValue();
                  getLogWriter().info("Destroying bucket id:"+bukId);
                  pr.getDataStore().getLocalBucketById(bukId).destroyRegion();
                }
                ++noOfAccess;
              }              
            };
            
            QueryObserverHolder.setInstance(new MyQueryObserver());
            final DefaultQuery query = (DefaultQuery)cache.getQueryService().newQuery("Select * from /"+name);

            try {
              query.execute();
              getLogWriter().info("PRQueryRemoteNodeExceptionDUnitTest: Query executed successfully with ForceReattemptException on local and remote both.");
            } catch (Exception ex) {
              gotException = true;
              fail("PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Test received Exception", ex);
            }
          }
        }
      );
    
    getLogWriter()
        .info(
            "PRQueryRemoteNodeExceptionDUnitTest#testPRWithLocalAndRemoteException: Querying with PR Local/Remote Exception Test ENDED");
  }
}
