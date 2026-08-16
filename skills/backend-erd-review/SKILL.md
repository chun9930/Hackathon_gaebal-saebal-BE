---
name: backend-erd-review
description: Review and validate Toggle ERD and DBML schemas for Spring Boot + MySQL MVP services, covering requirements, normalization, constraints, indexes, and schema risks.
---

# Backend ERD Review Skill

## Purpose

Review and validate an ERD for a Spring Boot + MySQL backend before entity implementation.

This skill ensures:
- the schema matches product requirements
- normalization is appropriate
- relationships are correct
- constraints and indexes are not missing
- risky design choices are identified early

---

## When to Use

Use this skill when:
- an ERD has already been drafted
- reviewing DBML / SQL schema / table design
- validating before JPA entity design
- checking MVP schema quality
- reviewing Codex/Antigravity generated ERD output

---

## Review Scope

This skill reviews:

1. requirement coverage
2. table responsibility
3. normalization level
4. PK/FK structure
5. N:M resolution
6. current-state vs history separation
7. nullable risk
8. uniqueness constraints
9. indexing strategy
10. enum/status design
11. report / polymorphic relation risk
12. future extensibility

---

## Core Principles

### 1. Review Against Requirements First
Do not review tables in isolation.

Always ask:
- does this schema cover the product requirements?
- is any required feature impossible with this design?
- is any table over-designed for MVP?

---

### 2. One Table, One Responsibility
Reject tables that combine unrelated concerns.

❌ Bad:
- store + owner settings + status log in one table

✅ Good:
- stores
- store_status_logs
- owner settings separated when needed

---

### 3. Resolve N:M Explicitly
Any many-to-many relationship must be resolved with a join table.

❌ Bad:
- arrays
- JSON lists
- comma-separated ids

✅ Good:
- map_stores
- favorite tables
- relation entities

---

### 4. Separate Current State from History
Do not mix audit/history data into the main table when history matters.

Example:
- `stores.business_status` = current status
- `store_status_logs` = status history

---

### 5. Validate for MVP, Not Fantasy
Review whether the schema is:
- sufficient for MVP
- not prematurely over-engineered
- extensible enough for next iteration

---

## Mandatory Review Checklist

### A. Requirement Coverage
- [ ] all MVP features are supported
- [ ] no critical feature is blocked by schema design
- [ ] non-member vs member vs owner vs admin concerns are reflected correctly

### B. Table Design
- [ ] each table has a clear responsibility
- [ ] no duplicate semantic tables
- [ ] names are consistent and meaningful

### C. Relationship Design
- [ ] every FK is appropriate
- [ ] every N:M is resolved by a join table
- [ ] ownership direction is clear

### D. Constraint Design
- [ ] PK exists on every table
- [ ] required UNIQUE constraints exist
- [ ] nullable fields are intentional
- [ ] required business rules can be enforced

### E. Performance Design
- [ ] FK indexes are considered
- [ ] frequent filter/sort columns are considered
- [ ] current-state read paths are efficient

### F. Evolution Risk
- [ ] polymorphic references are acceptable for MVP
- [ ] enum/string fields are manageable
- [ ] table split suggestions are made when needed

---

## Required Constraint Review Rules

Always check whether these are needed:

- unique email on users
- unique composite on relation tables
- unique per-user category names
- required FK not null
- current status fields not null

Examples:
- `(user_id, store_id)` unique for favorites
- `(map_id, store_id)` unique for map_stores
- `(user_id, name)` unique for saved_place_categories

---

## Required Index Review Rules

Always suggest indexes for:
- all FK columns
- frequent filters
- sort columns used by list APIs
- status columns used in map/list searches
- visibility/public lookup fields

Examples:
- `stores.owner_id`
- `stores.category_id`
- `stores.business_status`
- `favorites.user_id`
- `saved_places.user_id`
- `map_stores.map_id`
- `maps.user_id`
- `maps.visibility`
- `store_status_logs.store_id`
- `store_status_logs.changed_at`

---

## Review Rules for Enum / Status Fields

For MVP:
- `varchar` is acceptable when enums may evolve quickly

For stable production:
- application enum + constrained DB values should be considered

Always check:
- are status names clear?
- are there overlapping statuses?
- can one field represent too many concepts?

---

## Review Rules for Polymorphic Relations

If schema uses:
- `target_type`
- `target_id`

Then review:

### Acceptable for MVP if:
- target domains are few
- application-level validation is easy
- admin/report tooling is simple

### Risky if:
- many target types will grow
- strong FK integrity is needed
- reporting/analytics must be strict

Alternative:
- split tables such as:
  - `store_reports`
  - `map_reports`

---

## Output Format

When reviewing, always return:

1. **Review Summary**
   - overall evaluation
   - whether schema is acceptable for MVP

2. **Critical Fixes**
   - must fix before implementation

3. **Optional Improvements**
   - useful but not blocking

4. **Constraint / Index Suggestions**
   - concrete additions

5. **Improved Schema**
   - revised DBML or SQL if needed

---

## Severity Rules

### Critical
Blocks implementation or causes data integrity problems.

Examples:
- unresolved N:M
- missing unique constraint on relation table
- wrong FK direction
- missing table for required feature

### Warning
Not blocking, but risky.

Examples:
- too many nullable columns
- weak naming
- missing helpful indexes
- polymorphic design with future growth risk

### Optional
Good improvement, not urgent.

Examples:
- separate enum table
- add audit fields
- improve naming consistency

---

## Anti-Patterns

❌ Review only by syntax  
❌ Ignore product requirements  
❌ Accept arrays/JSON for core relations  
❌ Ignore missing indexes  
❌ Ignore nullable abuse  
❌ Approve over-engineered schemas for MVP  
❌ Force perfect enterprise design when MVP simplicity is better  

---

## Example Review Prompt Pattern

Use this structure:

- check requirement coverage
- validate normalization
- validate relationships
- identify missing constraints
- identify missing indexes
- evaluate polymorphic references
- propose improved schema

---

## Workflow Integration

This skill must be used:

1. AFTER product requirements review
2. AFTER ERD generation
3. BEFORE entity design
4. BEFORE migration creation

---

## For Codex / Antigravity

When this skill is invoked:
- be critical but practical
- optimize for MVP + extensibility
- suggest concrete schema fixes
- return improved DBML when useful
