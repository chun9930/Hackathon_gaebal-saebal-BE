---
name: write-prd
description: Write strict, implementation-ready PRDs for Toggle. Enforces MVP scope control, role-based design, and real-time store status domain.
---

# Write PRD (Toggle)

## 🚨 MANDATORY RULES (NON-NEGOTIABLE)

You MUST follow all rules below:

1. ALWAYS start from a real user problem
2. ALWAYS define MVP scope clearly
3. ALWAYS separate roles:
   - Guest
   - Member
   - Owner
   - Admin
4. ALWAYS include:
   - user scenarios
   - functional requirements
   - edge cases
5. ALWAYS reflect Toggle core domain:
   - real-time store status
   - map-based exploration
   - decision speed
6. NEVER start from implementation
7. NEVER write vague statements like:
   - "improve UX"
   - "optimize performance"
8. NEVER expand scope unnecessarily

---

## 🧠 CORE PRODUCT CONTEXT (FIXED)

### Product
Toggle is a map-based web app that helps users avoid wasted trips by showing real-time store availability.

### Core Value
- Avoid wasted visits
- Trust real-time status
- Enable fast decisions

---

## 👥 ROLES (MANDATORY)

You MUST clearly define which roles are involved:

- Guest → browsing only
- Member → favorites, personal maps
- Owner → store status control
- Admin → moderation

---

## 📦 OUTPUT FORMAT (STRICT)

You MUST output in this exact structure:

# PRD: {Feature Name}

## 1. Overview
- what this feature is
- why it matters
- target role

## 2. Problem Statement
- current user pain
- why existing solution fails

## 3. Goal
- what user can do after this feature

## 4. Non-Goals
- explicitly excluded scope

## 5. Target Users
- Guest / Member / Owner / Admin

## 6. User Scenarios

### Scenario 1
- realistic situation

### Scenario 2
- realistic situation

---

## 7. Functional Requirements

1. ...
2. ...
3. ...

Requirements MUST be:
- testable
- specific
- unambiguous

---

## 8. UX / Interaction Notes

- entry point
- key interactions
- empty states
- login prompts if needed

---

## 9. Domain Considerations

- entities involved
- important rules
- state transitions (if any)

---

## 10. Backend Considerations

- required APIs (high-level)
- read vs write separation
- permission boundaries

---

## 11. Edge Cases

- permission failures
- empty results
- invalid inputs
- stale state
- deleted resources

---

## 12. Success Metrics

- measurable product outcome
- e.g. reduced failed visits, increased usage

---

## 13. Open Questions

- unresolved decisions

---

## 🧩 TOGGLE-SPECIFIC RULES

### 1. Map Features

MUST focus on:
- fast decision making
- visible status
- minimal friction

---

### 2. Favorites / Maps

MUST include:
- login boundary
- ownership
- visibility (public/private)

---

### 3. Owner Features

MUST include:
- fast status update
- correctness of status
- allowed transitions

---

### 4. Admin Features

MUST include:
- moderation flow
- access control
- auditability

---

## 🎯 QUALITY BAR

The PRD MUST be:

- scoped (MVP-ready)
- realistic to implement
- role-aware
- domain-consistent
- useful for backend + frontend + QA

---

## ❌ DO NOT

- write generic startup PRDs
- include unrelated features (e.g. POS, congestion data)
- create large, undefined scope
- skip role definitions
- skip edge cases

---

## ✅ EXAMPLES

- "Write PRD for owner status update"
- "Define MVP for favorites feature"
- "Create PRD for public map sharing"
- "Write PRD for guest browsing flow"

---

## FINAL INSTRUCTION

Write PRDs like a product manager preparing for immediate development.

Focus on:
- clarity
- scope control
- real user behavior
- implementation readiness