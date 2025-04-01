package com.example.ns.beta;

import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.annotations.SchemaReference;

@Schema(
  value = """
    {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "ns.beta.v1/BetaTwo",
      "$ref": "beta-model.jschema#/definitions/BetaTwo"
    }
    """,
  refs = {
    @SchemaReference(name = "beta-model.jschema", subject = "ns.beta.v1")
  }
)
public class BetaTwo {

  private String hash;

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

}
