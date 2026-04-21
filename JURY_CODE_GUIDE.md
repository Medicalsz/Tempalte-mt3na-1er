# Guide Du Code Pour Le Jury

## 1. Vue generale du projet

Ce projet est une application desktop en **JavaFX** organisee avec **Maven**.  
L'application suit une separation proche de **MVC** :

- **Model** : classes metier dans `src/main/java/com/medicare/models`
- **View** : interfaces FXML dans `src/main/resources/com/medicare`
- **Controller** : logique UI dans `src/main/java/com/medicare/controllers`
- **Service** : acces base de donnees et logique CRUD dans `src/main/java/com/medicare/services`
- **Utils** : connexion BD et stockage de fichiers dans `src/main/java/com/medicare/utils`

Exemples :

- `User.java` represente un utilisateur
- `login-view.fxml` et `register-view.fxml` sont les vues
- `LoginController.java` et `RegisterController.java` gerent les interactions
- `UserService.java` et `RendezVousService.java` executent les requetes SQL

## 2. Explication simple du MVC dans ce projet

### Model

Le **Model** contient les donnees metier.  
Exemple : `User.java` contient les champs `nom`, `prenom`, `email`, `password`, `numero`, `adresse`, `roles`, etc.

```java
public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String numero;
    private String adresse;
    private String photo;
    private String roles;
}
```

### View

La **View** est l'interface graphique JavaFX, definie dans les fichiers `.fxml`.  
Exemples :

- `login-view.fxml`
- `register-view.fxml`
- `dashboard-patient-view.fxml`
- `dashboard-medecin-view.fxml`
- `dashboard-admin-view.fxml`

### Controller

Le **Controller** lit les donnees saisies par l'utilisateur, valide, puis appelle les services.  
Exemples :

- `LoginController` pour la connexion
- `RegisterController` pour l'inscription
- `DashboardPatientController` pour le tableau de bord patient

### Service

La couche **Service** communique avec MySQL avec JDBC.  
Exemples :

- `UserService` : login, register, update profile, delete user
- `RendezVousService` : CRUD des rendez-vous

Donc le flux est :

**FXML (vue) -> Controller -> Service -> Base MySQL**

## 3. JavaFX + Maven + packages

Le projet est gere par Maven avec `pom.xml`.

Les dependances principales sont :

- `javafx-controls`
- `javafx-fxml`
- `mysql-connector-j`
- `jbcrypt`
- `ikonli-javafx`

Extrait du `pom.xml` :

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
</dependency>
```

Le plugin `javafx-maven-plugin` permet de lancer l'application avec :

```bash
mvn clean javafx:run
```

Le point d'entree est `HelloApplication.java` :

```java
public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
    Scene scene = new Scene(fxmlLoader.load());
    stage.setTitle("Medicare");
    stage.setScene(scene);
    stage.show();
}
```

Le fichier `module-info.java` declare les modules JavaFX et ouvre les packages aux controllers FXML :

```java
module com.medicare.medicarejavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
}
```

## 4. Comment je recupere email, mot de passe et telephone et comment je les valide

### 4.1 Login

Dans `LoginController.java`, les champs sont injectes depuis la vue JavaFX :

```java
@FXML private TextField emailField;
@FXML private PasswordField passwordField;
@FXML private Label errorLabel;
```

Quand l'utilisateur clique sur le bouton login :

```java
String email = emailField.getText().trim();
String password = passwordField.getText();

if (email.isEmpty() || password.isEmpty()) {
    errorLabel.setText("Veuillez remplir tous les champs.");
    return;
}

User user = userService.login(email, password);
```

Explication :

- `emailField.getText()` recupere l'email saisi
- `passwordField.getText()` recupere le mot de passe
- `.trim()` retire les espaces inutiles
- si un champ est vide, on bloque la connexion
- sinon on appelle `userService.login(email, password)`

### 4.2 Verification du mot de passe dans le service

Dans `UserService.java`, on verifie l'utilisateur avec une requete SQL parametree :

```java
String query = "SELECT * FROM user WHERE email = ?";
try (PreparedStatement ps = cnx.prepareStatement(query)) {
    ps.setString(1, email);
    ResultSet rs = ps.executeQuery();

    if (rs.next()) {
        String storedHash = rs.getString("password");
        String javaHash = storedHash.replace("$2y$", "$2a$");

        if (BCrypt.checkpw(password, javaHash)) {
            return mapUser(rs);
        }
    }
}
```

Ici :

- on cherche l'utilisateur par email
- le mot de passe n'est pas compare en clair
- il est compare avec **BCrypt**
- si le hash correspond, on retourne l'objet `User`

### 4.3 Signup / Register

Dans `RegisterController.java`, on recupere les informations saisies :

```java
String nom = nomField.getText().trim();
String prenom = prenomField.getText().trim();
String email = emailField.getText().trim().toLowerCase();
String password = passwordField.getText();
String confirm = confirmPasswordField.getText();
String numero = numeroField.getText().trim();
String adresse = adresseField.getText().trim();
```

Les validations sont faites dans `validateForm(...)`.

#### Validation email

```java
private static final Pattern EMAIL_PATTERN =
    Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

if (!EMAIL_PATTERN.matcher(email).matches()) {
    return "Email invalide.";
}
```

#### Validation telephone

```java
private static final Pattern PHONE_PATTERN =
    Pattern.compile("^[0-9+\\s]{8,15}$");

if (!PHONE_PATTERN.matcher(numero).matches()) {
    return "Numero invalide. Utilisez entre 8 et 15 chiffres.";
}
```

#### Validation mot de passe

```java
if (password.length() < 6) {
    return "Le mot de passe doit contenir au moins 6 caracteres.";
}
if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
    return "Le mot de passe doit contenir au moins une lettre et un chiffre.";
}
if (!password.equals(confirm)) {
    return "Les mots de passe ne correspondent pas.";
}
```

#### Verification email deja utilise

```java
if (userService.emailExists(email)) {
    return "Cet email est deja utilise.";
}
```

Puis on cree l'objet `User` et on l'envoie au service :

```java
User user = new User(
    nom,
    prenom,
    email,
    password,
    numero,
    adresse.isEmpty() ? null : adresse,
    null,
    "[\"ROLE_USER\"]",
    false
);

boolean success = userService.register(user);
```

## 5. Comment j'etablis la connexion entre l'application et la base de donnees

La connexion est centralisee dans `MyConnection.java`.

```java
public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/medicare",
                    "root",
                    ""
            );
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

Explication :

- `DriverManager.getConnection(...)` connecte l'application a MySQL
- la base utilisee est `medicare`
- le pattern applique est un **Singleton**
- une seule instance de connexion est partagee dans tout le projet

Recuperation de la connexion :

```java
cnx = MyConnection.getInstance().getCnx();
```

On retrouve cela dans `UserService` et `RendezVousService`.

## 6. Comment je gere la session utilisateur

Comme c'est une application desktop JavaFX, il n'y a pas de session web HTTP.  
La "session" est geree **en memoire** avec un utilisateur courant stocke dans les dashboards.

Exemple dans `DashboardPatientController.java` :

```java
private static User currentUser;

public static void setCurrentUser(User user) { currentUser = user; }
public static User getCurrentUser() { return currentUser; }
```

Apres login :

```java
if (user.getRoles().contains("ROLE_ADMIN")) {
    DashboardAdminController.setCurrentUser(user);
} else {
    DashboardPatientController.setCurrentUser(user);
}
```

Ensuite, le dashboard reutilise `currentUser` pour :

- afficher le nom
- afficher l'email
- afficher la photo
- ouvrir le profil
- filtrer les rendez-vous du bon utilisateur

Lors du logout :

```java
currentUser = null;
```

Donc la session est simple :

1. Login reussi
2. Stockage du `currentUser`
3. Utilisation du `currentUser` dans les pages protegees
4. Logout = suppression de `currentUser`

## 7. Comment j'utilise les requetes SQL dans le CRUD

Le projet contient une interface generique :

```java
public interface Crud<T> {
    void add(T t);
    void update(T t);
    void delete(int id);
    List<T> getAll();
}
```

`UserService` implemente cette interface :

```java
public class UserService implements Crud<User> {
```

### 7.1 Create

Exemple ajout utilisateur :

```java
String query = "INSERT INTO user (nom, prenom, email, password, numero, adresse, photo, roles, is_verified) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
try (PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
    ps.setString(1, user.getNom());
    ps.setString(2, user.getPrenom());
    ps.setString(3, user.getEmail());
    ps.setString(4, hashPassword(user.getPassword()));
    ...
    ps.executeUpdate();
}
```

Ici j'utilise `PreparedStatement`, ce qui permet :

- de passer les valeurs proprement
- d'eviter la concatenation directe
- de mieux securiser la requete

### 7.2 Read

Exemple lecture de tous les utilisateurs :

```java
String query = "SELECT * FROM user ORDER BY id DESC";
try (Statement st = cnx.createStatement()) {
    ResultSet rs = st.executeQuery(query);
    while (rs.next()) {
        list.add(mapUser(rs));
    }
}
```

Exemple lecture par identifiant :

```java
String query = "SELECT * FROM user WHERE id = ?";
try (PreparedStatement ps = cnx.prepareStatement(query)) {
    ps.setInt(1, userId);
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
        return mapUser(rs);
    }
}
```

### 7.3 Update

Exemple mise a jour du profil :

```java
StringBuilder query = new StringBuilder(
    "UPDATE user SET nom = ?, prenom = ?, email = ?, numero = ?, adresse = ?, photo = ?"
);
...
query.append(" WHERE id = ?");
```

Puis :

```java
ps.setString(index++, user.getNom());
ps.setString(index++, user.getPrenom());
ps.setString(index++, user.getEmail());
...
ps.setInt(index, user.getId());
return ps.executeUpdate() > 0;
```

### 7.4 Delete

Exemple suppression d'un rendez-vous :

```java
String q = "DELETE FROM rendez_vous WHERE id = ?";
PreparedStatement ps = cnx.prepareStatement(q);
ps.setInt(1, id);
ps.executeUpdate();
```

### 7.5 CRUD complet dans `RendezVousService`

Le service `RendezVousService` contient un CRUD clair :

- `create(RendezVous rv)` -> `INSERT`
- `getByPatient(int patientId)` -> `SELECT`
- `getById(int id)` -> `SELECT`
- `update(RendezVous rv)` -> `UPDATE`
- `delete(int id)` -> `DELETE`

Exemple create :

```java
String q = "INSERT INTO rendez_vous (medecin_id, patient_id, date, heure, statut) VALUES (?, ?, ?, ?, ?)";
```

Exemple update :

```java
String q = "UPDATE rendez_vous SET medecin_id=?, date=?, heure=?, statut=? WHERE id=?";
```

Exemple read avec jointure :

```java
String q = "SELECT rv.*, u.nom AS med_nom, u.prenom AS med_prenom, m.specialite " +
           "FROM rendez_vous rv " +
           "JOIN medecin m ON rv.medecin_id = m.id " +
           "JOIN user u ON m.user_id = u.id " +
           "WHERE rv.patient_id = ? AND rv.hidden_by_patient = 0 " +
           "ORDER BY rv.date DESC, rv.heure DESC";
```

Ce point montre que le projet ne fait pas seulement des requetes simples, mais aussi des **jointures SQL** entre plusieurs tables.

## 8. Comment j'ai concu le profil utilisateur

Le profil utilisateur n'est pas uniquement une page FXML statique.  
Il est construit de facon dynamique dans `UserSectionFactory.java`.

La methode principale est :

```java
public static Node createProfileSection(
    User currentUser,
    Window owner,
    Consumer<User> onUpdated,
    Runnable onDeleteAccount
)
```

### Le profil contient

- une photo de profil
- nom et prenom
- email
- numero
- adresse
- mot de passe modifiable
- regles de confidentialite
- bouton de sauvegarde
- bouton de suppression du compte

### Exemple de creation des champs

```java
TextField nomField = styledTextField(currentUser.getNom(), "Nom");
TextField prenomField = styledTextField(currentUser.getPrenom(), "Prenom");
TextField emailField = styledTextField(currentUser.getEmail(), "Email");
TextField numeroField = styledTextField(currentUser.getNumero(), "Numero de telephone");
TextField adresseField = styledTextField(currentUser.getAdresse(), "Adresse");
```

### Upload de la photo

La photo est choisie avec `FileChooser`, puis copiee dans `uploads/` :

```java
updatedUser.setPhoto(FileStorageUtil.copyToUploads(selectedPhotoPath[0], "profiles"));
```

La classe `FileStorageUtil` cree un nom unique puis copie le fichier :

```java
Path targetDir = BASE_UPLOAD_DIR.resolve(folderName);
Files.createDirectories(targetDir);
Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
```

### Sauvegarde du profil

Apres validation, les nouvelles donnees sont envoyees a :

```java
boolean updated = userService.updateProfile(updatedUser, passwordField.getText().trim());
```

Puis l'utilisateur courant est mis a jour en memoire :

```java
currentUser.setNom(updatedUser.getNom());
currentUser.setPrenom(updatedUser.getPrenom());
currentUser.setEmail(updatedUser.getEmail());
currentUser.setNumero(updatedUser.getNumero());
currentUser.setAdresse(updatedUser.getAdresse());
currentUser.setPhoto(updatedUser.getPhoto());
```

### Confidentialite

Le profil contient aussi des `ComboBox` pour definir la visibilite :

- email : public / private
- telephone : public / private
- adresse : public / private

Cela montre que le profil n'est pas seulement visuel, il inclut aussi une logique fonctionnelle complete.

## 9. Points techniques interessants a dire au jury

- Le mot de passe est **hashe avec BCrypt**, il n'est pas stocke en clair.
- La plupart des acces SQL utilisent **PreparedStatement**.
- L'architecture separe bien **vue / controle / acces donnees**.
- L'application gere plusieurs roles : **admin, patient, medecin**.
- Le profil utilisateur inclut **modification des donnees, photo, confidentialite et suppression du compte**.
- Les rendez-vous utilisent des **jointures SQL** entre `user`, `patient`, `medecin`, `specialite` et `rendez_vous`.
- Le projet utilise **JavaFX pour l'interface** et **Maven pour la gestion des dependances et de l'execution**.

## 10. Conclusion courte

Ce projet est une application JavaFX structuree de maniere claire :

- **les models** representent les donnees
- **les controllers** recuperent les saisies utilisateur
- **les services** executent les requetes SQL
- **MyConnection** relie l'application a MySQL
- **la session** est geree en memoire avec `currentUser`
- **Maven** simplifie la compilation et le lancement

Pour un passage devant le jury, les meilleurs exemples a montrer sont :

1. `LoginController` + `UserService.login`
2. `RegisterController.validateForm`
3. `MyConnection`
4. `UserService.add / updateProfile`
5. `RendezVousService.create / getByPatient / update / delete`
6. `UserSectionFactory.createProfileSection`
