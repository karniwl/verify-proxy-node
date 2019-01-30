#!/usr/bin/env bash

GRADLE_CONFIGURATIONS="dropwizard saml saml_gov common soft_hsm"

for configuration in $GRADLE_CONFIGURATIONS; do
    echo "######################################################################"
    echo "### Monitoring dependencies for $configuration gradle configuration"
    echo "######################################################################"
    snyk monitor -d --gradle-sub-project=proxy-node-translator --project-name="$configuration"-config -- --configuration="$configuration"
done;