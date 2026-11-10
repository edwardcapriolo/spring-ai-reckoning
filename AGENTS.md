# Contribution Guide

- Preserve the Java version split: client, model, and demo modules target Java 17; embedded targets Java 25.
- Keep the four-module structure and `io.teknek.reckoning` coordinates aligned with the parent POM.
- Run `mvn clean install` before submitting changes.
- Keep Docker model integration tests opt-in with `RECKONING_RUN_DOCKER_IT=true`.
- Do not commit generated `target/` output, credentials, model files, or local cache contents.
