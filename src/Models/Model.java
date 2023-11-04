package Models;

import Classe.Caddie;
import Classe.Employe;
import Classe.Facture;
import Cryptage.MyCrypto;
import OVESP.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.*;
import java.net.Socket;
import java.security.*;
import javax.crypto.*;


public class Model {
    private List<Facture> factures = new ArrayList<>();
    private boolean isInitialized = false;
    private String message;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private boolean islogged = false;
    private Employe employe;
    private int numClient;
    private SecretKey cleSession;
    private  List<Caddie> caddie1 = new ArrayList<>();

    private void initializeObjectStreams() {
        if (!isInitialized) {
            Properties properties = new Properties();
            FileInputStream input = null;
            int port;
            String ipServeur;
            try {
                input = new FileInputStream("src\\config.properties");
                properties.load(input);
                port = Integer.parseInt(properties.getProperty("PORT_PAIEMENT_SECURE"));
                ipServeur = properties.getProperty(("IP_SERVEUR"));
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ex) {
                        message = ("erreur de lecture dans le fichier de conf");
                    }
                }
            }
            try {
                socket = new Socket(ipServeur, port);
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());
                isInitialized = true; // Marquer comme initialisé
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public String getMessage() {
        return message;
    }

    public List<Facture> getFactures() {
        return factures;
    }
    public List<Caddie> getCaddie1() {
        return caddie1;
    }

    public boolean login(String nom, String mdp, boolean creation) {
        boolean v = false;
        initializeObjectStreams();
        if (nom.isEmpty() || mdp.isEmpty()) {
            message = ("veuillez compléter correctement les champs");
        } else if (!nom.matches("^[a-zA-Z0-9]*$") || !mdp.matches("^[a-zA-Z0-9]*$")) {
            message = ("rah ouais, t'essaies quoi la?");
        } else {
            try {
                //pour la clé de session
                ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                DataOutputStream dos1 = new DataOutputStream(baos1);
                dos1.writeUTF(nom);
                dos1.writeUTF(mdp);
                //TODO creation de login
                byte[] messageClair = baos1.toByteArray();
                // Génération d'une clé de session
                KeyGenerator cleGen = KeyGenerator.getInstance("DES","BC");
                cleGen.init(new SecureRandom());
                cleSession = cleGen.generateKey();
                // Recuperation de la clé publique du serveur
                PublicKey clePubliqueServeur = RecupereClePubliqueServeur();
                // Cryptage asymétrique de la clé de session
                byte[] cleSessionCrypte;
                cleSessionCrypte = MyCrypto.CryptAsymRSA(clePubliqueServeur,cleSession.getEncoded());
                // Cryptage symétrique du message
                byte[] messageCrypte;
                messageCrypte = MyCrypto.CryptSymDES(cleSession,messageClair);

                RequeteLogin requete = new RequeteLogin(nom, mdp, creation);
                requete.setData1(cleSessionCrypte);
                requete.setData2(messageCrypte);
                oos.writeObject(requete);
                System.out.println("est passé dans le login requete" + requete);
                ReponseLogin reponse = (ReponseLogin) ois.readObject();
                if (reponse.isValide()) {
                    employe = new Employe(nom, mdp);
                    message = ("connecté");
                    v = true;
                    islogged = true;
                } else {
                    System.out.println("le messsage = "+reponse.getMessage());
                    message = reponse.getMessage();
                }
            } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | NoSuchProviderException ex) {
                ex.printStackTrace();
                message = "problème lors du login : " + ex.getMessage();
            } catch (UnrecoverableKeyException | CertificateException | SignatureException | KeyStoreException |
                     InvalidKeyException | IllegalBlockSizeException | NoSuchPaddingException | BadPaddingException e) {
                e.printStackTrace();
                System.out.println("attention Erreur : " + e.getMessage());
            }

        }
        return v;
    }

//    public boolean Rechercher(String idCli) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException {
//        boolean v = false;
//        numClient = Integer.parseInt(idCli);
//        if (idCli.matches("\\d+"))    //pour les chiffres positifs
//        {
//            RequeteFacture requete = new RequeteFacture(idCli);
//
//            try {
//                oos.writeObject(requete);
//                ReponseFacture reponse = (ReponseFacture) ois.readObject();
//                factures = reponse.getFacture();
//                v = true;
//            } catch (IOException | ClassNotFoundException ex) {
//                throw new RuntimeException(ex);
//            }
//        } else
//            message = ("veuillez entrer un id de facture valide");
//        return v;
//    }
    public boolean Rechercher(String idCli) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException {
        boolean v = false;
        numClient = Integer.parseInt(idCli);

        if (idCli.matches("\\d+")) {
            RequeteFacture requete = new RequeteFacture(idCli);

            try {
                oos.writeObject(requete);
                ReponseFacture reponse = (ReponseFacture) ois.readObject();

                if (reponse.getFactureCrypte() != null) {
                    byte[] facturesCryptees = reponse.getFactureCrypte();
                    byte[] facturesDecryptees = decryptFacture(facturesCryptees);
                    System.out.println("facture : "+facturesDecryptees);
                    // Maintenant, vous avez les factures désérialisées
                   factures = deserializeFactures(facturesDecryptees);

                    v = true;
                } else {
                    message = reponse.getMessage();
                }
            } catch (IOException | ClassNotFoundException | IllegalBlockSizeException | NoSuchPaddingException |
                     BadPaddingException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            message = "Veuillez entrer un ID de facture valide.";
        }

        return v;
    }

    private byte[] decryptFacture(byte[] facture) throws IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        byte[] messageDecrypte;
        messageDecrypte = MyCrypto.DecryptSymDES(cleSession, facture);
        return messageDecrypte;
    }


//    public boolean payer(String nom, String numVisa, int Row, String info) {
//        boolean v = false;
//        if (Row != -1) {
//
//            RequetePayeFacture requete = new RequetePayeFacture(info, String.valueOf(numClient), nom, numVisa);
//            try {
//                oos.writeObject(requete);
//                ReponsePayeFacture reponse1 = (ReponsePayeFacture) ois.readObject();
//                if (reponse1.getPaye()) {
//                    RequeteFacture requete1 = new RequeteFacture(String.valueOf(numClient));
//                    oos.writeObject(requete1);
//                    ReponseFacture reponse = (ReponseFacture) ois.readObject();
//                    factures = reponse.getFacture();
//                    message = ("Facture payée");
//                    v = true;
//                } else message = ("Probleme lors du paiement de la facture");
//            } catch (IOException | ClassNotFoundException | UnrecoverableKeyException | CertificateException |
//                     NoSuchAlgorithmException | KeyStoreException | SignatureException | NoSuchProviderException |
//                     InvalidKeyException ex) {
//                throw new RuntimeException(ex);
//            }
//        } else
//            message = "aucune facture selectionnée";
//        return v;
//    }
public boolean payer(String nom, String numVisa, int Row, String info) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, InvalidKeyException, NoSuchProviderException {
    boolean v = false;
    if (Row != -1) {
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        DataOutputStream dos1 = new DataOutputStream(baos1);
        dos1.writeUTF(info);
        //dos1.writeInt(numClient); inutile
        dos1.writeUTF(nom);
        dos1.writeUTF(numVisa);
        byte[] messageClair = baos1.toByteArray();
        // Cryptage symétrique du message
        byte[] messageCrypte;
        messageCrypte = MyCrypto.CryptSymDES(cleSession,messageClair);
        RequetePayeFacture requete = new RequetePayeFacture(messageCrypte);
        //RequetePayeFacture requete = new RequetePayeFacture(info, String.valueOf(numClient), nom, numVisa);
        try {
            oos.writeObject(requete);
            ReponsePayeFacture reponse1 = (ReponsePayeFacture) ois.readObject();
            // Validation du HMAC
            Mac hm = Mac.getInstance("HMAC-MD5", "BC");
            hm.init(cleSession);
            byte[] reponseHMAC = reponse1.getHMAC();
            byte[] reponseSansHMAC = reponse1.getMessage();

            hm.update(reponseSansHMAC);
            byte[] hmacCalcul = hm.doFinal();
            boolean payer = (reponseSansHMAC[0] == 1);   //retourne true ou false

            if (MessageDigest.isEqual(reponseHMAC, hmacCalcul)) {
                // Le HMAC est valide
                System.out.println("HMAC validé");
                if (payer) {
                    RequeteFacture requete1 = new RequeteFacture(String.valueOf(numClient));
                    oos.writeObject(requete1);
                    ReponseFacture reponse = (ReponseFacture) ois.readObject();
                    factures = reponse.getFacture();
                    message = ("Facture payée");
                    v = true;
                } else message = ("Probleme lors du paiement de la facture");
            }
        } catch (IOException | ClassNotFoundException | UnrecoverableKeyException | CertificateException |
                 NoSuchAlgorithmException | KeyStoreException | SignatureException | NoSuchProviderException |
                 InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }
    } else
        message = "aucune facture selectionnée";
    return v;
}
    public void caddie(String caddie)
    {
        try{

            RequeteCaddie requete = new RequeteCaddie(caddie);
            oos.writeObject(requete);
            ReponseCaddie reponse1 = (ReponseCaddie) ois.readObject();
            caddie1 = reponse1.getCaddieList();
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }

    }

    public void fermerApp() {
        if (socket != null) {
            if (islogged) {

                RequeteLOGOUT requete = new RequeteLOGOUT(employe.getLogin());
                try {
                    oos.writeObject(requete);
                    ReponseLogout reponse = (ReponseLogout) ois.readObject();
                    socket.close();
                    oos.close();
                    ois.close();
                    islogged = false;
                    isInitialized = false;

                } catch (IOException | ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }


        }

    }
    public List<Facture> deserializeFactures(byte[] facturesBytes) {
        List<Facture> factures = new ArrayList<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(facturesBytes);
        DataInputStream dis = new DataInputStream(bais);

        try {
            int numberOfFactures = dis.readInt();

            for (int i = 0; i < numberOfFactures; i++) {
                Facture facture = Facture.fromDataInputStream(dis);
                if (facture != null) {
                    factures.add(facture);
                }
            }
        } catch (IOException e) {
            // Gérez l'exception comme requis (peut-être la journalisation ou le renvoi d'une liste vide)
            e.printStackTrace();
        }

        return factures;
    }
    public static PublicKey RecupereClePubliqueServeur() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        // Récupération de la clé publique de Jean-Marc dans le keystore deChristophe
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("KeystoreClientCryptage.jks"),"ClientCryptage".toCharArray());
        X509Certificate certif = (X509Certificate)ks.getCertificate("ServeurCryptage");
        PublicKey cle = certif.getPublicKey();
        return cle;
    }


}