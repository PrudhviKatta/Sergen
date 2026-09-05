// Mirrors the backend DTOs in com.company.resumeai.generation (Milestone 5/6).
// Keep in sync by hand - there's no shared schema/codegen between the two yet.

export type GenerationStatus = "PENDING" | "COMPLETED" | "FAILED";

export type SimilarityVerdict = "ACCEPTABLE" | "REVIEW" | "REWRITE";

export interface ProjectGenerationInput {
  client: string;
  startDate: string; // ISO yyyy-MM-dd
  endDate: string; // ISO yyyy-MM-dd
  role?: string;
  domain?: string;
  knownTechnologies?: string[];
}

export interface ResumeGenerationRequest {
  candidateName: string;
  primaryRole: string;
  totalExperienceYears?: number;
  projects: ProjectGenerationInput[];
}

export interface GeneratedProjectResponse {
  id: string;
  clientName: string;
  roleTitle: string | null;
  startDate: string;
  endDate: string;
  domain: string | null;
  description: string | null;
  responsibilities: string[];
  environment: string[];
  promptVersion: string | null;
  similarityScore: number | null;
  similarityVerdict: SimilarityVerdict | null;
  duplicatePhraseDetected: boolean;
  rewriteAttempts: number;
}

export interface ResumeGenerationResponse {
  id: string;
  candidateName: string;
  primaryRole: string;
  totalExperienceYears: number | null;
  status: GenerationStatus;
  generatedSummary: string | null;
  promptVersion: string | null;
  model: string | null;
  failureReason: string | null;
  createdAt: string;
  projects: GeneratedProjectResponse[];
}

// Mirrors com.company.resumeai.ingestion / parser (Milestone 2).

export type IngestionStatus = "PENDING" | "PARSED" | "FAILED";

export interface ParsedCandidate {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  location: string | null;
  primaryRole: string | null;
  totalExperienceYears: number | null;
  summary: string | null;
  technicalSkills: string[];
  education: string[];
  certifications: string[];
}

export interface ParsedProject {
  client: string | null;
  role: string | null;
  startDate: string | null;
  endDate: string | null;
  domain: string | null;
  technologies: string[];
  responsibilities: string[];
}

export interface ParsedResume {
  candidate: ParsedCandidate;
  projects: ParsedProject[];
}

export interface ResumeSourceResponse {
  id: string;
  candidateId: string | null;
  fileName: string;
  fileType: string;
  rawText: string | null;
  parsedJson: ParsedResume | null;
  status: IngestionStatus;
  failureReason: string | null;
  createdAt: string;
}

// Matches common.web.ApiError - what GlobalExceptionHandler returns for 4xx/5xx.
export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: { field: string; message: string }[];
}
