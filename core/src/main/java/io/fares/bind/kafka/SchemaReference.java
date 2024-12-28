package io.fares.bind.kafka;

public class SchemaReference {

  private String subject;

  private String name;

  private int version = -1;

  public String getSubject() {
    return subject;
  }

  void setSubject(String subject) {
    this.subject = subject;
  }

  public String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  public int getVersion() {
    return version;
  }

  void setVersion(int version) {
    this.version = version;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder()
      .append("resourceName=").append(name)
      .append(" subject=").append(subject);

    if (version > -1) {
      sb.append(" version=").append(version);
    }

    return sb.toString();
  }

}
