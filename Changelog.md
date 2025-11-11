# HeadBlocks v2.8.2

Thank you for using HeadBlocks ❤

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**discussion
  **](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)

## Nouveautés

### ✨ Nouvelles fonctionnalités

- **Masquage des têtes trouvées** : Ajout d'une option pour masquer visuellement les têtes déjà découvertes par les
  joueurs (nécessite PacketEvents)
- **Interface de récompenses** : Nouveau GUI permettant de gérer et visualiser les récompenses configurées pour chaque
  tête
- **Réinitialisation par tête** : Possibilité de réinitialiser la progression d'un joueur pour une tête spécifique via
  les commandes `/reset` et `/resetall`

### 🚀 Améliorations

- **Optimisation du cache** : Refonte complète du système de cache pour Redis et Memory, incluant le cache des joueurs,
  du classement et des têtes
- **Performances Redis** : Remplacement des listes par des sets pour le stockage des têtes des joueurs, simplifiant les
  opérations et améliorant les performances
- **Gestion des hologrammes** : Refonte du système d'hologrammes avec support des placeholders. Suppression du support
  CMI/FancyHolograms & DecentHolograms, remplacé par le type "Advanced hologram"
- **Particules asynchrones** : Optimisation du spawning des particules en utilisant le scheduler Bukkit pour réduire la
  charge serveur

### 🐛 Corrections de bugs

- **Gestion d'erreurs** : Gestion du spam d'erreur au démarrage s'il y a un problème de chargement avec la base de
  données.

### 🔧 Technique

- **Retrait des dépendances** : Permettant de faciliter la compilation du plugin.
- **Gestion des dépendances** : Centralisation des versions des dépendances via un catalog Gradle (`libs.versions.toml`)
- **Structure du projet** : Simplification de la structure Gradle en supprimant le module `core` inutile

---
