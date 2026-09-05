import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import Button from "@mui/material/Button";
import type { GeneratedProjectResponse, SimilarityVerdict } from "../api/types";

const VERDICT_COLOR: Record<SimilarityVerdict, "success" | "warning" | "error"> = {
  ACCEPTABLE: "success",
  REVIEW: "warning",
  REWRITE: "error",
};

const NOT_IMPLEMENTED =
  "Not implemented yet - the backend has no regenerate/approve endpoint (see docs/IMPLEMENTATION_NOTES.md).";

// §31 Screen 3 "Review" - one project's card. Approve/Edit/Regenerate are
// shown per the spec's layout but disabled: generation.ResumeGenerationController
// only exposes create/get so far, not per-project regenerate/approve (§21).
export default function GeneratedProjectCard({ project }: { project: GeneratedProjectResponse }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" sx={{ gap: 1, justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap" }}>
          <Box>
            <Typography variant="h6">{project.clientName}</Typography>
            <Typography variant="body2" color="text.secondary">
              {project.roleTitle ?? "—"} · {project.startDate} to {project.endDate}
              {project.domain ? ` · ${project.domain}` : ""}
            </Typography>
          </Box>
          {project.similarityVerdict && (
            <Chip
              label={`Similarity: ${project.similarityVerdict}${
                project.similarityScore !== null ? ` (${project.similarityScore.toFixed(2)})` : ""
              }`}
              color={VERDICT_COLOR[project.similarityVerdict]}
              size="small"
            />
          )}
        </Stack>

        {project.duplicatePhraseDetected && (
          <Alert severity="warning" sx={{ mt: 2 }}>
            A 12+ word phrase in this draft matched existing reference material.
          </Alert>
        )}

        {project.rewriteAttempts > 1 && (
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1 }}>
            Rewritten {project.rewriteAttempts - 1} time{project.rewriteAttempts - 1 === 1 ? "" : "s"} before acceptance.
          </Typography>
        )}

        <Typography sx={{ mt: 2 }}>{project.description}</Typography>

        {project.responsibilities.length > 0 && (
          <Box component="ul" sx={{ mt: 1, mb: 1, pl: 3 }}>
            {project.responsibilities.map((item, i) => (
              <li key={i}>
                <Typography variant="body2">{item}</Typography>
              </li>
            ))}
          </Box>
        )}

        {project.environment.length > 0 && (
          <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: "wrap" }}>
            {project.environment.map((tech) => (
              <Chip key={tech} label={tech} size="small" variant="outlined" />
            ))}
          </Stack>
        )}

        <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
          <Tooltip title={NOT_IMPLEMENTED}>
            <span>
              <Button size="small" disabled>
                Approve
              </Button>
            </span>
          </Tooltip>
          <Tooltip title={NOT_IMPLEMENTED}>
            <span>
              <Button size="small" disabled>
                Edit
              </Button>
            </span>
          </Tooltip>
          <Tooltip title={NOT_IMPLEMENTED}>
            <span>
              <Button size="small" disabled>
                Regenerate
              </Button>
            </span>
          </Tooltip>
        </Stack>
      </CardContent>
    </Card>
  );
}
