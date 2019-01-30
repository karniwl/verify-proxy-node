#!/usr/bin/env bash

GRADLE_CONFIGURATIONS="dropwizard saml saml_gov common soft_hsm"

exit_code=0

for configuration in $GRADLE_CONFIGURATIONS; do
    echo "######################################################################"
    echo "### Testing dependencies for $configuration gradle configuration"
    echo "######################################################################"
    snyk test --gradle-sub-project=proxy-node-translator -- --configuration="$configuration"
    if [[ $? -gt 0 ]]; then
        exit_code=1
    fi
done

exit $exit_code