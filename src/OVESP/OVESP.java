package OVESP;

import java.net.Socket;
import java.util.HashMap;

public class OVESP //implements Protocole
{
    private HashMap<String, String> passwords;
    private HashMap<String, Socket> clientsConnectes;
    private HashMap<Integer, String> idClientConnectes;

    private Logger logger;

    public OVESP(Logger log) {

    }
}