/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.size;

import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.test.junit.categories.UnitTest;

import junit.framework.TestCase;
import static com.gemstone.gemfire.internal.size.SizeTestUtil.*;

@Category(UnitTest.class)
public class ObjectSizerJUnitTest extends TestCase {
  
  
  public ObjectSizerJUnitTest() {
    super();
  }

  public ObjectSizerJUnitTest(String name) {
    super(name);
  }

  public void test() throws IllegalArgumentException, IllegalAccessException {
    long size = ObjectGraphSizer.size(new Object());
    assertEquals(OBJECT_SIZE, 8, size);
    
    assertEquals(roundup(OBJECT_SIZE + 4), ObjectGraphSizer.size(new TestObject1()));
    assertEquals(roundup(OBJECT_SIZE + 4), ObjectGraphSizer.size(new TestObject2()));
    assertEquals(roundup(OBJECT_SIZE), ObjectGraphSizer.size(new TestObject3()));
    assertEquals(roundup(OBJECT_SIZE * 2 + REFERENCE_SIZE), ObjectGraphSizer.size(new TestObject3(), true));
    assertEquals(roundup(OBJECT_SIZE + REFERENCE_SIZE), ObjectGraphSizer.size(new TestObject4()));
    assertEquals(roundup(OBJECT_SIZE + REFERENCE_SIZE) + roundup(OBJECT_SIZE + 4), ObjectGraphSizer.size(new TestObject5()));
    assertEquals(roundup(OBJECT_SIZE + REFERENCE_SIZE) + roundup(OBJECT_SIZE + REFERENCE_SIZE * 4 + 4) + roundup(OBJECT_SIZE + 4), ObjectGraphSizer.size(new TestObject6()));
  }
  
  private static class TestObject1 {
    int a;
  }
  
  private static class TestObject2 {
    int a;
  }
  
  private static class TestObject3 {
    static TestObject1 a = new TestObject1();
  }
  
  private static class TestObject4 {
    TestObject1 object = null;
  }
  
  private static class TestObject5 {
    TestObject1 object = new TestObject1();
  }
  
  private static class TestObject6 {
    TestObject1[] array =new TestObject1[4];
    
    public TestObject6() {
      array[3] = new TestObject1();
      array[2] = array[3];
    }
  }
}