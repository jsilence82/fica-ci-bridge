# Self-contained WireMock image with the SAP OData stubs baked in, so the
# Kubernetes deployment doesn't depend on a host path mount (kind/minikube
# nodes don't see the repo checkout the way docker-compose's bind mounts do).
FROM wiremock/wiremock:latest

COPY mappings/ /home/wiremock/mappings/
COPY __files/ /home/wiremock/__files/

# Keep the base image's entrypoint; only override the WireMock args (same as docker-compose.yml).
CMD ["--global-response-templating", "--verbose"]
