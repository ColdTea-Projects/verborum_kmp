---
name: write-a-skill
description: Creates new agent skills with proper structure, progressive disclosure, and bundled resources, then validates them against the six-item review checklist. Use when the user wants to create, write, build, author, split or audit a skill in .claude/skills.
license: MIT
metadata:
  derived_from: "https://github.com/alirezarezvani/claude-skills/tree/main/engineering/write-a-skill"
  original_author: "Matt Pocock (@mattpocock)"
  original_license: MIT
  voice: "Matt Pocock — direct, concrete, imperative, example-driven"
  version: 1.0.0
---

# Writing Skills

Attribution and what this copy changed: [README.md](README.md).

## Process

1. **Gather requirements** - ask user about:
   - What task/domain does the skill cover?
   - What specific use cases should it handle?
   - Does it need executable scripts or just instructions?
   - Any reference materials to include?

2. **Draft the skill** - create:
   - SKILL.md with concise instructions
   - Additional reference files if content exceeds 500 lines
   - Utility scripts if deterministic operations needed

3. **Review with user** - present draft and ask:
   - Does this cover your use cases?
   - Anything missing or unclear?
   - Should any section be more/less detailed?

## Skill Structure

```
skill-name/
├── SKILL.md           # Main instructions (required)
├── references/        # Detailed docs, one level deep (if needed)
│   └── topic.md
└── scripts/           # Utility scripts (if needed)
    └── helper.py
```

A SKILL.md is: frontmatter (`name`, `description`), a `## Quick start` holding a minimal working
example, `## Workflows` for step-by-step processes, and pointers into `references/` for anything
advanced.

## Description Requirements

The description is **the only thing your agent sees** when deciding which skill to load. It is
surfaced in the system prompt alongside every other installed skill.

- Max 1024 chars, third person
- First sentence: what it does, opening with an action verb
- Second sentence: "Use when [specific triggers]"

**Good**: `Extract text and tables from PDF files, fill forms, merge documents. Use when working
with PDF files or when user mentions PDFs, forms, or document extraction.`

**Bad**: `Helps with documents.` — no verb, no trigger, indistinguishable from every other document
skill.

Vocabulary choices, trigger patterns and anti-patterns:
[references/description_design_patterns.md](references/description_design_patterns.md).

## When to Add Scripts

Add utility scripts when the operation is deterministic (validation, formatting), when the same code
would be generated repeatedly, or when errors need explicit handling. Scripts save tokens and
improve reliability versus generated code.

## When to Split Files

Split into separate files when SKILL.md exceeds 100 lines, when content has distinct domains, or
when advanced features are rarely needed. How to choose the split and keep it one level deep:
[references/progressive_disclosure_principles.md](references/progressive_disclosure_principles.md).

## Review Checklist

Run it, do not eyeball it:

```bash
python3 scripts/skill_review_checklist_runner.py path/to/skill-folder
python3 scripts/skill_description_validator.py path/to/skill-folder/SKILL.md
python3 scripts/skill_structure_validator.py path/to/skill-folder
```

The six items and why each is binding:
[references/quality_gates_for_skills.md](references/quality_gates_for_skills.md). What each script
checks: [references/companion_tooling.md](references/companion_tooling.md).
