package com.example.ns.beta;

import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.annotations.SchemaReference;

@Schema(
  value = """
    {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "ns.beta.v1/BetaOne",
      "$ref": "beta-model.jschema#/definitions/BetaOne"
    }
    """,
  refs = {
    @SchemaReference(name = "beta-model.jschema", subject = "ns.beta.v1")
  }
)
public class BetaOne {

  private String id;

  private String name;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
