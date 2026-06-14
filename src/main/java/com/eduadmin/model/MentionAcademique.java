package com.eduadmin.model;

public enum MentionAcademique {
    AJOURNE("Ajourné"),
    PASSABLE("Passable"),
    ASSEZ_BIEN("Assez Bien"),
    BIEN("Bien"),
    TRES_BIEN("Très Bien"),
    EXCELLENT("Excellent");

    private final String libelle;

    MentionAcademique(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static MentionAcademique fromMoyenne(Double moyenne) {
        if (moyenne == null || moyenne < 10.0) {
            return AJOURNE;
        }
        if (moyenne >= 18.0) return EXCELLENT;
        if (moyenne >= 16.0) return TRES_BIEN;
        if (moyenne >= 14.0) return BIEN;
        if (moyenne >= 12.0) return ASSEZ_BIEN;
        return PASSABLE;
    }
}
