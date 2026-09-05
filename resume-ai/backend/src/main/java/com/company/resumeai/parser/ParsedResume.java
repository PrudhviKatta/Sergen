package com.company.resumeai.parser;

import java.util.List;

/** §10 Resume Parsing Output shape. */
public record ParsedResume(ParsedCandidate candidate, List<ParsedProject> projects) {
}
