import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Julian M. <julian@ddspn.org> on 21.02.14.
 */
public class MainForm {
    private JTabbedPane tabbedPane1;
    private JPanel mainPanel;
    private JList peerList;
    private JButton viewButton;
    private JButton destroyButton;
    private JButton connectNewButton;

    private RefreshableListModel<PeerListElement> peerListModel;

    private class PeerListElement {

        private Peer peer;

        public PeerListElement(Peer peer) {
            this.peer = peer;
        }

        public Peer getPeer() {
            return peer;
        }

        @Override
        public String toString() {
            return "peer #" + peer.getId() +
                    " (" + peer.getCode() + ") " +
                    " | blocks: " + peer.getBlocks().size() +
                    " | out: " + peer.getOutConnections().size() +
                    " | in: " + peer.getInConnections().size() +
                    " | queued: " + peer.getQueued().size();
        }

    }

    public MainForm() {
        peerListModel = new RefreshableListModel<PeerListElement>();
        peerList.setModel(peerListModel);
        peerList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                viewButton.setEnabled(true);
                destroyButton.setEnabled(true);
                connectNewButton.setEnabled(true);
            }
        });
        connectNewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Peer selectedPeer = ((PeerListElement) peerList.getSelectedValue()).getPeer();
                Peer newPeer = new Peer();
                Ddsn.addPeer(newPeer);
                selectedPeer.connect(newPeer);
            }
        });
        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Peer selectedPeer = ((PeerListElement) peerList.getSelectedValue()).getPeer();
                JFrame peerFrame = new JFrame(selectedPeer.toString());
                peerFrame.setContentPane(new PeerForm(selectedPeer).getMainPanel());
                peerFrame.pack();
                peerFrame.setVisible(true);
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void addPeer(Peer peer) {
        RefreshableListModel<PeerListElement> model = (RefreshableListModel<PeerListElement>) peerList.getModel();
        model.addElement(new PeerListElement(peer));
    }

    public void peerChanged(Peer peer) {
        for (int i = 0; i < peerListModel.getSize(); i++) {
            if (peerListModel.getElementAt(i).getPeer() == peer) {
                peerListModel.refreshElement(i);
            }
        }
    }
}
