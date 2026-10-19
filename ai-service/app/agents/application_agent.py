from app.agents.career_agent import CareerAgent, SKILL_ALIASES


class ApplicationAgent:
    """Generate deterministic application documents grounded in the supplied CV."""

    def generate(
        self,
        cv_text: str,
        job_title: str,
        company: str,
        job_description: str,
        candidate_name: str,
    ) -> dict:
        values = {
            "cv_text": cv_text,
            "job_title": job_title,
            "company": company,
            "job_description": job_description,
            "candidate_name": candidate_name,
        }
        for name, value in values.items():
            if not value or not value.strip():
                raise ValueError(f"{name} is required")

        candidate_skills = CareerAgent._extract_skills(cv_text)
        offer_skills = CareerAgent._extract_skills(job_description)
        candidate_skill_set = set(candidate_skills)
        matched_skills = [
            skill for skill in offer_skills if skill in candidate_skill_set
        ]
        missing_skills = [
            skill for skill in offer_skills if skill not in candidate_skill_set
        ]

        cv_lines = [line for line in cv_text.splitlines() if line.strip()]
        matched_aliases = {
            alias.lower()
            for skill in matched_skills
            for alias in SKILL_ALIASES[skill]
        }
        relevant_lines = [
            line for line in cv_lines if self._contains_skill(line, matched_aliases)
        ]
        other_lines = [
            line for line in cv_lines if not self._contains_skill(line, matched_aliases)
        ]

        adapted_sections = [
            f"Target role: {job_title.strip()} at {company.strip()}",
        ]
        if matched_skills:
            adapted_sections.append(f"Key skills for this role: {', '.join(matched_skills)}")
        adapted_sections.append("Relevant experience")
        adapted_sections.extend(relevant_lines)
        adapted_sections.append("Other information")
        adapted_sections.extend(other_lines)
        adapted_cv = "\n".join(adapted_sections)

        if matched_skills:
            skills_paragraph = (
                "My experience with "
                + ", ".join(matched_skills)
                + " aligns with the needs of this role."
            )
        else:
            skills_paragraph = (
                "I am motivated to learn and contribute to this role with dedication."
            )
        cover_letter = "\n\n".join(
            [
                f"Dear Hiring Manager at {company.strip()},",
                (
                    f"I am interested in the {job_title.strip()} position at "
                    f"{company.strip()}."
                ),
                skills_paragraph,
                "Thank you for considering my application. I would welcome the opportunity to discuss my application.",
                f"Sincerely,\n{candidate_name.strip()}",
            ]
        )
        verification = self.verify(cv_text, adapted_cv, cover_letter)
        return {
            "adapted_cv": adapted_cv,
            "cover_letter": cover_letter,
            "matched_skills": matched_skills,
            "missing_skills": missing_skills,
            "verification": verification,
        }

    def verify(self, cv_text: str, adapted_cv: str, cover_letter: str) -> dict:
        values = {
            "cv_text": cv_text,
            "adapted_cv": adapted_cv,
            "cover_letter": cover_letter,
        }
        for name, value in values.items():
            if not value or not value.strip():
                raise ValueError(f"{name} is required")

        cv_skills = set(CareerAgent._extract_skills(cv_text))
        generated_skills = set(
            CareerAgent._extract_skills(
                self._remove_metadata(adapted_cv, cover_letter)
            )
        )
        unsupported_skills = sorted(generated_skills - cv_skills)
        return {
            "verified": not unsupported_skills,
            "unsupported_skills": unsupported_skills,
        }

    @staticmethod
    def _remove_metadata(adapted_cv: str, cover_letter: str) -> str:
        adapted_lines = [
            line for line in adapted_cv.splitlines()
            if not line.startswith("Target role: ")
        ]
        letter_lines = []
        skip_signature_name = False
        for line in cover_letter.splitlines():
            if line.startswith("Dear Hiring Manager at "):
                continue
            if line.startswith("I am interested in the "):
                continue
            if line == "Sincerely,":
                skip_signature_name = True
                continue
            if skip_signature_name:
                skip_signature_name = False
                continue
            letter_lines.append(line)
        return "\n".join(adapted_lines + letter_lines)

    @staticmethod
    def _contains_skill(line: str, aliases: set[str]) -> bool:
        lowered = line.lower()
        return any(alias in lowered for alias in aliases)
