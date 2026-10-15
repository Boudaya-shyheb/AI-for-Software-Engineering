def calculer_prix_chambre(
    type_chambre: str,
    saison: str,
    a_vue_mer: bool,
    duree_sejour: int,
) -> float:
    """
    Calcule le prix total d'un séjour dans une chambre d'hôtel.
    - Prix de base: standard=100€/nuit, suite=250€/nuit.
    - Multiplicateur de saison: haute=+50%, basse=pas de changement.
    - Supplément vue sur mer: +20€/nuit.
    - Réduction long séjour: -10% sur le total si le séjour > 7 nuits.
    """
    if duree_sejour <= 0:
        raise ValueError("La durée du séjour doit être positive.")

    prix_base_nuit = 0
    if type_chambre == "standard":
        prix_base_nuit = 100
    elif type_chambre == "suite":
        prix_base_nuit = 250
    else:
        raise ValueError("Type de chambre inconnu.")

    prix_nuit = prix_base_nuit

    if a_vue_mer:
        prix_nuit += 20

    prix_total = prix_nuit * duree_sejour

    if saison == "haute":
        prix_total *= 1.5
    elif saison != "basse":
        raise ValueError("Saison inconnue.")

    if duree_sejour > 7:
        prix_total *= 0.9

    return round(prix_total, 2)
