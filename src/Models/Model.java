package Models;

import Classe.Caddie;
import Classe.Employe;
import Classe.Facture;
import Cryptage.MyCrypto;
import VESPAPS.*;

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
import java.util.List;
import java.util.Properties;
import java.io.*;
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
                dos1.writeBoolean(creation);

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

                RequeteSecuriseLogin requete = new RequeteSecuriseLogin(nom, mdp, creation);
                requete.setData1(cleSessionCrypte);
                requete.setData2(messageCrypte);
                oos.writeObject(requete);
                System.out.println("est passé dans le login requete" + requete);
                ReponseSecuriseLogin reponse = (ReponseSecuriseLogin) ois.readObject();
                if (reponse.isValide()) {
                    employe = new Employe(nom, mdp);
                    message = reponse.getMessage();
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
    public boolean Rechercher(String idCli) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException {
        boolean v = false;
        numClient = Integer.parseInt(idCli);

        if (idCli.matches("\\d+")) {
            RequeteSecuriseFacture requete = new RequeteSecuriseFacture(idCli);

            try {
                oos.writeObject(requete);
                ReponseSecuriseFacture reponse = (ReponseSecuriseFacture) ois.readObject();

                if (
                        reponse.getFactureCrypte() != null) {
                    byte[] facturesCryptees = reponse.getFactureCrypte();

                    System.out.println("les factures cryptées : "+facturesCryptees);
                    byte[] facturesDecryptees = decryptMessage(facturesCryptees);
                    System.out.println("facture : "+facturesDecryptees);
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
        RequeteSecurisePayeFacture requete = new RequeteSecurisePayeFacture(messageCrypte);
        //RequeteSecurisePayeFacture requete = new RequeteSecurisePayeFacture(info, String.valueOf(numClient), nom, numVisa);
        try {
            oos.writeObject(requete);
            ReponseSecurisePayeFacture reponse1 = (ReponseSecurisePayeFacture) ois.readObject();
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
                    RequeteSecuriseFacture requete1 = new RequeteSecuriseFacture(String.valueOf(numClient));
                    oos.writeObject(requete1);
                    ReponseSecuriseFacture reponse = (ReponseSecuriseFacture) ois.readObject();
                    if (reponse.getFactureCrypte() != null) {
                        byte[] facturesCryptees = reponse.getFactureCrypte();
                        System.out.println("les factures cryptées : "+facturesCryptees);
                        byte[] facturesDecryptees = decryptMessage(facturesCryptees);
                        System.out.println("facture : "+facturesDecryptees);
                        factures = deserializeFactures(facturesDecryptees);
                        message = "facture payée avec succès";
                        v = true;
                    } else {
                        message = reponse.getMessage();
                    }
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
    public boolean caddie(String caddie)
    {
        boolean v = false;
        try{

            RequeteSecuriseCaddie requete = new RequeteSecuriseCaddie(caddie);
            oos.writeObject(requete);
            ReponseSecuriseCaddie reponse1 = (ReponseSecuriseCaddie) ois.readObject();
            if (reponse1.getCaddieCrypte() != null) {
                byte[] caddieCrypte = reponse1.getCaddieCrypte();

                System.out.println("le caddie crypté : "+caddieCrypte);
                byte[] caddieDecrypte = decryptMessage(caddieCrypte);
                System.out.println("caddie : "+caddieDecrypte);
                caddie1 = deserializeCaddie(caddieDecrypte);

                v = true;
            } else {
                message = reponse1.getMessage();
            }
        } catch (IOException | ClassNotFoundException | UnrecoverableKeyException | CertificateException |
                 NoSuchAlgorithmException | KeyStoreException | SignatureException | NoSuchProviderException |
                 InvalidKeyException | IllegalBlockSizeException | NoSuchPaddingException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
        return v;
    }

    public void fermerApp() {
        if (socket != null) {
            if (islogged) {

                RequeteSecuriseLOGOUT requete = new RequeteSecuriseLOGOUT(employe.getLogin());
                try {
                    oos.writeObject(requete);
                    ReponseSecuriseLogout reponse = (ReponseSecuriseLogout) ois.readObject();
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
            e.printStackTrace();
        }

        return factures;
    }
    public List<Caddie> deserializeCaddie(byte[] caddieBytes) {
        List<Caddie> caddies = new ArrayList<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(caddieBytes);
        DataInputStream dis = new DataInputStream(bais);

        try {
            int numberOfArticles = dis.readInt();

            for (int i = 0; i < numberOfArticles; i++) {
                Caddie caddie = Caddie.fromDataInputStream(dis);
                if (caddie != null) {
                    caddies.add(caddie);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return caddies;
    }
    private byte[] decryptMessage(byte[] msg) throws IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        byte[] messageDecrypte;
        messageDecrypte = MyCrypto.DecryptSymDES(cleSession, msg);
        return messageDecrypte;
    }
    public static PublicKey RecupereClePubliqueServeur() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("KeystoreClientCryptage.jks"),"ClientCryptage".toCharArray());
        X509Certificate certif = (X509Certificate)ks.getCertificate("ServeurCryptage");
        PublicKey cle = certif.getPublicKey();
        return cle;
    }
}