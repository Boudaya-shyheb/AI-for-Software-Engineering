package com.aicareeragent.model;

public enum ProgressStatus {
    TO_DO,
    IN_PROGRESS,
    DONE;

    public static boolean canTransition(ProgressStatus current, ProgressStatus next) {
        if (current == null || next == null) {
            return false;
        }
        if (current == next) {
            return true;
        }
        return switch (current) {
            case TO_DO -> next == IN_PROGRESS || next == DONE;
            case IN_PROGRESS -> next == TO_DO || next == DONE;
            case DONE -> next == TO_DO || next == IN_PROGRESS;
        };
    }

    public String getLabel() {
        return switch (this) {
            case TO_DO -> "À faire";
            case IN_PROGRESS -> "En cours";
            case DONE -> "Terminé";
        };
    }
}
