# Terraform Provider

terraform-provider-kschark

The data model library is a data resource.

```hcl
data "maven_library" "dependency_alpha_one" {
  groupId    = ""
  artifactId = ""
  version    = ""
}
```

result schema

```hcl
{
"models" = []

}
```

## Local Development

* [Terraform Provider Local Installation](https://developer.hashicorp.com/terraform/enterprise/run/install-software)
* [Terraform Provider Wire Protocol Spec](https://github.com/hashicorp/terraform/blob/main/docs/plugin-protocol/object-wire-format.md)
* [MessagePack Jackson](https://github.com/msgpack/msgpack-java/blob/main/msgpack-jackson/README.md)

The [code](https://github.com/hashicorp/go-plugin/blob/v1.6.2/client.go#L854) in the Terraform plugin client that parses the server-client handshake string.

The [code](https://github.com/hashicorp/go-plugin/blob/v1.6.2/client.go#L916) in the Terraform plugin client where the server cert is parsed.

Local Debugging

set env variable `TF_REATTACH_PROVIDERS` to tell terraform to connect to an existing grpc server

```json
{
    "local/fares-io/kscharc": {
        "ProtocolVersion": 6,
        "Protocol": "grpc",
        "Pid": 98144,
        "Test": true,
        "Addr": {
            "Network": "tcp",
            "String": "127.0.0.1:9090"
        }
    }
}
```


```json
{
  "local/fares-io/kscharc": {
  Protocol:"grpc",
  "Pid":'%d',
  "Test":true,
    "Addr":{
      "Network":"tcp",
      "String": "{host}:{serverUri.Port}"}
  }
}
```

Install the build output as local terraform provider into test project:

```shell
TDIR=../test-support/tf-projects/alpha-installer/terraform.d

mkdir -p ${TDIR}/plugins/local/fares-io/kscharc/1.0.0/darwin_arm64

cp target/confluent-kafka-schema-registry-terraform-provider ${TDIR}/plugins/local/fares-io/kscharc/1.0.0/darwin_arm64/terraform-provider-kscharc
```


look into `HostApplicationLifetimeExtensions`

