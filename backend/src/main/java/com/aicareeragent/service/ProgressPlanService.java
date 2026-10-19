package com.aicareeragent.service;

import com.aicareeragent.model.AnalysisRecord;
import com.aicareeragent.model.JobOffer;
import com.aicareeragent.model.ProgressGoal;
import com.aicareeragent.model.ProgressPlan;
import com.aicareeragent.model.ProgressStatus;
import com.aicareeragent.model.UserAccount;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProgressPlanService {
    private static final String GENERIC_SUMMARY = "Suggestions automatiques fondées sur les lacunes détectées lors du matching.";

    public ProgressPlan buildPlan(UserAccount user, AnalysisRecord analysis, JobOffer jobOffer, List<String> missingSkills) {
        List<String> skills = normalizeSkills(missingSkills);
        String title = buildTitle(skills);
        String summary = buildSummary(skills);
        ProgressPlan plan = new ProgressPlan(user, analysis, jobOffer, title, summary);
        List<ProgressGoal> goals = new ArrayList<>();
        if (skills.isEmpty()) {
            goals.add(new ProgressGoal(plan, "Compétence cible", "Renforcer la progression globale pour combler les écarts détectés.", "Consacrez 30 minutes à relire la documentation ou un cas pratique lié au poste ciblé.", "Produisez un petit exercice ou un prototype court pour valider votre compréhension.", 0));
        } else {
            for (int index = 0; index < skills.size(); index++) {
                String skill = skills.get(index);
                var template = resolveTemplate(skill);
                goals.add(new ProgressGoal(plan, skill, template.objective(), template.activity(), template.exercise(), index));
            }
        }
        plan.setGoals(goals);
        return plan;
    }

    public String buildTitle(List<String> missingSkills) {
        List<String> normalized = normalizeSkills(missingSkills);
        if (normalized.isEmpty()) {
            return "Plan de progression personnalisé";
        }
        if (normalized.size() == 1) {
            return "Plan de progression : " + normalized.getFirst();
        }
        return "Plan de progression : " + normalized.getFirst() + " + " + (normalized.size() - 1) + " axe(s)";
    }

    public String buildSummary(List<String> missingSkills) {
        List<String> normalized = normalizeSkills(missingSkills);
        if (normalized.isEmpty()) {
            return GENERIC_SUMMARY;
        }
        return "Suggestions automatiques fondées sur les compétences manquantes détectées : " + String.join(", ", normalized) + ".";
    }

    public ProgressStatus defaultStatusForNewSkill() {
        return ProgressStatus.TO_DO;
    }

    private List<String> normalizeSkills(List<String> missingSkills) {
        if (missingSkills == null || missingSkills.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String skill : missingSkills) {
            if (skill == null || skill.isBlank()) {
                continue;
            }
            String normalized = skill.trim();
            if (!normalized.isEmpty()) {
                unique.add(normalized);
            }
        }
        return new ArrayList<>(unique);
    }

    private SkillTemplate resolveTemplate(String skill) {
        String key = skill.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "java" -> new SkillTemplate(
                    "Maîtriser les concepts de base de Java pour écrire des méthodes lisibles et fiables.",
                    "Suivre un tutoriel de 30 minutes sur les collections, les exceptions et la structure d'un programme Java.",
                    "Créer une mini-application Java avec 2 classes, 1 interface et 3 tests de validation."
            );
            case "spring boot" -> new SkillTemplate(
                    "Mettre en place une API REST Spring Boot simple et correctement structurée.",
                    "Revoir un guide de 25 minutes sur les contrôleurs, services et injections de dépendances.",
                    "Construire une API avec 2 endpoints, validation des entrées et un test unitaire simple."
            );
            case "sql" -> new SkillTemplate(
                    "Renforcer la manipulation des requêtes SQL pour filtrer, joindre et agréger des données.",
                    "Lire un exemple de base de données et pratiquer 3 requêtes SELECT, JOIN et GROUP BY.",
                    "Réaliser un petit schéma de données et tester 5 requêtes sur un jeu de données simple."
            );
            case "postgresql" -> new SkillTemplate(
                    "Maîtriser la modélisation et les requêtes PostgreSQL courantes.",
                    "Consacrer 30 minutes à un atelier de création de tables et de requêtes SQL.",
                    "Mettre en place une base de test avec 2 tables et exécuter des requêtes de lecture et d’agrégation."
            );
            case "angular" -> new SkillTemplate(
                    "Comprendre la structure d’une application Angular et la communication avec une API.",
                    "Suivre un mini-cours sur les composants, les services et le binding de données.",
                    "Créer un composant Angular avec un formulaire et un appel HTTP vers une API mockée."
            );
            case "typescript" -> new SkillTemplate(
                    "Renforcer le typage et la robustesse du code TypeScript.",
                    "Relire un guide sur les interfaces, types unions et gestion d’erreurs.",
                    "Refactoriser un petit module TypeScript avec des interfaces et des validations typées."
            );
            case "docker" -> new SkillTemplate(
                    "Comprendre le packaging et le déploiement d'une application avec Docker.",
                    "Lire un tutoriel de 20 minutes sur les images, conteneurs et volumes.",
                    "Créer un Dockerfile simple et valider le lancement d’une application locale."
            );
            case "api" -> new SkillTemplate(
                    "Structurer des API claires, testables et documentées.",
                    "Comparer 2 exemples d’API REST et noter les bonnes pratiques observées.",
                    "Rédiger une petite API REST avec endpoints, validation et retour cohérent."
            );
            case "python" -> new SkillTemplate(
                    "Améliorer la qualité et la lisibilité de scripts Python.",
                    "Faire une séance de 25 minutes sur les fonctions, modules et gestion des erreurs.",
                    "Écrire un script Python avec lecture de fichiers, traitement simple et sortie exploitable."
            );
            case "git" -> new SkillTemplate(
                    "Conseiller votre maîtrise des branches, commits et résolution de conflits.",
                    "Suivre un exercice de 20 minutes sur les commandes Git de base et de collaboration.",
                    "Créer une branche, faire 3 commits et simuler une résolution de conflit simple."
            );
            case "agile" -> new SkillTemplate(
                    "Mettre en pratique les routines d’une équipe agile pour mieux organiser le travail.",
                    "Relire les principes de planification, backlog et stand-up sur un cas simple.",
                    "Rédiger un backlog avec priorisation, tâches et critères de validation."
            );
            case "communication" -> new SkillTemplate(
                    "Améliorer la clarté de vos explications techniques et de vos échanges professionnels.",
                    "Préparer une synthèse sur un sujet technique de 10 minutes pour un public non spécialiste.",
                    "Réaliser un petit pitch de 2 minutes sur un projet ou un enjeu métier."
            );
            default -> new SkillTemplate(
                    "Renforcer votre maîtrise de " + skill + " dans un contexte professionnel concret.",
                    "Consacrer 30 minutes à une ressource fiable pour comprendre les concepts clés et les bonnes pratiques associées à " + skill + ".",
                    "Mener un exercice pratique sur un cas simple pour appliquer " + skill + " à un projet ou un scénario réel."
            );
        };
    }

    private record SkillTemplate(String objective, String activity, String exercise) {}
}
