/**
 * Timeline/chronology/technology/content-safety/tone validators (§29).
 *
 * Built so far (Milestone 4):
 *   - {@link com.company.resumeai.validation.ChronologyValidator} - start &lt;= end date
 *   - {@link com.company.resumeai.validation.TechnologyTimelineValidator} - §14 timeline
 *     engine: is a technology era-appropriate for a project's date range, and if not,
 *     what should replace it (§40 era profiles)
 *
 * Not yet built: client-name alias validation, content-safety validation
 * (fabricated degrees/awards/etc, §29), and the LLM-as-judge tone/quality
 * validator (§29 "Tone & Quality Validation") - those land with Milestone 5/6.
 */
package com.company.resumeai.validation;
