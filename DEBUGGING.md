# Debugging log

Each entry: symptom → cause → fix → lesson. Added when the bug is fixed.

## 1. Off-topic questions returned chunks with score 0.000
- **Symptom:** "What's the weather in Paris?" still returned the top 3 chunks.
- **Cause:** search always returned top-K, however low the score.
- **Fix:** added a minimum-score filter (now 0.5, in application.properties).
- **Lesson:** retrieval needs a way to say "nothing relevant".

## 2. LLM said "I don't know" when the right chunk was retrieved
- **Symptom:** correct chunk in the results, but the answer was "I don't know".
- **Cause:** printing the prompt showed `PromptBuilder.build()` never appended the question.
- **Fix:** appended `Question: ...` before `Answer:`.
- **Lesson:** print the exact prompt before blaming the model.

## 3. One-word answers ("Inheritance.")
- **Symptom:** answers were a single word.
- **Cause:** the prompt gave no guidance on length.
- **Fix:** instructions now ask for one or two complete sentences.
- **Lesson:** the model only does what the prompt asks.

## 4. Pushed code didn't compile
- **Symptom:** GitHub had code that didn't build.
- **Cause:** getter typo `getSscore`, pushed without checking.
- **Fix:** corrected the typo.
- **Lesson:** check the Problems tab and `git status` before pushing; CI will catch this automatically (Phase 3).

## 5. Vectors overwritten with multiple documents
- **Symptom:** none yet. Spotted before adding a second document: every file numbers its chunks from 1, so two files would produce the same id.
- **Cause:** the map was keyed by position only.
- **Fix:** `TextChunk` carries source name + position; map keyed by `getKey()` (e.g. `git_notes.txt#5`).
- **Lesson:** keys must be unique across the whole system, not just one file.

## 6. Second document silently skipped
- **Symptom:** 2 files in `documents/`, but only 1 loaded.
- **Cause:** Console text had been pasted into the file name, so it didn't end in `.txt` and the filter skipped it.
- **Fix:** renamed the file with `mv`.
- **Lesson:** when something is silently skipped, list the folder and check the real file names.