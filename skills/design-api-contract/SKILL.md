---
name: design-api-contract
description: Design strict, implementation-ready API contracts for Toggle. Enforces role separation, real-time store status domain, and DTO-based contracts. Use for all backend API design tasks.
---

# Design API Contract (Toggle)

## 🚨 MANDATORY RULES (NON-NEGOTIABLE)

You MUST follow all rules below:

1. ALWAYS separate roles:
   - Guest (public read)
   - Member (user features)
   - Owner (store control)
   - Admin (moderation)

2. NEVER mix roles in one endpoint

3. ALWAYS define:
   - request DTO
   - response DTO
   - error cases

4. ALWAYS include:
   - permission rules
   - edge cases
   - state transition rules (if applicable)

5. NEVER expose entity directly

6. ALWAYS reflect Toggle core domain:
   - map-based browsing
   - real-time store status
   - role-based access

7. If unclear → ASK for clarification instead of guessing

---

## 🧠 CORE DOMAIN (FIXED CONTEXT)

### Store Status Enum (DO NOT CHANGE)

```text
OPEN
BREAK_TIME
CLOSED
TEMP_CLOSED
EARLY_CLOSED
