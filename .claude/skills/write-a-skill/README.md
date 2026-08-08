# write-a-skill — attribution and local changes

Vendored from
[alirezarezvani/claude-skills · engineering/write-a-skill](https://github.com/alirezarezvani/claude-skills/tree/main/engineering/write-a-skill/skills/write-a-skill)
(MIT), which is itself derived from
[Matt Pocock's write-a-skill](https://github.com/mattpocock/skills/tree/main/skills/productivity/write-a-skill)
(MIT, © Matt Pocock).

**Preserved verbatim:** the three-phase process, the skill-structure and SKILL.md templates, the
description rules and good/bad examples, the "when to add scripts" and "when to split files"
criteria, the six-item review checklist, all four `references/` files, and all three `scripts/`.

**Changed for this repo:**

- The upstream SKILL.md ran to 141 lines and failed its own 100-line gate. The derivation banner
  moved here, and the longer explanations were replaced with pointers into the `references/` files
  they already duplicated.
- The upstream `references/companion_tooling.md` linked back to `../SKILL.md`, which the structure
  validator reports as a circular reference. That link is gone.
- The upstream skill shipped alongside a `cs-skill-author` agent and a `/cs:write-a-skill` slash
  command. Neither is installed here, so references to them are marked as not present.
- `python` in the run commands became `python3` (macOS has no bare `python`).

All three validators pass on this copy.

## Applied to this repo

Every skill in `.claude/skills/` was restructured against this standard: descriptions rewritten to
an action verb plus a "Use when …" trigger, each `SKILL.md` cut below 100 lines with the overflow
moved into a flat `references/` folder, and each one verified with
`scripts/skill_review_checklist_runner.py`.
