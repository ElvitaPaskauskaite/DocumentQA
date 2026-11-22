# Evaluation results

Each run asks the 20 questions in `questions.json` (15 answerable, 5 unanswerable) and scores every answer with `AnswerScorer`. Every "yes" for answer facts was also checked by hand. Small fictional set of 3 policies (39 chunks); results only apply to these settings.

## Baseline 

**Settings:** nomic-embed-text, llama3.2 at temperature 0, 39 chunks (max 120 words, overlap 20), top-k 3, min-score 0.5.

| Measure | Result |
|---|---|
| Expected chunk retrieved (top 3) | 15/15 |
| Expected chunk cited | 11/15 |
| Answer contains the expected facts | 10/15 |
| Answerable question wrongly refused | 1/15 |
| Unanswerable question correctly refused | 5/5 |

Two runs gave word-for-word identical answers.

**Failures (all in the answer step, none in retrieval):**
| Question | What went wrong |
|---|---|
| proc-01 (€4,000 laptop) | Said one quote; put €4,000 in the "under €1,000" range |
| proc-02 (exactly €1,000) | Said one quote; €1,000 is in the "€1,000 or more" range |
| proc-04 (€25,000 purchase) | Answered from the travel rule for expenses over €25 |
| leave-02 (carry over leave) | Gave the 5 days but left out the 31 March deadline |
| leave-04 (Finance, 2 January) | Refused, though the quarter-close rule was retrieved |

**Pattern:** retrieval is not the problem. 4 of the 5 failures involve comparing an amount or date against a rule.

**Scoring note:** the first run reported 12/15 for answer facts. Checking by hand showed the fact check was counting the "3" inside the citation label `#3`; fixed before this baseline (see `DEBUGGING.md` #7).

## Experiment 1: prompt line for comparing amounts and dates

**Change:** one prompt line added: "If the question contains an amount or a date, compare it with the limits in each paragraph and answer from the paragraph whose range includes it." Everything else as in the baseline.

| Measure | Baseline | With the line |
|---|---|---|
| Expected chunk retrieved (top 3) | 15/15 | 15/15 |
| Expected chunk cited | 11/15 | 12/15 |
| Answer contains the expected facts | 10/15 | 11/15 |
| Answerable question wrongly refused | 1/15 | 1/15 |
| Unanswerable question correctly refused | 5/5 | 5/5 |

**What changed:** proc-04 (€25,000) is now answered from the procurement rule instead of the travel rule for expenses over €25. No previously correct answer got worse.

**What didn't:** proc-01 (€4,000) and proc-02 (exactly €1,000), the questions the line was aimed at, still answer "one quote". leave-02 and leave-04 are unchanged.

**Decision:** kept. A small gain (one question) on a small set, so it is weak evidence; the target failure is not fixed.

## Experiment 2: only the top paragraph (top-k 1)

**Change:** top-k 3 → 1, on top of Experiment 1. The idea: proc-01 and proc-02 had the right paragraph first but answered from the second one.

| Measure | Experiment 1 (top-k 3) | Top-k 1 |
|---|---|---|
| Expected chunk retrieved | 15/15 (top 3) | 12/15 (top 1) |
| Expected chunk cited | 12/15 | 10/15 |
| Answer contains the expected facts | 11/15 | 8/15 |
| Answerable question wrongly refused | 1/15 | 3/15 |
| Unanswerable question correctly refused | 5/5 | 5/5 |

**What changed:** proc-02 (exactly €1,000) is now correct. proc-01 (€4,000) cited the right paragraph but said "two quotes", a number not in it, so the model misreads the rule even without the distracting paragraph. proc-03, leave-01 and leave-03 lost their right paragraph (ranked 2nd or 3rd); leave-01 invented "26 days". proc-05 was refused.

**Also measured:** the right paragraph is ranked first for 12/15 questions and in the top 3 for 15/15.

**Decision:** rejected, top-k back to 3. Fewer paragraphs removes the distraction but loses too many right answers.

**Note:** experiments are run one at a time on top of the best setup so far, so combinations of changes are not tested.

## Experiment 3: larger chat model (llama3.1:8b)

**Change:** chat model llama3.2 → llama3.1:8b, on top of Experiment 1. Only a setting changed; no code, because the model sits behind the `ChatModel` interface.

| Measure | Experiment 1 (llama3.2) | llama3.1:8b |
|---|---|---|
| Expected chunk retrieved (top 3) | 15/15 | 15/15 |
| Expected chunk cited | 12/15 | 13/15 |
| Answer contains the expected facts (scorer) | 11/15 | 12/15 |
| Answer correct, checked by hand | 11/15 | 11/15 |
| Answerable question wrongly refused | 1/15 | 1/15 |
| Unanswerable question correctly refused | 5/5 | 5/5 |

**What changed:** proc-01 (€4,000 laptop), wrong in every earlier run, is now correct. proc-05 broke: it said an €80 gift *can* be accepted ("€30 less than the allowed €50"). The scorer counted that as correct because "50" appears in it; the hand check caught it.

**Decision:** rejected, back to llama3.2. Same result by hand, a much larger and slower model, and its new mistake is a confident wrong "yes".

## Not tested

- **Leaving header and contact paragraphs out of the index, and tuning min-score.** Refusals are already 5/5 and retrieval 15/15 on this question set, so neither change could show a measurable gain. They would need more unanswerable questions that currently slip through.