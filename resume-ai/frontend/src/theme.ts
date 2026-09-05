import { createTheme } from "@mui/material/styles";

// §6 "Recommended: React, TypeScript, Vite, Material UI". Default palette -
// nothing about this app needs custom branding yet.
export const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#1565c0" },
  },
});
