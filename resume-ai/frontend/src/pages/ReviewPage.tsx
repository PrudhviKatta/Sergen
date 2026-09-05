import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Alert, Box, Chip, CircularProgress, Stack, Typography } from "@mui/material";
import { getResumeGeneration } from "../api/resumeGenerations";
import { ApiRequestError } from "../api/client";
import type { GenerationStatus, ResumeGenerationResponse } from "../api/types";
import GeneratedProjectCard from "../components/GeneratedProjectCard";

const STATUS_COLOR: Record<GenerationStatus, "default" | "success" | "error"> = {
  PENDING: "default",
  COMPLETED: "success",
  FAILED: "error",
};

// §31 Screen 3 "Review": professional summary + one card per project.
export default function ReviewPage() {
  const { id } = useParams<{ id: string }>();
  const [generation, setGeneration] = useState<ResumeGenerationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    getResumeGeneration(id)
      .then(setGeneration)
      .catch((e) =>
        setError(e instanceof ApiRequestError ? e.message : "Could not reach the backend."),
      )
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  if (!generation) {
    return null;
  }

  return (
    <Stack spacing={3}>
      <Box>
        <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
          <Typography variant="h4">{generation.candidateName}</Typography>
          <Chip label={generation.status} color={STATUS_COLOR[generation.status]} />
        </Stack>
        <Typography variant="body1" color="text.secondary">
          {generation.primaryRole}
          {generation.totalExperienceYears !== null ? ` · ${generation.totalExperienceYears} years` : ""}
        </Typography>
      </Box>

      {generation.status === "FAILED" && (
        <Alert severity="error">{generation.failureReason ?? "Generation failed."}</Alert>
      )}

      {generation.generatedSummary && (
        <Box>
          <Typography variant="h6">Professional Summary</Typography>
          <Typography>{generation.generatedSummary}</Typography>
        </Box>
      )}

      <Stack spacing={2}>
        {generation.projects.map((project) => (
          <GeneratedProjectCard key={project.id} project={project} />
        ))}
      </Stack>
    </Stack>
  );
}
