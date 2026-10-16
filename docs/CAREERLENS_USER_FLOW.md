# CareerLens: parcours utilisateur après authentification

## 1. Connexion et espace personnel

Après inscription ou connexion, l’utilisateur arrive dans **Matching IA**. Le frontend charge son profil privé depuis l’API. Le compte administrateur garde son espace de gestion et n’accède pas au parcours candidat.

## 2. Enregistrer un CV une seule fois

Dans **Profil**, l’utilisateur peut importer un CV PDF, DOCX ou TXT, ou coller le texte directement. Il vérifie le texte extrait, puis enregistre son profil. Le backend associe le CV au compte connecté.

## 3. Relancer un matching

Dans **Matching IA**, deux choix apparaissent côte à côte :

- **Use my CV** récupère le texte déjà enregistré dans le profil et le place dans le champ modifiable ;
- **Upload CV** importe un autre document pour cette analyse.

Si le profil ne contient pas encore de CV, l’interface l’indique et invite l’utilisateur à en enregistrer un depuis **Profil**. Il peut aussi coller son texte dans le champ.

## 4. Laisser CareerLens comparer le CV

Un clic sur **Analyze my CV & find matching jobs** déclenche le workflow :

1. l’API compare le CV avec les offres publiées et actives ;
2. elle renvoie les offres classées et leurs compétences correspondantes ;
3. l’interface sélectionne l’offre la mieux classée ;
4. elle génère le rapport détaillé pour cette offre ;
5. elle affiche le rapport avant les autres offres classées.

Le rapport comprend le score global, les correspondances lexicales et sémantiques, les compétences alignées, les compétences à renforcer et les recommandations.

## 5. Continuer depuis une offre

L’utilisateur peut ouvrir une autre offre classée et lancer un matching ciblé, ou démarrer **Interview practice** avec le CV et le poste sélectionné. Les simulations terminées ou en cours sont disponibles dans leur historique.

Le produit automatise l’analyse et le classement, mais ne transmet pas de dossier de candidature à une entreprise.

## Démonstration sans services

Ouvrir [`demo-matching.html`](demo-matching.html) dans un navigateur. La maquette permet de charger le CV de démonstration, de lancer l’analyse simulée et de consulter un rapport et des offres fictifs. Elle ne démarre pas le frontend, l’API, le service IA, PostgreSQL ou Docker.
