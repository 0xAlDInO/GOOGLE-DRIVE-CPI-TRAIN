# Manuel de Réalisation Étape par Étape : SAP CPI ↔ Google Drive ↔ Email

Ce manuel détaille la réalisation intégrale, pas à pas, de la solution d'intégration entre **SAP CPI**, **Google Drive API v3** et **Postman**.

---

## Sommaire
1. [Étape 1 : Préparation dans Google Cloud Console](#étape-1--préparation-dans-google-cloud-console)
2. [Étape 2 : Configuration de la Sécurité dans SAP CPI](#étape-2--configuration-de-la-sécurité-dans-sap-cpi)
3. [Étape 3 : Création de l'IFlow Partie 1 (Récupérer un fichier)](#étape-3--création-de-liflow-partie-1-récupérer-un-fichier)
4. [Étape 4 : Création de l'IFlow Partie 2 (Uploader un fichier)](#étape-4--création-de-liflow-partie-2-uploader-un-fichier)
5. [Étape 5 : Configuration et Exécution des Tests Postman](#étape-5--configuration-et-exécution-des-tests-postman)
6. [Étape 6 : Validation et Vérification des Livrables](#étape-6--validation-et-vérification-des-livrables)
7. [Étape 7 : Résolution du Problème d'Autorisation Google (Erreur 403 : access_denied)](#étape-7--résolution-du-problème-dautorisation-google-erreur-403--access_denied)
8. [Étape 8 : Résolution des Erreurs de Configuration Visuelle IFlow Upload (`Request Reply 1` et Adapter `FTP`)](#étape-8--résolution-des-erreurs-de-configuration-visuelle-iflow-upload-request-reply-1-et-adapter-ftp)

---

## Étape 1 : Préparation dans Google Cloud Console

1. Connectez-vous à la [Console Google Cloud](https://console.cloud.google.com/).
2. Créer ou sélectionner un projet :
   - Cliquez sur le sélecteur de projet en haut de la page.
   - Cliquez sur **Nouveau Projet** et nommez-le `project-sap-cpi`.
3. Activer l'API Google Drive :
   - Rendez-vous dans **API et services** > **Bibliothèque**.
   - Recherchez **Google Drive API**.
   - Cliquez sur **Activer**.
4. Configurer l'Écran de consentement OAuth :
   - Rendez-vous dans **API et services** > **Écran de consentement OAuth**.
   - Sélectionnez **Externe** (ou Interne si Google Workspace).
   - Indiquez le nom de l'application : `SAP CPI Google Drive Integration`.
   - Ajoutez l'adresse e-mail de support.
   - Ajoutez le scope : `https://www.googleapis.com/auth/drive`.
5. Créer les Identifiants OAuth 2.0 :
   - Rendez-vous dans **API et services** > **Identifiants**.
   - Cliquez sur **Créer des identifiants** > **ID client OAuth**.
   - Type d'application : **Application Web**.
   - Nom : `SAP CPI Client`.
   - URI de redirection autorisés (si Authorization Code) : `https://<tenant-cpi>.integrationsuite.cfapps.eu10.hana.ondemand.com/api/v1/oauth2/callback`.
   - Cliquez sur **Créer**.
   - Copiez soigneusement le **Client ID** et le **Client Secret**.

---

## Étape 2 : Configuration de la Sécurité dans SAP CPI

1. Connectez-vous à l'environnement **SAP Integration Suite / CPI Tenant**.
2. Allez dans **Monitor** > **Integrations and APIs** > **Manage Security Material**.
3. Cliquez sur **Create** > **OAuth2 Client Credentials** (ou OAuth2 Authorization Code) :
   - **Name** : `GOOGLE_DRIVE_OAUTH`
   - **Grant Type** : `Client Credentials` / `Authorization Code`
   - **Token Service URL** : `https://oauth2.googleapis.com/token`
   - **Client ID** : Copier le Client ID de Google Cloud
   - **Client Secret** : Copier le Client Secret de Google Cloud
   - **Scope** : `https://www.googleapis.com/auth/drive`
   - Cliquez sur **Save**.
4. Vérifier les certificats TLS (Keystore) :
   - Assurez-vous que la chaîne de certificats SSL/TLS de Google (`googleapis.com`) est présente dans le Keystore CPI (**Manage Keystore**).

---

## Étape 3 : Création de l'IFlow Partie 1 (Récupérer un fichier)

### 3.1 Structure du Flux Dans SAP CPI Designer
1. Dans l'espace de travail CPI (*Design*), créez un nouveau package `Google Drive Integration`.
2. Créez un artefact IFlow nommé `IFlow_Get_File_GoogleDrive`.
3. Modifiez l'adaptateur Sender HTTP:
   - **Sender** : `Postman_Client`
   - **Adapter Type** : `HTTPS`
   - **Address** : `/http/googledrive/getfile`
   - **Authorization** : `User Role` (`ESBMessaging.send`)
4. Ajoutez un composant **Script Task** (Groovy Script) :
   - Nom : `Extract Request Parameters`
   - Fichier script : `extractParams.groovy`
   - Contenu du script :
     ```groovy
     import com.sap.gateway.ip.core.customdev.util.Message;

     def Message processData(Message message) {
         def headers = message.getHeaders();
         def fileId = headers.get("fileId") ?: headers.get("CamelHttpQuery")?.split('&')?.find { it.startsWith("fileId=") }?.split('=')?.getAt(1);
         def fileName = headers.get("fileName") ?: "CV_SAPERP.PDF";

         message.setProperty("fileId", fileId);
         message.setProperty("fileName", fileName);
         return message;
     }
     ```
5. Ajoutez un composant **Request Reply / Service Task** (HTTP Receiver Adapter) :
   - **Receiver** : `Google_Drive_API`
   - **Adapter Type** : `HTTP`
   - **Address** : `https://www.googleapis.com/drive/v3/files/${property.fileId}?alt=media`
   - **Method** : `GET`
   - **Authentication** : `OAuth2 Client Credentials`
   - **Credential Name** : `GOOGLE_DRIVE_OAUTH`
6. Ajoutez un deuxième **Script Task** :
   - Nom : `Format Email Notification`
   - Fichier script : `formatEmailNotification.groovy`
   - Prépare le sujet et le texte du mail conformément au modèle attendu.
7. Ajoutez un composant **Send Task / Mail Adapter** :
   - **Receiver** : `SMTP_Mail_Server`
   - **Adapter Type** : `Mail`
   - **Address** : `smtp.gmail.com:587`
   - **Authentication** : Plain / Encrypted
8. Déployez l'IFlow (`Deploy`).

---

## Étape 4 : Création de l'IFlow Partie 2 (Uploader un fichier)

### 4.1 Structure du Flux dans le Designer SAP CPI
1. Créez un artefact IFlow nommé `IFlow_Upload_File_GoogleDrive`.
2. Configurez l'adaptateur Sender HTTP:
   - Relié du bloc **`postman`** vers le bloc **`Start`**.
   - **Address** : `/http/googledrive/uploadfile`
   - **HTTP Method** : `POST`
3. Ajoutez le premier **Script Task** (Groovy Script) :
   - Fichier script : `buildMultipartBody.groovy`
   - Construit le corps `multipart/related` (Metadata JSON + Fichier binaire).
4. Ajoutez le composant **Request Reply** dans la palette *Call* > *External Call* > *Request Reply* :
   - Placer `Request Reply` après le `Script Task`.
5. **IMPORTANT : Reliure vers le bloc `Receiver` (Configuration de l'Adaptateur HTTP)** :
   - Dans le designer SAP CPI, le composant `Request Reply` lui-même ne contient pas la configuration HTTP.
   - **Cliquez sur l'icône de flèche (`Connect`) sur la boîte `Request Reply` et faites glisser un trait vers le bloc extérieur `Receiver` (à droite)**.
   - Un menu pop-up s'affiche pour choisir l'adaptateur : sélectionnez **`HTTP`**.
   - Une flèche **`Message Flow`** (de couleur bleue) est ainsi créée entre `Request Reply` et `Receiver`.
   - **Cliquez sur cette flèche `Message Flow`** entre `Request Reply` et `Receiver`.
   - Dans le panneau de configuration du bas, sous l'onglet **Adapter Specific** :
     - **Address** : `https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`
     - **HTTP Method** : `POST`
     - **Authentication** : `OAuth2 Client Credentials` (ou `OAuth2 Authorization Code`)
     - **Credential Name** : `GOOGLE_DRIVE_OAUTH`
6. Ajoutez le second **Script Task** après le `Request Reply` :
   - Fichier script : `parseResponseAndFormatEmail.groovy`
   - Parse la réponse JSON retournée par Google Drive (`id`, `name`) et formate l'e-mail de confirmation.
7. Ajoutez l'adaptateur Mail SMTP relié à un second bloc Receiver pour envoyer la notification.
8. Déployez l'IFlow (`Deploy`).

---

## Étape 5 : Configuration et Exécution des Tests Postman

1. Ouvrez **Postman**.
2. Importez le fichier collection : `postman/Google_Drive_SAP_CPI.postman_collection.json`.
3. Mettez à jour les variables de la collection :
   - `cpi_tenant_url` : L'URL runtime de votre CPI (ex: `https://tenant-rt.cfapps.eu10.hana.ondemand.com`).
   - `cpi_client_id` : Client ID de la clé de service CPI (Service Key).
   - `cpi_client_secret` : Client Secret de la clé de service CPI.
   - `google_file_id` : ID d'un fichier existant sur Google Drive.
4. Exécuter la requête **1. Récupérer un fichier depuis Google Drive** :
   - Envoyez la requête `GET`.
   - Vérifiez le statut `200 OK`.
   - Vérifiez l'arrivée de l'e-mail avec le sujet **Fichier récupéré depuis Google Drive**.
5. Exécuter la requête **2. Uploader un fichier vers Google Drive** :
   - Dans l'onglet *Body*, insérez le contenu du fichier (ex: CSV ou binaire).
   - Envoyez la requête `POST`.
   - Vérifiez la réponse `200 OK` avec l'ID du fichier Google Drive créé.
   - Vérifiez l'arrivée de l'e-mail avec le sujet **Upload du fichier réussi**.

---

## Étape 6 : Validation et Vérification des Livrables

- [x] Artefacts SAP CPI (`IFlow_Get_File_GoogleDrive`, `IFlow_Upload_File_GoogleDrive`) créés et documentés.
- [x] Collection Postman valide et prête à l'emploi.
- [x] Spécification Technique (`SPECIFICATION_TECHNIQUE.md`) rédigée.
- [x] Manuel de Réalisation (`MANUEL_DE_REALISATION.md`) rédigé étape par étape.
- [x] Date limite de livraison respectée (Vendredi 18 septembre 2026).

---

## Étape 7 : Résolution du Problème d'Autorisation Google (Erreur 403 : `access_denied`)

Si lors de l'authentification OAuth 2.0 vous rencontrez l'erreur suivante :
> **"Accès bloqué : ondemand.com n'a pas terminé la procédure de validation de Google. L'appli est en cours de test et seuls les testeurs approuvés par le développeur y ont accès. Erreur 403 : access_denied"**

### Cause du problème
L'écran de consentement OAuth de votre projet Google Cloud est configuré en statut **"Testing" (En cours de test)**, et le compte Google utilisé (`ramanantsirahonana@gmail.com`) n'a pas été déclaré dans la liste des **Utilisateurs de test (Test users)** autorisés.

### Solution Étape par Étape

#### Option A : Ajouter votre compte Google aux Utilisateurs de Test (Recommandé pour le Dev/Test)
1. Rendez-vous sur la [Console Google Cloud - Écran de consentement OAuth](https://console.cloud.google.com/apis/credentials/consent).
2. Dans la section **Utilisateurs de test** (*Test users*), cliquez sur **+ ADD USERS** / **+ AJOUTER DES UTILISATEURS**.
3. Indiquez l'adresse e-mail de votre compte Google : `ramanantsirahonana@gmail.com`.
4. Cliquez sur **ENREGISTRER** (*SAVE*).
5. Relancez le consentement OAuth 2.0 depuis SAP CPI / Navigateur. L'accès sera immédiatement débloqué.

#### Option B : Publier l'application dans Google Cloud Console (*Publish App*)
1. Rendez-vous sur la page **Écran de consentement OAuth**.
2. Sous **Statut de la publication** (*Publishing status*), cliquez sur **PUBLIER L'APPLICATION** (*PUBLISH APP*).
3. Confirmez la publication. L'application acceptera désormais les connexions de tout compte Google sans restriction de testeur.

#### Option C : Utiliser un Compte de Service Google (*Service Account*)
Pour une intégration automatique Server-to-Server sans dépendre du consentement interactif d'un compte utilisateur :
1. Allez dans **API et services** > **Identifiants** > **Créer des identifiants** > **Compte de service**.
2. Téléchargez la clé de compte de service (format JSON / P12).
3. Importez les identifiants dans SAP CPI Security Material.

---

## Étape 8 : Résolution des Erreurs de Configuration Visuelle IFlow Upload (`Request Reply 1` et Adapter `FTP`)

Si dans le designer SAP CPI votre flux `UPLOAD_FILE` présente des icônes d'erreur rouges (`x`) sur le composant **`Request Reply 1`** et sur le canal **`FTP`** (comme illustré dans la console Designer) :

### 1. Correction de l'erreur sur `Request Reply 1`
- **Cause** : Le composant `Request Reply 1` n'est connecté à aucun `Receiver` externe via un `Message Flow`.
- **Procédure de correction** :
  1. Cliquez sur le composant `Request Reply 1` dans l'éditeur CPI.
  2. Cliquez sur l'icône de flèche de connexion (**Connect**).
  3. Faites glisser la flèche vers le bloc externe **`Receiver`** (à droite).
  4. Dans le menu pop-up de sélection d'adaptateur, choisissez **`HTTP`**.
  5. Sélectionnez la flèche bleue `Message Flow` ainsi créée.
  6. Dans le panneau inférieur sous **Adapter Specific**, saisissez :
     - **Address** : `https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`
     - **HTTP Method** : `POST`
     - **Authentication** : `OAuth2 Client Credentials`
     - **Credential Name** : `GOOGLE_DRIVE_OAUTH`

### 2. Correction de l'erreur sur l'Adaptateur `FTP`
- **Cause** : Un canal `FTP` incorrect relie le nœud `End` (ou `Groovy Script 1`) vers `Receiver`. L'API Google Drive v3 utilise HTTPS REST, pas FTP.
- **Procédure de correction** :
  1. Cliquez sur le canal pointillé étiqueté **`FTP`** (qui comporte l'icône d'erreur rouge).
  2. Cliquez sur l'icône de la **Corbeille** (ou appuyez sur la touche `Suppr`) pour supprimer cette liaison FTP incorrecte.
  3. Connectez la sortie du second Script Task (`parseResponseAndFormatEmail.groovy`) au composant **`Send Task`** (Mail Adapter) ou directement au nœud **`End`**.

### 3. Sauvegarde et Déploiement
- Cliquez sur le bouton **Save** en haut à droite.
- Cliquez sur **Deploy**.
- Le statut passe à **Deployed** sans aucune erreur.
