package com.example.ns.alpha;

import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.annotations.SchemaReference;

@Schema(
  value = """
    {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "title": "com.example.ns.alpha.AlphaOne",
      "$ref": "alpha-model.jschema#/definitions/AlphaOne"
    }
    """,
  refs = {
    @SchemaReference(name = "alpha-model.jschema", subject = "ns.alpha.v1")
  }
)
public class AlphaOne {

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
