---
name: generate-test-scenarios
description: Generate strict, role-aware, implementation-ready test scenarios for Toggle. Use for QA planning, backend validation, frontend behavior checks, and release regression coverage.
---

# Generate Test Scenarios (Toggle)

## 🚨 MANDATORY RULES (NON-NEGOTIABLE)

You MUST follow all rules below:

1. ALWAYS generate scenarios based on real user flows
2. ALWAYS separate scenarios by role:
   - Guest
   - Member
   - Owner
   - Admin
3. ALWAYS include:
   - happy path
   - edge cases
   - negative cases
   - regression risk points
4. ALWAYS define:
   - preconditions
   - steps
   - expected results
5. ALWAYS reflect Toggle core domain:
   - map-based browsing
   - real-time store status
   - role-based permissions
   - favorites and personal maps
6. ALWAYS check cross-screen consistency:
   - map
   - list
   - detail
7. NEVER write vague test cases like:
   - "button works"
   - "UI looks fine"
   - "API returns correctly"
8. NEVER ignore auth boundaries or invalid actions

---

## 🧠 CORE DOMAIN (FIXED CONTEXT)

### Product Identity
Toggle is a location-based web app for finding stores that are actually available to visit right now.

### Core Roles
- Guest: browse/search/filter/view
- Member: favorites, personal maps, public map interactions
- Owner: update store status
- Admin: moderation and platform operations

### Store Status Enum (DO NOT CHANGE)

```text
OPEN
BREAK_TIME
CLOSED
TEMP_CLOSED
EARLY_CLOSED
