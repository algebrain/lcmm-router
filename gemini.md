# Gemini Context: Clojure Modular Router

This file provides context for the Gemini assistant working on this project.

## Project Overview

- **Goal:** To develop a centralized router component in Clojure for a modular monolith architecture.
- **Technology Stack:** Clojure, `reitit` for routing, `http-kit` as the target webserver, and `kaocha` for testing.
- **Core Concept:** The router implements an Inversion of Control (IoC) pattern. Modules are given an instance of the router during initialization and use it to register their own HTTP endpoints. The router then compiles all registered routes into a single Ring-compliant handler.

## Key Files

- `deps.edn`: Project dependencies and test runner configuration.
- `src/lcmm/router.clj`: The main implementation of the router component.
- `test/lcmm/router_test.clj`: Unit and integration tests for the router.
- `docs/`: Directory for documentation in Russian.
- `docs_en/`: Directory for documentation in English.

## Development Workflow

### Running Tests

The primary command to verify functionality is running the test suite. The project uses `kaocha` as the test runner.

```bash
clj -M:test
```

### Important Implementation Details

- **`as-ring-handler` Metadata:** The `as-ring-handler` function attaches the compiled `reitit` router object to the returned Ring handler's metadata. The key `::lcmm.router/router` is used for this purpose. This is crucial for tests that need to inspect the compiled router (e.g., `reitit.core/match-by-name`).

### Documentation

- Documentation is maintained in two languages. When updating documentation, be sure to update both the Russian version in `docs/` and the English version in `docs_en/`.
