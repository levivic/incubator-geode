/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.query.data.Portfolio;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

/**
 * A sample test class using the QueryTestUtils
 * 
 * @author Tejas Nomulwar
 * 
 */
@Category(IntegrationTest.class)
public class QueryTestUtilsJUnitTest {

  private static final long serialVersionUID = 1L;

  QueryTestUtils utils;

  @Before
  public void setUp() throws Exception {

    utils = new QueryTestUtils();
    utils.createCache(null);
    // create regions
    utils.createReplicateRegion("exampleRegion");
    utils.createReplicateRegion("numericRegion");
    // put entries in the region
    utils.createValuesStringKeys("exampleRegion", 10);
    utils.createNumericValuesStringKeys("numericRegion", 10);
  }

  @Test
  public void testQueries()  {
    String[] queries = { "1" }; //SELECT * FROM /exampleRegion WHERE status = 'active'
    int results = 0;
    try {
      for (Object result :  utils.executeQueries(queries)) {
        if (result instanceof SelectResults) {
         Collection<?> collection = ((SelectResults<?>) result).asList();
         results = collection.size();
         assertEquals(5, results);
         for (Object e : collection) {
           if(e instanceof Portfolio){
            assertEquals(true,((Portfolio)e).isActive());
           }
         }
       }
  }
    } catch (Exception e) {
      fail("Query execution failed. : " + e);
    }
    // execute all the queries from the map
    // utils.executeAllQueries();
  }
  
  @Test
  public void testQueriesWithoutDistinct() throws Exception{
    utils.createDiffValuesStringKeys("exampleRegion", 2);
    String[] queries = { "181" };
    int results = 0;
    for (Object result :  utils.executeQueriesWithoutDistinct(queries)) {
      if (result instanceof SelectResults) {
       Collection<?> collection = ((SelectResults<?>) result).asList();
       results = collection.size();
       assertEquals(9, results);
       List expectedIds = new ArrayList(Arrays.asList( 10, 9, 8, 7, 6, 5, 4, 3, 3 ));
       for (Object e : collection) {
         if (e instanceof Portfolio) {
           assertTrue(expectedIds.contains(((Portfolio) e).getID()));
           expectedIds.remove((Integer)((Portfolio) e).getID());
         }
       }
     }
   }
  }

  @Test
  public void testQueriesWithDistinct() throws Exception{
    String[] queries = { "181" };
    int results = 0;
    int i = 7;
    for (Object result :  utils.executeQueriesWithDistinct(queries)) {
      if (result instanceof SelectResults) {
       Collection<?> collection = ((SelectResults<?>) result).asList();
       results = collection.size();
       assertEquals(8, results);
       List expectedIds = new ArrayList(Arrays.asList( 10, 9, 8, 7, 6, 5, 4, 3, 2 ));
       for (Object e : collection) {
         if (e instanceof Portfolio) {
           assertTrue(expectedIds.contains(((Portfolio) e).getID()));
           expectedIds.remove((Integer)((Portfolio) e).getID());
         }
       }
     }
   }    
  }
  
  @After
  public void tearDown() throws Exception{
    utils.closeCache();
  }


}
