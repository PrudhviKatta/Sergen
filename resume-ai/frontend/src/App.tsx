import { AppBar, Box, Button, Container, Toolbar, Typography } from "@mui/material";
import { Link as RouterLink, Route, Routes } from "react-router-dom";
import GenerateResumePage from "./pages/GenerateResumePage";
import ReviewPage from "./pages/ReviewPage";
import UploadResumePage from "./pages/UploadResumePage";

// §31 Recruiter/Candidate UI: Screen 1 (Knowledge Base/upload, Milestone 2),
// Screen 2 (Generate Resume), Screen 3 (Review). Screen 4 (Export) is
// Milestone 8, not built.
export default function App() {
  return (
    <>
      <AppBar position="static" color="primary" enableColorOnDark>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Sergen
          </Typography>
          <Button color="inherit" component={RouterLink} to="/">
            Generate
          </Button>
          <Button color="inherit" component={RouterLink} to="/upload">
            Upload Resume
          </Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="md">
        <Box sx={{ py: 4 }}>
          <Routes>
            <Route path="/" element={<GenerateResumePage />} />
            <Route path="/resume-generations/:id" element={<ReviewPage />} />
            <Route path="/upload" element={<UploadResumePage />} />
          </Routes>
        </Box>
      </Container>
    </>
  );
}
