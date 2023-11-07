package VESPAPS;
import java.net.Socket;

public interface Protocole {
    String getNom();
    ReponseSecuriseLogin TraiteRequete(RequeteSecurise requeteSecurise, Socket socket) throws FinConnexionException;

}
