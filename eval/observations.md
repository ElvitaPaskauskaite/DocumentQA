# Evaluation observations

Problems I noticed while testing DocumentQA by hand. Each one shows a way a
question-answering system can go wrong, and each will become a test case
when I measure the system properly.

These are not bugs in my code. The code did what it was written to do, but
the result still wasn't good enough. When I fix one, I move it to
`DEBUGGING.md` with the fix.

## Summary

| # | Problem | Found the right text? | Answer correct? |
|---|---|---|---|
| 1 | Answer mixed in an unrelated sentence | Yes | Partly |
| 2 | Same question, different wording each run | Yes | Yes |
| 3 | Model compared amounts wrongly | Yes | No |
| 4 | Unanswerable question still found "relevant" text | No (nothing to find) | Yes, it refused |

The main lesson: **finding the right text and giving the right answer are
two separate steps, and either can fail.** So I measure them separately.

---

## 1. The answer mixed in an unrelated sentence
- **Question:** "What is inheritance?" (asked against my Java notes)
- **Found:** the correct paragraph, `java_notes.txt#4` (score 0.775)
- **Answer:** correct, but it added "serves as a blueprint", which came from
  a different paragraph (`#7`, about classes).
- **Why:** the language model sees all 3 retrieved paragraphs and doesn't
  always stick to the most relevant one.
- **Ideas to test:** retrieve fewer paragraphs; tell the model to use only
  the paragraph that answers the question.

## 2. Same question, different wording each run
- **What happened:** the search scores were identical every run, but the
  answer was worded differently each time.
- **Why:** search is deterministic (same text gives the same numbers), but
  the language model picks words with some randomness. This is controlled
  by a setting called temperature, which isn't 0 by default.
- **Ideas to test:** set temperature to 0 so the same question gives the
  same answer. That makes test results repeatable.

## 3. The model compared amounts wrongly (20 Nov 2026)
- **Question:** "How many quotes do I need for a €4,000 laptop?"
- **Correct answer:** three quotes (purchases from €1,000 to under €10,000).
- **Found:**
  - `procurement_policy.txt#4` 0.745: €1,000 to under €10,000, three quotes (correct)
  - `procurement_policy.txt#3` 0.661: under €1,000, one quote
  - `procurement_policy.txt#5` 0.569: €10,000 or more, tender
- **Answer:** "one quote" in all 3 runs. One run said €4,000 "falls under
  the category of purchases under €1,000".
- **With citations turned on (20 Nov 2026), 2 runs:**
  - Run 1: "one quote", cited `[procurement_policy.txt#3]`, so the citation shows which paragraph the wrong answer came from.
  - Run 2: "two quotes" (a number not in any document) and **no citation at all**, despite the prompt asking for one. The output showed "Sources: none cited", which flags the answer as ungrounded.
- **Lesson:** citations don't make the model correct, but they make a wrong or invented answer visible. A factual answer with no citation should not be trusted.
- **Why:** the search worked, since the right paragraph came first. The
  model (llama3.2, a small model) then got the comparison wrong. The
  wording changed between runs but the mistake didn't, so randomness isn't
  the cause, and setting temperature to 0 won't fix it.
- **Ideas to test:** tell the model to check the amount against each range;
  retrieve fewer paragraphs; try a larger model.

## 4. An unanswerable question still found "relevant" text (20 Nov 2026)
- **Question:** "What is the parental leave policy?" (no document covers this)
- **Found:** all 3 passed the minimum score of 0.5:
  - `annual_leave_policy.txt#2` 0.600: the policy's title paragraph (owner, version)
  - `annual_leave_policy.txt#13` 0.589: "Questions about the Annual Leave Policy..."
  - `annual_leave_policy.txt#7` 0.545: carrying over leave
- **Answer:** "I don't know based on the documents." Correct: the model
  refused.
- **Why:** the title and contact paragraphs repeat the words "Leave Policy"
  but contain no rules, so they match any "... leave policy?" question on
  wording alone. 0.600 is higher than a correct paragraph scored in #3
  (0.569), so raising the minimum score can't separate answerable from
  unanswerable questions. This time the model's instruction to refuse was
  the safety net.
- **Ideas to test:** don't index title and contact paragraphs; check what
  scores all my unanswerable questions get before choosing a minimum score.