-- Milestone 4: Technology Timeline Engine (§14, §40, §43).
--
-- Adds era_profile / era_profile_technology (new tables - project_technology
-- from §8.4 still does not exist; nothing here links a technology to a
-- specific candidate_project yet, that's still Milestone 5+) and seeds the
-- technology catalog + five era profiles transcribed directly from §14/§40.
--
-- Years are curated approximations maintained administratively (§14), not
-- guaranteed-precise release dates - e.g. "Oracle" and "JavaScript" are
-- treated as always-mainstream rather than dated to their actual 1970s/1990s
-- origins, since the timeline engine only needs to catch clearly anachronistic
-- combinations (Java 21 on a 2012 project), not perfect historical accuracy.
-- "Modern React" (§40, 2024+ profile) is intentionally not a separate row -
-- it's mapped to the existing "React" row; "modern" describes usage patterns
-- (hooks, server components), not a separately validatable artifact.

CREATE TABLE era_profile (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label       VARCHAR(50) NOT NULL,
    start_year  SMALLINT NOT NULL,
    end_year    SMALLINT,             -- null = open-ended (the "2024+" profile)
    CONSTRAINT uq_era_profile_label UNIQUE (label)
);

CREATE TABLE era_profile_technology (
    era_profile_id  UUID NOT NULL REFERENCES era_profile(id) ON DELETE CASCADE,
    technology_id   UUID NOT NULL REFERENCES technology(id) ON DELETE CASCADE,
    PRIMARY KEY (era_profile_id, technology_id)
);

-- ---------------------------------------------------------------------------
-- Technology catalog
-- ---------------------------------------------------------------------------

INSERT INTO technology (name, category, first_available_year, mainstream_from_year, deprecated_from_year, notes) VALUES
    ('Java 5',   'language', 2004, 2004, NULL, NULL),
    ('Java 6',   'language', 2006, 2007, NULL, NULL),
    ('Java 7',   'language', 2011, 2012, NULL, NULL),
    ('Java 8',   'language', 2014, 2014, NULL, NULL),
    ('Java 11',  'language', 2018, 2019, NULL, NULL),
    ('Java 17',  'language', 2021, 2022, NULL, NULL),
    ('Java 21',  'language', 2023, 2024, NULL, NULL),

    ('Spring Framework', 'backend_framework', 2003, 2004, NULL, NULL),
    ('Spring MVC',       'backend_framework', 2003, 2005, NULL, 'Part of Spring Framework'),
    ('Spring Boot',      'backend_framework', 2014, 2015, NULL, NULL),
    ('Spring Boot 3',    'backend_framework', 2022, 2023, NULL, NULL),
    ('Struts',           'backend_framework', 2000, 2001, 2015, 'Fell out of favor after Struts2 CVEs'),

    ('JSP',       'web_technology', 1999, 2000, NULL, NULL),
    ('Servlets',  'web_technology', 1997, 1998, NULL, NULL),
    ('Hibernate', 'orm',            2001, 2003, NULL, NULL),
    ('SOAP',      'api_style',      1999, 2001, NULL, NULL),
    ('REST',      'api_style',      2000, 2008, NULL, NULL),

    ('Oracle',    'database',   1979, 1979, NULL, 'Treated as always-mainstream'),
    ('WebLogic',  'app_server', 1997, 1998, NULL, NULL),
    ('Ant',       'build_tool', 2000, 2001, 2010, NULL),
    ('Maven',     'build_tool', 2004, 2006, NULL, NULL),
    ('SVN',       'vcs',        2000, 2004, 2018, NULL),

    ('AngularJS', 'frontend_framework', 2010, 2012, 2018, 'Superseded by Angular (2+)'),
    ('Angular',   'frontend_framework', 2016, 2018, NULL, 'Angular 2+'),
    ('React',     'frontend_framework', 2013, 2015, NULL, NULL),
    ('JQuery',    'frontend_library',   2006, 2008, 2018, NULL),
    ('JavaScript','language',           1995, 1995, NULL, 'Treated as always-mainstream'),

    ('AWS',   'cloud_provider', 2006, 2012, NULL, NULL),
    ('Azure', 'cloud_provider', 2010, 2015, NULL, NULL),
    ('GCP',   'cloud_provider', 2011, 2016, NULL, NULL),

    ('Docker',     'containerization',          2013, 2015, NULL, NULL),
    ('Kubernetes', 'container_orchestration',   2014, 2017, NULL, NULL),
    ('Jenkins',    'ci_cd',                     2011, 2012, NULL, NULL),
    ('Hudson',     'ci_cd',                     2004, 2005, 2011, 'Forked into Jenkins in 2011'),
    ('Kafka',      'messaging',                 2011, 2014, NULL, NULL),
    ('Terraform',  'infrastructure_as_code',    2014, 2018, NULL, NULL),
    ('OpenTelemetry', 'observability',          2019, 2021, NULL, NULL),

    ('Microservices',             'architecture_pattern', 2014, 2016, NULL, NULL),
    ('Cloud-native architecture', 'architecture_pattern', 2015, 2019, NULL, NULL),
    ('Observability',             'practice',             2017, 2020, NULL, NULL),
    ('AI-assisted development',   'practice',             2021, 2023, NULL, 'e.g. Copilot-style tooling'),
    ('Platform engineering',      'practice',              2017, 2023, NULL, NULL);

-- ---------------------------------------------------------------------------
-- Era profiles (§40)
-- ---------------------------------------------------------------------------

INSERT INTO era_profile (label, start_year, end_year) VALUES
    ('2008-2011', 2008, 2011),
    ('2012-2015', 2012, 2015),
    ('2016-2019', 2016, 2019),
    ('2020-2023', 2020, 2023),
    ('2024+',     2024, NULL);

INSERT INTO era_profile_technology (era_profile_id, technology_id)
SELECT ep.id, t.id FROM era_profile ep, technology t
WHERE ep.label = '2008-2011' AND t.name IN
    ('Java 5', 'Java 6', 'Spring Framework', 'Struts', 'JSP', 'Servlets', 'Hibernate', 'SOAP', 'Oracle', 'WebLogic', 'Ant', 'Maven', 'SVN');

INSERT INTO era_profile_technology (era_profile_id, technology_id)
SELECT ep.id, t.id FROM era_profile ep, technology t
WHERE ep.label = '2012-2015' AND t.name IN
    ('Java 7', 'Java 8', 'Spring MVC', 'Hibernate', 'REST', 'AngularJS', 'JQuery', 'Oracle', 'Maven', 'Jenkins', 'AWS');

INSERT INTO era_profile_technology (era_profile_id, technology_id)
SELECT ep.id, t.id FROM era_profile ep, technology t
WHERE ep.label = '2016-2019' AND t.name IN
    ('Java 8', 'Java 11', 'Spring Boot', 'Microservices', 'REST', 'Angular', 'React', 'Kafka', 'Docker', 'AWS', 'Jenkins', 'Kubernetes');

INSERT INTO era_profile_technology (era_profile_id, technology_id)
SELECT ep.id, t.id FROM era_profile ep, technology t
WHERE ep.label = '2020-2023' AND t.name IN
    ('Java 11', 'Java 17', 'Spring Boot', 'Microservices', 'Kafka', 'React', 'Angular', 'Docker', 'Kubernetes', 'AWS', 'Azure', 'GCP', 'Terraform', 'OpenTelemetry');

INSERT INTO era_profile_technology (era_profile_id, technology_id)
SELECT ep.id, t.id FROM era_profile ep, technology t
WHERE ep.label = '2024+' AND t.name IN
    ('Java 17', 'Java 21', 'Spring Boot 3', 'Kubernetes', 'Kafka', 'Cloud-native architecture', 'Observability', 'OpenTelemetry', 'AI-assisted development', 'React', 'Platform engineering');
