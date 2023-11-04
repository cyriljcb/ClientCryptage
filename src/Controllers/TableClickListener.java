package Controllers;

import Models.Model;
import View.ClientPaiement;

import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class TableClickListener implements MouseListener {
    private Model model;
    private ClientPaiement view;

    public TableClickListener(Model model, ClientPaiement view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (view.isTestTable()) {
            int row = view.getFactureTable().getSelectedRow();
            if (row != -1) { // Vérifiez d'abord si une ligne est sélectionnée
                DefaultTableModel model1 = (DefaultTableModel) view.getFactureTable().getModel();
                String info = model1.getValueAt(row, 0).toString();
                boolean v = model.caddie(info);
                if(v)
                    view.updateViewTable(model);
                else
                    view.updateViewMessage(model);
            }
        }
    }


    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    // Autres méthodes de l'interface MouseListener à implémenter
}

