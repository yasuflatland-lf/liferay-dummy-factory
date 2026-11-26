# Java unit tests (JUnit)

Scope: `modules/liferay-dummy-factory/src/test/`. Also obey [`../../AGENTS.md`](../../AGENTS.md) (module) and the root [`AGENTS.md`](../../../../AGENTS.md). Java conventions from [`src/main/java/AGENTS.md`](../main/java/AGENTS.md) apply here too.

Run: `./gradlew :modules:liferay-dummy-factory:test` (JaCoCo report: [coverage](../../../../docs/reference/test-harness.md#java-unit-tests)).

- Test design (what to test, branch coverage, RNG locks, response-shape locks) follows [`integration-test/AGENTS.md`](../../../../integration-test/AGENTS.md#what-to-test); this suite is where pure logic is tested first.
- Every new class under `utils/` has a matching test class here.
