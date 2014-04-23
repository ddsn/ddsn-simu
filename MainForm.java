import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Julian M. <julian@ddsn.org> on 21.02.14.
 */
public class MainForm {
    private JTabbedPane tabbedPane1;
    private JPanel mainPanel;
    private JList peerList;
    private JButton viewButton;
    private JButton destroyButton;
    private JButton connectNewButton;
    private JTextPane textPane1;
    private JButton consistencyCheckButton;

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
            StringBuffer sb = new StringBuffer();
            sb.append("Peer #" + peer.getId());
            sb.append(" (" + peer.getCode() + ")");
            sb.append(" | blocks: " + peer.getBlocks().size());
            sb.append(" | out: " + peer.getOutConnections().size());
            sb.append(" | in: " + peer.getInConnections().size());
            sb.append(" | queued: " + peer.getQueued().size());
            if (peer.overloaded() > 0) {
                sb.append(" | overloaded by " + peer.overloaded());
            }
            return sb.toString();
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
                Ddsn.messages.addLast(new Message.ConnectPeerMessage(selectedPeer, null, newPeer));
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
        consistencyCheckButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.err.println("Start consistency check...");
                for (Peer peer : Ddsn.peers) {
                    for (Peer.OutConnection outConnection : peer.getOutConnections()) {
                        if (outConnection.getPeer() == null) {
                            System.err.println(peer + " has no peer for layer " + outConnection.getLayer());
                            continue;
                        }

                        boolean found = false;
                        for (Peer.InConnection inConnection : outConnection.getPeer().getInConnections()) {
                            if (inConnection.getPeer() == peer && inConnection.getLayer() == outConnection.getLayer()) {
                                found = true;
                            }
                        }
                        if (!found) {
                            System.err.println("Detected inconsistency: no inConnection at " + outConnection.getPeer() + " for outConnection of " + peer + " (layer " + outConnection.getLayer() + ")");
                        }
                    }

                    for (Peer.InConnection inConnection : peer.getInConnections()) {
                        boolean found = false;
                        for (Peer.OutConnection outConnection : inConnection.getPeer().getOutConnections()) {
                            if (outConnection.getPeer() == peer && inConnection.getLayer() == outConnection.getLayer()) {
                                found = true;
                            }
                        }
                        if (!found) {
                            System.err.println("Detected inconsistency: no outConnection at " + inConnection.getPeer() + " for inConnection of " + peer + " (layer " + inConnection.getLayer() + ")");
                        }
                    }
                }
                System.err.println("...end consistency check");
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JTextPane getMainTextPane() {
        return textPane1;
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
