import { apiDelete, apiPostMultipart } from "./client";
import type { ResumeSourceResponse } from "./types";

// §21 "Upload Resume" ("Get Parsed Resume" exists too, but nothing in the UI
// re-fetches an upload by id - the upload response already has everything
// UploadResumePage needs).
export function uploadResume(file: File): Promise<ResumeSourceResponse> {
  const formData = new FormData();
  formData.append("file", file);
  return apiPostMultipart<ResumeSourceResponse>("/resumes/upload", formData);
}

// Deletes the resume_source and every knowledge fragment created from it
// (see ingestion.ResumeUploadService.delete()) - the way to undo a duplicate
// or bad upload without going to raw SQL.
export function deleteResume(resumeId: string): Promise<void> {
  return apiDelete(`/resumes/${resumeId}`);
}
