package com.example.ns.alpha;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelTest {

  @Test
  void itShouldCreateOneEntity() {
    AlphaOne alphaOne = new AlphaOne();
    alphaOne.setId("id");
    alphaOne.setName("name");
    assertEquals("id", alphaOne.getId());
    assertEquals("name", alphaOne.getName());
  }

  @Test
  void itShouldCreateTwoEntity() {
    AlphaTwo entityOne = new AlphaTwo();
    entityOne.setHash("hash");
    assertEquals("hash", entityOne.getHash());
  }

}
