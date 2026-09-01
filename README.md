# LocaAuto Offline

Application Android native de gestion d'une agence de location de voitures au Maroc.

## Principes

- Fonctionne entièrement hors ligne : aucune permission Internet, aucun compte et aucun service cloud.
- Toutes les données sont stockées localement dans Room/SQLite.
- Les réservations refusent les véhicules en maintenance et les chevauchements de dates.
- Les changements de statut créent les contrats et factures associés.
- Les contrats et factures peuvent être exportés en PDF dans l'espace Documents de l'application.

## Modules

- Tableau de bord : chiffre d'affaires encaissé, occupation, flotte et dernières réservations.
- Flotte : véhicules, tarifs, statuts et entretiens.
- Réservations : création, validation, démarrage, clôture et annulation.
- Clients : recherche et fiches conducteurs.
- Documents : contrats, factures, paiements et export PDF.
- Opérations : entretiens et dépenses de l'agence.

Les modules Flotte, Réservations, Clients et Documents disposent de leur cycle CRUD : création, consultation, modification et suppression. Les contrats sont créés à la confirmation d'une réservation et les factures à la signature du contrat (des APIs explicites existent aussi pour ces créations). Les contrats signés sont exportés à partir du modèle bilingue `Exemple_contrat_location_app_auto.pdf` : les informations du client, du véhicule et de la réservation sont superposées dans les champs correspondants. La fiche Client permet également de conserver les URI persistées d'une Carte Nationale et d'un Permis de conduite via le sélecteur de fichiers Android. Les suppressions contrôlent les dépendances métier (historique de réservations, entretiens, paiements) et les changements liés sont exécutés dans une transaction Room unique. Une réservation recalculée met aussi à jour sa facture liée, et sa suppression supprime atomiquement ses documents générés.

## Ouvrir le projet

Ouvrir le dossier dans Android Studio, laisser Gradle synchroniser les dépendances, puis lancer la configuration `app` sur un émulateur ou un appareil Android.

Le projet ne demande aucune clé API ni configuration Firebase. Les données d'exemple sont insérées uniquement lors de la première création de la base locale.

## Prochaines extensions recommandées

- Ajouter une sauvegarde/restauration chiffrée vers un fichier local ou une clé USB.
- Ajouter l'impression, le partage et la signature numérique des contrats.
- Ajouter un verrouillage PIN/biométrique pour protéger les données clients.
- Ajouter les rappels locaux de retour, d'entretien et de paiement.
# loca-auto-offline
