import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by User_2 on 21.02.14.
 */
public class PeerForm {
    private JTabbedPane tabbedPane1;
    private JPanel mainPanel;
    private JList<Peer.OutConnection> outConnectionList;
    private JButton viewButton;
    private JList<Block> blockList;
    private JList<Peer> queuedList;
    private JButton viewButton1;
    private JButton addRandomBlockButton;
    private JButton viewButton2;
    private JList inConnectionList;

    private Peer peer;

    public PeerForm(final Peer peer) {
        this.peer = peer;
        peer.addForm(this);

        redrawBlocks();
        redrawOutConnections();
        redrawInConnections();
        redrawQueued();

        outConnectionList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                viewButton.setEnabled(true);
            }
        });
        inConnectionList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                viewButton2.setEnabled(true);
            }
        });
        queuedList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                viewButton1.setEnabled(true);
            }
        });
        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Peer.OutConnection selectedOutConnection = (Peer.OutConnection) outConnectionList.getSelectedValue();
                JFrame peerFrame = new JFrame(selectedOutConnection.getPeer().toString());
                peerFrame.setContentPane(new PeerForm(selectedOutConnection.getPeer()).getMainPanel());
                peerFrame.pack();
                peerFrame.setVisible(true);
            }
        });
        viewButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Peer.InConnection selectedInConnection = (Peer.InConnection) inConnectionList.getSelectedValue();
                JFrame peerFrame = new JFrame(selectedInConnection.getPeer().toString());
                peerFrame.setContentPane(new PeerForm(selectedInConnection.getPeer()).getMainPanel());
                peerFrame.pack();
                peerFrame.setVisible(true);
            }
        });
        viewButton1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Peer selectedPeer = (Peer) queuedList.getSelectedValue();
                JFrame peerFrame = new JFrame(selectedPeer.toString());
                peerFrame.setContentPane(new PeerForm(selectedPeer).getMainPanel());
                peerFrame.pack();
                peerFrame.setVisible(true);
            }
        });
        addRandomBlockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // first, create random code
                int codeLength = 8 + (int) (Math.random() * 25);
                Code code = new Code();
                for (int i = 0; i < codeLength; i++) {
                    code.setLayerCode(i, Math.random() < 0.5 ? 0 : 1);
                }

                // now, hand block to selected peer
                peer.store(new Block(code));

                redrawBlocks();
            }
        });
    }

    public void redrawBlocks() {
        tabbedPane1.setTitleAt(1, "Blocks (" + peer.getBlocks().size() + ")");

        RefreshableListModel<Block> blockListModel = new RefreshableListModel<Block>();
        for (Block block : peer.getBlocks()) {
            blockListModel.addElement(block);
        }
        blockList.setModel(blockListModel);
    }

    public void redrawOutConnections() {
        tabbedPane1.setTitleAt(2, "Out-Connections (" + peer.getOutConnections().size() + ")");

        RefreshableListModel<Peer.OutConnection> outConnectionListModel = new RefreshableListModel<Peer.OutConnection>();
        for (Peer.OutConnection outConnection : peer.getOutConnections()) {
            outConnectionListModel.addElement(outConnection);
        }
        outConnectionList.setModel(outConnectionListModel);
    }

    public void redrawInConnections() {
        tabbedPane1.setTitleAt(3, "In-Connections (" + peer.getInConnections().size() + ")");

        RefreshableListModel<Peer.InConnection> inConnectionListModel = new RefreshableListModel<Peer.InConnection>();
        for (Peer.InConnection inConnection : peer.getInConnections()) {
            inConnectionListModel.addElement(inConnection);
        }
        inConnectionList.setModel(inConnectionListModel);
    }

    public void redrawQueued() {
        tabbedPane1.setTitleAt(4, "Queued (" + peer.getQueued().size() + ")");

        RefreshableListModel<Peer> queuedListModel = new RefreshableListModel<Peer>();
        for (Peer peer1 : peer.getQueued()) {
            queuedListModel.addElement(peer1);
        }
        queuedList.setModel(queuedListModel);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
