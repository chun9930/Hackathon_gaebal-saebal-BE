# Backend Entity Design Skill (JPA Optimized)

## Purpose

Convert ERD into production-ready JPA Entities for Spring Boot.

This skill ensures:
- correct entity relationships
- optimal JPA performance
- clean domain modeling
- compatibility with Spring Boot 3.x (Hibernate 6+)

---

## When to Use

Use this skill when:
- ERD is finalized
- starting domain/entity layer
- designing JPA relationships
- optimizing performance (N+1, cascade, fetch)

---

## Core Principles

### 1. Entity = Domain Model

- Entity is NOT a DB table wrapper
- It represents domain behavior

---

### 2. Default = LAZY Loading

```java
@ManyToOne(fetch = FetchType.LAZY)
@OneToMany(fetch = FetchType.LAZY)