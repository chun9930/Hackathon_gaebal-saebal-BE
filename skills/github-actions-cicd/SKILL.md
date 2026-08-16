---
name: github-actions-cicd
description: Design and validate a GitHub Actions based CI/CD pipeline for Toggle and Spring Boot services, including build, test, deploy, secret handling, and release safety.
---

# GitHub Actions CI/CD Skill

## Purpose

Design and validate a GitHub Actions based CI/CD pipeline for Spring Boot services.

This skill ensures:
- consistent build and test automation
- safe deployment flow
- release readiness checks
- environment-aware configuration
- migration/deployment verification

---

## When to Use

Use this skill when:
- setting up GitHub Actions for a backend project
- designing CI/CD workflow
- reviewing deployment strategy
- preparing staging/production release flow
- validating build/test/deploy automation
- adding DB migration verification to pipeline

---

## Target Stack

Primary target:
- Spring Boot 3.x
- Java 21
- Gradle
- MySQL
- GitHub Actions
- Docker optional
- cloud VM / container / PaaS deployment possible

---

## Core Principles

### 1. Separate CI and CD
CI and CD should be logically distinct.

- CI = build, test, lint, validate
- CD = deploy after verification

Recommended:
- `ci.yml`
- `cd.yml`

---

### 2. Main Branch Must Be Protected
Production deployment should not trigger from arbitrary branches.

Recommended:
- PR → CI
- merge to `main` → staging/prod deployment flow
- manual approval for production when needed

---

### 3. Fail Fast
The workflow must stop early on:
- test failure
- build failure
- migration validation failure
- missing secrets
- invalid artifact

---

### 4. Artifact Consistency
Build once, deploy the same artifact.

Preferred:
- generate JAR once in CI
- upload artifact
- use same artifact in deploy job

Avoid:
- rebuilding different artifacts in multiple places

---

### 5. Environment Separation
Always distinguish:
- local
- dev
- staging
- prod

Use:
- GitHub Secrets
- GitHub Environments
- environment-specific config injection

---

## Required Pipeline Stages

### CI Stage
Must include:

1. checkout
2. setup JDK
3. cache Gradle dependencies
4. grant gradlew permission if needed
5. build
6. test
7. optional lint/static analysis
8. upload artifact

Example checks:
- `./gradlew clean build`
- `./gradlew test`

---

### CD Stage
Must include:

1. download artifact
2. verify deployment target/secrets
3. backup or safe replacement strategy
4. run deploy step
5. health check
6. rollback guidance or failure handling

---

## Recommended Branch Strategy

### Default
- feature/* → PR
- develop (optional) → integration
- main → release-ready

### MVP Simplicity
If branch model is simple:
- feature/* → PR → main
- main triggers deployment

---

## GitHub Actions Design Rules

### 1. Use Official Actions First
Prefer:
- `actions/checkout`
- `actions/setup-java`
- `actions/upload-artifact`
- `actions/download-artifact`

---

### 2. Use Gradle Cache
Always enable Gradle dependency caching when possible.

---

### 3. Use Concurrency Control
Prevent overlapping deploys.

Example concept:
- one deploy per branch/environment
- cancel older in-progress runs if appropriate

---

### 4. Use Environment Protection
For production:
- require GitHub Environment approval if possible
- isolate secrets by environment

---

## Secrets Rules

Never hardcode:
- DB URL
- DB username/password
- JWT secret
- SSH private key
- server host/user
- cloud credentials

Use:
- GitHub Secrets
- GitHub Environment secrets

Examples:
- `SPRING_PROFILES_ACTIVE`
- `PROD_DB_URL`
- `PROD_DB_USERNAME`
- `PROD_DB_PASSWORD`
- `JWT_SECRET`
- `SSH_PRIVATE_KEY`
- `SERVER_HOST`
- `SERVER_USER`

---

## Deployment Strategy Options

This skill should choose one and explain why.

### Option A. SSH + JAR Restart
Best for:
- simple VM deployment
- MVP
- low infrastructure complexity

Flow:
- build JAR
- copy via SCP/rsync
- restart systemd or nohup process
- run health check

---

### Option B. Docker Image Deployment
Best for:
- consistent runtime
- easier rollback
- container server environments

Flow:
- build Docker image
- push registry
- pull on server
- restart container

---

### Option C. Cloud Platform Deploy
Best for:
- Render / Railway / ECS / Elastic Beanstalk / Cloud Run style workflows

Flow depends on platform.

---

## Recommended Default for MVP

For a Spring Boot student/team project using GitHub Actions:

### Recommended
- CI: GitHub Actions
- CD: GitHub Actions + SSH deploy to server
- artifact: bootJar
- process management: systemd or controlled restart script

This is usually the best balance of:
- simplicity
- control
- learning value
- production similarity

---

## DB Migration Verification Rules

If using Flyway or Liquibase, CI/CD must validate migration safety.

### Required Checks
- migration files exist in version order
- application starts with migration enabled in safe environment
- migration syntax is valid
- no destructive migration without explicit review

### Preferred
- run migration validation in CI using test DB
- run deploy-time migration only after backup/checkpoint strategy

---

## Health Check Rules

After deploy, always validate:

- app process started
- port is open
- `/actuator/health` responds successfully
- critical profile/env vars loaded

Deployment is not complete until health check passes.

---

## Rollback Rules

This skill must always provide rollback guidance.

At minimum include:
- previous artifact retention
- previous release path/version
- manual rollback command or script
- what to do if health check fails

---

## Required Output Format

When generating a CI/CD design, always return:

1. **Pipeline Summary**
   - what CI does
   - what CD does
   - trigger strategy

2. **Recommended Workflow Files**
   - filenames
   - responsibility of each file

3. **Secrets List**
   - required GitHub secrets/env vars

4. **Deployment Flow**
   - step-by-step deploy lifecycle

5. **Verification Checklist**
   - build/test/deploy/health check items

6. **Rollback Plan**
   - exact rollback approach

7. **Workflow YAML**
   - production-ready GitHub Actions files

---

## Minimum Recommended Files

### Required
- `.github/workflows/ci.yml`
- `.github/workflows/cd.yml`

### Optional
- `deploy.sh`
- `health-check.sh`
- `rollback.sh`

---

## CI Review Checklist

- [ ] workflow triggers on PR and push appropriately
- [ ] Java version matches project
- [ ] Gradle wrapper used
- [ ] test stage exists
- [ ] build artifact uploaded
- [ ] secrets not exposed in logs

---

## CD Review Checklist

- [ ] deploy only from allowed branch/event
- [ ] concurrency configured
- [ ] environment secrets used
- [ ] artifact reused from CI
- [ ] health check exists
- [ ] rollback path exists

---

## Anti-Patterns

❌ Deploy on every random branch  
❌ Build separately in multiple stages with inconsistent outputs  
❌ Hardcode secrets in YAML  
❌ Deploy without health check  
❌ Deploy without rollback plan  
❌ Ignore DB migration verification  
❌ Restart blindly without confirming app readiness  

---

## Default Recommendation for Spring Boot + GitHub Actions

Use this default unless project constraints say otherwise:

- CI on PR and push
- build/test with Java 21 + Gradle
- upload JAR artifact
- CD on merge to main
- deploy over SSH to target server
- restart app safely
- verify `/actuator/health`
- keep rollback-ready previous artifact

---

## Workflow Integration

This skill must be used:

1. AFTER backend structure is stable
2. BEFORE release setup
3. BEFORE production deployment
4. TO review release readiness

---

## For Codex / Antigravity

When this skill is invoked:
- prefer practical GitHub Actions setup
- optimize for Spring Boot + Gradle
- include artifact reuse
- include secret design
- include migration validation
- include health check and rollback
- return production-usable YAML
