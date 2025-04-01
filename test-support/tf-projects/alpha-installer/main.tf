# example resource

terraform {
  required_version = "~> 1.6"

  required_providers {

    kscharc = {
      source  = "local/fares-io/kscharc"
      version = "1.0.0"
    }

    # maven = {
    #   source = "registry.terraform.io/kota65535/maven"
    #   version = "0.2.1"
    # }

  }
}

provider "kscharc" {

}


data "kscharc_model" "test_model" {


}

# data "maven_artifact" "commons" {
#   group_id    = "io.fares.examples"
#   artifact_id = "alpha-model-project"
#   version     = "1.0.0"
#   output_dir  = "${path.root}/.terraform/tmp"
# }
