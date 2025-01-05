package com.example.ns.beta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelTest {

  @Test
  void itShouldCreateOneEntity() {
    BetaOne betaOne = new BetaOne();
    betaOne.setId("id");
    betaOne.setName("name");
    assertEquals("id", betaOne.getId());
    assertEquals("name", betaOne.getName());
  }

  @Test
  void itShouldCreateTwoEntity() {
    BetaTwo entityOne = new BetaTwo();
    entityOne.setHash("hash");
    assertEquals("hash", entityOne.getHash());
  }

}
