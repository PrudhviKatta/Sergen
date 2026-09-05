import { apiGet, apiPost } from "./client";
import type { ResumeGenerationRequest, ResumeGenerationResponse } from "./types";

// §21 "Generate Base Resume" / "Get Generation" - the only two endpoints
// generation.ResumeGenerationController exposes so far. Regenerate/approve
// (also §21) aren't implemented on the backend yet - see
// docs/IMPLEMENTATION_NOTES.md's Milestone 5/6 sections - so there's no
// client function for them here either.

export function createResumeGeneration(
  request: ResumeGenerationRequest,
): Promise<ResumeGenerationResponse> {
  return apiPost<ResumeGenerationResponse>("/resume-generations", request);
}

export function getResumeGeneration(id: string): Promise<ResumeGenerationResponse> {
  return apiGet<ResumeGenerationResponse>(`/resume-generations/${id}`);
}
