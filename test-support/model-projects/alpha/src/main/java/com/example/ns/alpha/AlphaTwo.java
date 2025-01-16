package com.example.ns.alpha;

import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.annotations.SchemaReference;

@Schema(
  value = """
    {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "ns.alpha.v1/AlphaTwo",
      "$ref": "alpha-model.jschema#/definitions/AlphaTwo"
    }
    """,
  refs = {
    @SchemaReference(name = "alpha-model.jschema", subject = "ns.alpha.v1")
  }
)
public class AlphaTwo {

  private String hash;

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

}
