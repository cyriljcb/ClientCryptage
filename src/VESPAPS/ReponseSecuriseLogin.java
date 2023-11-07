package VESPAPS;

public class ReponseSecuriseLogin implements ReponseSecurise {
    private boolean valide;
    private String message;

    ReponseSecuriseLogin(boolean v, String message) {
        valide = v;
        if(v)
            this.message = message;
    }
    public boolean isValide() {
        return valide;
    }

    public String getMessage() {
        return message;
    }

}
