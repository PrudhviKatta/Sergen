import { useRef, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Stack,
  Typography,
} from "@mui/material";
import { deleteResume, uploadResume } from "../api/resumes";
import { ApiRequestError } from "../api/client";
import type { IngestionStatus, ResumeSourceResponse } from "../api/types";

const STATUS_COLOR: Record<IngestionStatus, "default" | "success" | "error"> = {
  PENDING: "default",
  PARSED: "success",
  FAILED: "error",
};

const ACCEPTED_EXTENSIONS = ".pdf,.docx,.txt";

// §31 Screen 1 "Knowledge Base": upload + view parsing result. "Edit parsed
// projects" / "Confirm technologies" (turning this into real candidate_project
// rows) aren't built - ingestion.ResumeUploadService deliberately stops at
// creating knowledge fragments, see docs/IMPLEMENTATION_NOTES.md.
export default function UploadResumePage() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ResumeSourceResponse | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleted, setDeleted] = useState(false);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    setResult(null);
    setError(null);
    setDeleted(false);
    setUploading(true);
    try {
      const response = await uploadResume(file);
      setResult(response);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Could not reach the backend.");
    } finally {
      setUploading(false);
      e.target.value = ""; // allow re-selecting the same file
    }
  };

  const handleDelete = async () => {
    if (!result) return;
    if (!window.confirm(`Delete "${result.fileName}" and its knowledge fragments? This can't be undone.`)) {
      return;
    }
    setDeleting(true);
    setError(null);
    try {
      await deleteResume(result.id);
      setResult(null);
      setDeleted(true);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Could not reach the backend.");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Upload Resume</Typography>
      <Typography variant="body2" color="text.secondary">
        Adds a resume to the knowledge base: extracts text, parses it into structured
        project data, and creates searchable knowledge fragments from it. Supported
        formats: PDF, DOCX, plain text.
      </Typography>

      <Box>
        <Button variant="contained" component="label" disabled={uploading}>
          {uploading ? "Uploading..." : "Choose File"}
          <input
            ref={fileInputRef}
            type="file"
            hidden
            accept={ACCEPTED_EXTENSIONS}
            onChange={handleFileChange}
          />
        </Button>
        {fileName && (
          <Typography variant="body2" sx={{ mt: 1 }} color="text.secondary">
            {fileName}
          </Typography>
        )}
        {uploading && <CircularProgress size={20} sx={{ ml: 2 }} />}
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {deleted && <Alert severity="success">Deleted.</Alert>}

      {result && (
        <Card variant="outlined">
          <CardContent>
            <Stack direction="row" spacing={2} sx={{ alignItems: "center", justifyContent: "space-between", mb: 2 }}>
              <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
                <Typography variant="h6">{result.fileName}</Typography>
                <Chip label={result.status} color={STATUS_COLOR[result.status]} size="small" />
              </Stack>
              <Button color="error" size="small" disabled={deleting} onClick={handleDelete}>
                {deleting ? "Deleting..." : "Delete"}
              </Button>
            </Stack>

            {result.status === "FAILED" && (
              <Alert severity="error">{result.failureReason ?? "Parsing failed."}</Alert>
            )}

            {result.parsedJson && (
              <Stack spacing={2}>
                <Box>
                  <Typography variant="subtitle2">Candidate</Typography>
                  <Typography variant="body1">
                    {[result.parsedJson.candidate.firstName, result.parsedJson.candidate.lastName]
                      .filter(Boolean)
                      .join(" ") || "—"}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {[
                      result.parsedJson.candidate.email,
                      result.parsedJson.candidate.phone,
                      result.parsedJson.candidate.location,
                    ]
                      .filter(Boolean)
                      .join(" · ")}
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    {result.parsedJson.candidate.primaryRole ?? "—"}
                    {result.parsedJson.candidate.totalExperienceYears !== null
                      ? ` · ${result.parsedJson.candidate.totalExperienceYears} years`
                      : ""}
                  </Typography>
                  {result.parsedJson.candidate.summary && (
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      {result.parsedJson.candidate.summary}
                    </Typography>
                  )}
                </Box>

                {result.parsedJson.candidate.technicalSkills.length > 0 && (
                  <Box>
                    <Typography variant="subtitle2">Technical Skills</Typography>
                    <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap", gap: 1, mt: 1 }}>
                      {result.parsedJson.candidate.technicalSkills.map((skill) => (
                        <Chip key={skill} label={skill} size="small" />
                      ))}
                    </Stack>
                  </Box>
                )}

                {result.parsedJson.candidate.education.length > 0 && (
                  <Box>
                    <Typography variant="subtitle2">Education</Typography>
                    {result.parsedJson.candidate.education.map((entry, i) => (
                      <Typography key={i} variant="body2">
                        {entry}
                      </Typography>
                    ))}
                  </Box>
                )}

                {result.parsedJson.candidate.certifications.length > 0 && (
                  <Box>
                    <Typography variant="subtitle2">Certifications</Typography>
                    {result.parsedJson.candidate.certifications.map((entry, i) => (
                      <Typography key={i} variant="body2">
                        {entry}
                      </Typography>
                    ))}
                  </Box>
                )}

                {result.parsedJson.projects.map((project, i) => (
                  <Card key={i} variant="outlined" sx={{ bgcolor: "action.hover" }}>
                    <CardContent>
                      <Typography variant="subtitle2">
                        {project.client ?? "Unknown client"} — {project.role ?? "—"}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {project.startDate ?? "?"} to {project.endDate ?? "?"}
                        {project.domain ? ` · ${project.domain}` : ""}
                      </Typography>
                      {project.responsibilities.length > 0 && (
                        <Box component="ul" sx={{ mt: 1, mb: 1, pl: 3 }}>
                          {project.responsibilities.map((r, j) => (
                            <li key={j}>
                              <Typography variant="body2">{r}</Typography>
                            </li>
                          ))}
                        </Box>
                      )}
                      {project.technologies.length > 0 && (
                        <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap", gap: 1 }}>
                          {project.technologies.map((tech) => (
                            <Chip key={tech} label={tech} size="small" variant="outlined" />
                          ))}
                        </Stack>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </Stack>
            )}
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
