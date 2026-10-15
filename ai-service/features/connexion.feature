# language: fr
Fonctionnalité: Connexion de l'utilisateur
  En tant qu'utilisateur enregistré
  Je veux me connecter à mon compte avec mon email et mon mot de passe
  Afin d'accéder à mon tableau de bord personnel

  Scénario: Connexion réussie
    Soit l'utilisateur "alice@example.com" avec le mot de passe "MotDePasseValide123!"
    Quand il se connecte avec l'email "alice@example.com" et le mot de passe "MotDePasseValide123!"
    Alors il est redirigé vers "/dashboard"

  Scénario: Mot de passe incorrect
    Soit l'utilisateur "alice@example.com" avec le mot de passe "MotDePasseValide123!"
    Quand il se connecte avec l'email "alice@example.com" et le mot de passe "mauvais-mot-de-passe"
    Alors le message d'erreur "Email ou mot de passe incorrect." s'affiche

  Scénario: Adresse email inconnue
    Soit aucun utilisateur n'est enregistré avec l'email "inconnu@example.com"
    Quand il se connecte avec l'email "inconnu@example.com" et le mot de passe "MotDePasseValide123!"
    Alors le message d'erreur "Email ou mot de passe incorrect." s'affiche
