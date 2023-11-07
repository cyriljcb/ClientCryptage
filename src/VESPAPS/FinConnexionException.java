package VESPAPS;

public class FinConnexionException extends Exception {
    private ReponseSecuriseLogin reponse;

    public FinConnexionException(ReponseSecuriseLogin reponse)
    {
        super("Fin de Connexion décidée par protocole");
        this.reponse = reponse;
    }

    public ReponseSecuriseLogin getReponse()
    {
        return reponse;
    }
}
