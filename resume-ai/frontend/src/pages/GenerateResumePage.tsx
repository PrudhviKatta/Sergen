import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import { createResumeGeneration } from "../api/resumeGenerations";
import { ApiRequestError } from "../api/client";
import type { ProjectGenerationInput, ResumeGenerationRequest } from "../api/types";
import ProjectInputRow, {
  emptyProjectFormValue,
  type ProjectFormValue,
} from "../components/ProjectInputRow";

// §31 Screen 2 "Generate Resume".
export default function GenerateResumePage() {
  const navigate = useNavigate();

  const [candidateName, setCandidateName] = useState("");
  const [primaryRole, setPrimaryRole] = useState("");
  const [totalExperienceYears, setTotalExperienceYears] = useState("");
  const [projects, setProjects] = useState<ProjectFormValue[]>([emptyProjectFormValue()]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateProject = (index: number, value: ProjectFormValue) =>
    setProjects((current) => current.map((p, i) => (i === index ? value : p)));

  const removeProject = (index: number) =>
    setProjects((current) => current.filter((_, i) => i !== index));

  const addProject = () => setProjects((current) => [...current, emptyProjectFormValue()]);

  const isValid =
    candidateName.trim() !== "" &&
    primaryRole.trim() !== "" &&
    projects.length > 0 &&
    projects.every((p) => p.client.trim() !== "" && p.startDate !== "" && p.endDate !== "");

  const handleSubmit = async () => {
    setError(null);
    setSubmitting(true);
    try {
      const request: ResumeGenerationRequest = {
        candidateName: candidateName.trim(),
        primaryRole: primaryRole.trim(),
        totalExperienceYears: totalExperienceYears ? Number(totalExperienceYears) : undefined,
        projects: projects.map((p): ProjectGenerationInput => ({
          client: p.client.trim(),
          startDate: p.startDate,
          endDate: p.endDate,
          role: p.role.trim() || undefined,
          domain: p.domain.trim() || undefined,
          knownTechnologies: p.knownTechnologies.trim()
            ? p.knownTechnologies.split(",").map((t) => t.trim()).filter(Boolean)
            : undefined,
        })),
      };
      const generation = await createResumeGeneration(request);
      navigate(`/resume-generations/${generation.id}`);
    } catch (e) {
      setError(e instanceof ApiRequestError ? e.message : "Could not reach the backend.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Generate Base Resume</Typography>

      {error && <Alert severity="error">{error}</Alert>}

      <TextField
        label="Candidate Name"
        required
        fullWidth
        value={candidateName}
        onChange={(e) => setCandidateName(e.target.value)}
      />
      <TextField
        label="Primary Role"
        required
        fullWidth
        value={primaryRole}
        onChange={(e) => setPrimaryRole(e.target.value)}
      />
      <TextField
        label="Total Experience (years, optional)"
        type="number"
        value={totalExperienceYears}
        onChange={(e) => setTotalExperienceYears(e.target.value)}
        sx={{ maxWidth: 260 }}
      />

      <Box>
        {projects.map((project, index) => (
          <ProjectInputRow
            key={index}
            index={index}
            value={project}
            onChange={(value) => updateProject(index, value)}
            onRemove={() => removeProject(index)}
            removable={projects.length > 1}
          />
        ))}
        <Button onClick={addProject}>+ Add Project</Button>
      </Box>

      <Box>
        <Button
          variant="contained"
          size="large"
          disabled={!isValid || submitting}
          onClick={handleSubmit}
          startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {submitting ? "Generating..." : "Generate Base Resume"}
        </Button>
      </Box>
    </Stack>
  );
}
