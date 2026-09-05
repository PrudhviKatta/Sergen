import { Card, CardContent, Grid, IconButton, TextField, Typography } from "@mui/material";

export interface ProjectFormValue {
  client: string;
  startDate: string;
  endDate: string;
  role: string;
  domain: string;
  knownTechnologies: string; // comma-separated, split on submit
}

export function emptyProjectFormValue(): ProjectFormValue {
  return { client: "", startDate: "", endDate: "", role: "", domain: "", knownTechnologies: "" };
}

interface Props {
  index: number;
  value: ProjectFormValue;
  onChange: (value: ProjectFormValue) => void;
  onRemove: () => void;
  removable: boolean;
}

// §31 Screen 2 "Generate Resume" project fields: Client, Start Date, End
// Date, Role (optional). Domain/knownTechnologies are §11's other listed
// optional fields, included here since ResumeGenerationRequest accepts them.
export default function ProjectInputRow({ index, value, onChange, onRemove, removable }: Props) {
  const set = <K extends keyof ProjectFormValue>(key: K, fieldValue: ProjectFormValue[K]) =>
    onChange({ ...value, [key]: fieldValue });

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent>
        <Grid container sx={{ mb: 1, justifyContent: "space-between", alignItems: "center" }}>
          <Typography variant="subtitle1">Project {index + 1}</Typography>
          {removable && (
            <IconButton aria-label={`Remove project ${index + 1}`} onClick={onRemove} size="small">
              ✕
            </IconButton>
          )}
        </Grid>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Client"
              required
              fullWidth
              value={value.client}
              onChange={(e) => set("client", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Role (optional)"
              fullWidth
              value={value.role}
              onChange={(e) => set("role", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Start Date"
              type="date"
              required
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              value={value.startDate}
              onChange={(e) => set("startDate", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="End Date"
              type="date"
              required
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              value={value.endDate}
              onChange={(e) => set("endDate", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Domain (optional)"
              fullWidth
              value={value.domain}
              onChange={(e) => set("domain", e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Known technologies (optional, comma-separated)"
              fullWidth
              value={value.knownTechnologies}
              onChange={(e) => set("knownTechnologies", e.target.value)}
            />
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}
