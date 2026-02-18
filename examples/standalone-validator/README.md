# Standalone Validator

Example project demonstrating how to use the `qnx-buildfile-lang` library for
buildfile validation with variable substitution and directory analysis.

## What it does

For each input buildfile:

1. **Parse** — loads the buildfile into a model (syntax check)
2. **Variable substitution** — resolves `${VAR}` references from environment
   variables and optional `-e KEY=VALUE` overrides
3. **Unresolved variable detection** — warns (or errors with `--strict-vars`)
   about any `${VAR}` that couldn't be resolved
4. **Validation** — runs the standard validators (attribute names, values,
   duplicate paths). Optionally loads a custom validator JAR with `-c`
5. **Directory deployment analysis** — detects `[type=dir]` deployments that
   copy a host directory and suggests replacing them with individual file
   deployments

## Build

```bash
cd examples/standalone-validator
mvn package
```

## Usage

```bash
# Basic validation
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar my.build

# Multiple files
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar ifs1.build ifs2.build

# With custom validator
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar \
    -c ../custom-validator/target/custom-validator-0.0.1-SNAPSHOT.jar \
    my.build

# Fail on unresolved variables
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar \
    --strict-vars my.build

# Fail on warnings (useful in CI)
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar \
    -W my.build

# Pass extra variables
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar \
    -e VARIANT=aarch64le,PREFIX=/proc/boot \
    my.build

# Write report to file
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar \
    -r report.txt my.build

# Quiet mode (only errors/warnings on stderr)
java -jar target/standalone-validator-0.0.1-SNAPSHOT-shaded.jar \
    -q my.build
```

## Exit codes

- 0    | No errors (and no warnings if `-W` is set)
- 1    | Validation errors found, or warnings found with `-W`

## Pipeline

This is the recommended order for buildfile processing in CI:

```
parse → substitute variables → validate → analyse directories → report
```

Variable substitution happens **before** validation so that validators see
the resolved values (e.g. `uid=0` rather than `uid=${DEFAULT_UID}`).
